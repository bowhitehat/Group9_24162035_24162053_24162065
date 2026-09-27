package com.group9.topicmanagement.service;

import com.group9.topicmanagement.domain.User;
import com.group9.topicmanagement.domain.council.CouncilAssignment;
import com.group9.topicmanagement.domain.council.CouncilMember;
import com.group9.topicmanagement.domain.enums.CouncilStatus;
import com.group9.topicmanagement.domain.enums.EvaluationStatus;
import com.group9.topicmanagement.domain.enums.EvaluationType;
import com.group9.topicmanagement.domain.evaluation.Evaluation;
import com.group9.topicmanagement.domain.evaluation.EvaluationCriterion;
import com.group9.topicmanagement.domain.evaluation.EvaluationScore;
import com.group9.topicmanagement.domain.evaluation.ReviewerAssignment;
import com.group9.topicmanagement.domain.enums.ReviewerAssignmentStatus;
import com.group9.topicmanagement.domain.topic.Topic;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.exception.NotFoundException;
import com.group9.topicmanagement.repository.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class EvaluationService {
    private final EvaluationRepository evaluationRepository;
    private final EvaluationScoreRepository scoreRepository;
    private final EvaluationCriterionRepository criterionRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final ReviewerAssignmentService reviewerAssignmentService;
    private final CouncilAssignmentRepository councilAssignmentRepository;
    private final CouncilMemberRepository councilMemberRepository;

    public EvaluationService(EvaluationRepository evaluationRepository,
                             EvaluationScoreRepository scoreRepository,
                             EvaluationCriterionRepository criterionRepository,
                             TopicRepository topicRepository,
                             UserRepository userRepository,
                             ReviewerAssignmentService reviewerAssignmentService,
                             CouncilAssignmentRepository councilAssignmentRepository,
                             CouncilMemberRepository councilMemberRepository) {
        this.evaluationRepository = evaluationRepository;
        this.scoreRepository = scoreRepository;
        this.criterionRepository = criterionRepository;
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
        this.reviewerAssignmentService = reviewerAssignmentService;
        this.councilAssignmentRepository = councilAssignmentRepository;
        this.councilMemberRepository = councilMemberRepository;
    }

    @PreAuthorize("hasAnyRole('LECTURER', 'FACULTY_MANAGER')")
    public Evaluation saveDraft(Long topicId, String evaluatorUsername, EvaluationType type,
                                List<EvaluationScore> scores, String comments) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đề tài"));
        User evaluator = userRepository.findByUsernameIgnoreCase(evaluatorUsername)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người đánh giá"));

        if (topic.getAdvisors().stream().anyMatch(a -> a.getId().equals(evaluator.getId()))) {
            throw new BusinessRuleException("Không được chấm đề tài mình đang hướng dẫn");
        }

        EvaluationContext context = requireEditableContext(topicId, evaluator, type);
        ReviewerAssignment reviewerAssignment = context.reviewerAssignment();
        CouncilMember councilMember = context.councilMember();

        Evaluation evaluation = evaluationRepository
                .findByTopicIdAndEvaluatorIdAndEvaluationType(topicId, evaluator.getId(), type)
                .orElseGet(() -> {
                    Evaluation e = new Evaluation();
                    e.setTopic(topic);
                    e.setEvaluator(evaluator);
                    e.setEvaluationType(type);
                    e.setStatus(EvaluationStatus.DRAFT);
                    return e;
                });

        if (evaluation.getStatus() == EvaluationStatus.LOCKED) {
            throw new BusinessRuleException("Đánh giá đã bị khóa, không thể sửa");
        }

        if (evaluation.getStatus() == EvaluationStatus.SUBMITTED) {
            evaluation.setStatus(EvaluationStatus.DRAFT);
            evaluation.setSubmissionTime(null);
            if (type == EvaluationType.REVIEWER) {
                reviewerAssignmentService.markInProgressForEdit(topicId, evaluator.getId());
            }
        } else if (type == EvaluationType.REVIEWER) {
            reviewerAssignmentService.markInProgress(topicId, evaluator.getId());
        }

        evaluation.setReviewerAssignment(reviewerAssignment);
        evaluation.setCouncilMember(councilMember);
        evaluation.setComments(comments);
        evaluation = evaluationRepository.save(evaluation);

        Set<Long> submittedCriterionIds = new HashSet<>();
        for (EvaluationScore scoreDto : scores) {
            if (scoreDto.getScore() == null
                    || scoreDto.getScore().compareTo(BigDecimal.ZERO) < 0
                    || scoreDto.getScore().compareTo(BigDecimal.TEN) > 0) {
                throw new BusinessRuleException("Điểm phải từ 0 đến 10");
            }
            EvaluationCriterion criterion = criterionRepository.findById(scoreDto.getCriterion().getId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy tiêu chí"));

            if (!criterion.getRegistrationPeriod().getId().equals(topic.getRegistrationPeriod().getId())) {
                throw new BusinessRuleException("Tiêu chí không thuộc đợt đăng ký của đề tài");
            }
            if (!Boolean.TRUE.equals(criterion.getIsActive())) {
                throw new BusinessRuleException("Tiêu chí không còn hoạt động");
            }
            if (!submittedCriterionIds.add(criterion.getId())) {
                throw new BusinessRuleException("Mỗi tiêu chí chỉ được nhập một lần");
            }

            Optional<EvaluationScore> existingScore = scoreRepository.findByEvaluationId(evaluation.getId()).stream()
                    .filter(s -> s.getCriterion().getId().equals(criterion.getId())).findFirst();

            EvaluationScore score = existingScore.orElse(new EvaluationScore());
            score.setEvaluation(evaluation);
            score.setCriterion(criterion);
            score.setScore(scoreDto.getScore());
            score.setNote(scoreDto.getNote());
            scoreRepository.save(score);
        }

        List<EvaluationScore> removedScores = scoreRepository.findByEvaluationId(evaluation.getId()).stream()
                .filter(existing -> !submittedCriterionIds.contains(existing.getCriterion().getId()))
                .toList();
        if (!removedScores.isEmpty()) {
            scoreRepository.deleteAll(removedScores);
            scoreRepository.flush();
        }

        return evaluation;
    }

    @PreAuthorize("hasRole('LECTURER')")
    public void submitEvaluation(Long evaluationId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiếu chấm"));

        String currentUsername = currentUsername();
        if (!evaluation.getEvaluator().getUsername().equalsIgnoreCase(currentUsername)) {
            throw new AccessDeniedException("Bạn chỉ được nộp phiếu chấm của mình");
        }

        requireEditableContext(evaluation.getTopic().getId(), evaluation.getEvaluator(),
                evaluation.getEvaluationType());

        if (evaluation.getStatus() != EvaluationStatus.DRAFT && evaluation.getStatus() != EvaluationStatus.SUBMITTED) {
            throw new BusinessRuleException("Chỉ phiếu DRAFT hoặc SUBMITTED mới có thể nộp");
        }

        List<EvaluationCriterion> criteria = criterionRepository
                .findByRegistrationPeriodIdAndIsActiveTrueOrderByDisplayOrderAsc(
                        evaluation.getTopic().getRegistrationPeriod().getId());
        List<EvaluationScore> scores = scoreRepository.findByEvaluationId(evaluationId);

        for (EvaluationCriterion criterion : criteria) {
            if (Boolean.TRUE.equals(criterion.getIsMandatory())) {
                boolean hasScore = scores.stream().anyMatch(s -> s.getCriterion().getId().equals(criterion.getId()));
                if (!hasScore) {
                    throw new BusinessRuleException("Chưa chấm đủ các tiêu chí bắt buộc");
                }
            }
        }

        evaluation.setStatus(EvaluationStatus.SUBMITTED);
        evaluation.setSubmissionTime(LocalDateTime.now());
        evaluationRepository.save(evaluation);

        if (evaluation.getEvaluationType() == EvaluationType.REVIEWER) {
            reviewerAssignmentService.markSubmitted(evaluation.getTopic().getId(), evaluation.getEvaluator().getId());
        }
    }

    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public Evaluation lockEvaluation(Long evaluationId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiếu chấm"));
        if (evaluation.getStatus() != EvaluationStatus.SUBMITTED) {
            throw new BusinessRuleException("Chỉ khóa phiếu đã nộp");
        }
        evaluation.setStatus(EvaluationStatus.LOCKED);
        evaluation.setLockedTime(LocalDateTime.now());
        return evaluationRepository.save(evaluation);
    }

    @Transactional(readOnly = true)
    public Evaluation getOrEmpty(Long topicId, Long evaluatorId, EvaluationType type) {
        return evaluationRepository.findByTopicIdAndEvaluatorIdAndEvaluationType(topicId, evaluatorId, type)
                .orElseGet(Evaluation::new);
    }

    public Evaluation getForForm(Long topicId, String evaluatorUsername, EvaluationType type) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đề tài"));
        User evaluator = userRepository.findByUsernameIgnoreCase(evaluatorUsername)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người đánh giá"));
        if (topic.getAdvisors().stream().anyMatch(a -> a.getId().equals(evaluator.getId()))) {
            throw new AccessDeniedException("Không được xem phiếu chấm của đề tài mình đang hướng dẫn");
        }
        EvaluationContext context;
        try {
            context = requireAssignedContext(topicId, evaluator, type);
        } catch (BusinessRuleException ex) {
            throw new AccessDeniedException(ex.getMessage());
        }
        if (type == EvaluationType.COUNCIL_MEMBER
                && context.councilMember().getCouncil().getStatus() != CouncilStatus.ACTIVE) {
            throw new BusinessRuleException("Chỉ cho chấm điểm khi hội đồng đang hoạt động");
        }
        Evaluation evaluation = evaluationRepository
                .findByTopicIdAndEvaluatorIdAndEvaluationType(topicId, evaluator.getId(), type)
                .orElseGet(Evaluation::new);
        if (type == EvaluationType.REVIEWER && canEdit(topicId, evaluatorUsername, type, evaluation)) {
            reviewerAssignmentService.markInProgress(topicId, evaluator.getId());
        }
        return evaluation;
    }

    @Transactional(readOnly = true)
    public boolean canEdit(Long topicId, String evaluatorUsername, EvaluationType type, Evaluation evaluation) {
        if (evaluation.getStatus() == EvaluationStatus.LOCKED) {
            return false;
        }
        try {
            User evaluator = userRepository.findByUsernameIgnoreCase(evaluatorUsername)
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy người đánh giá"));
            requireEditableContext(topicId, evaluator, type);
            return true;
        } catch (BusinessRuleException | AccessDeniedException ex) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public List<Evaluation> listByTopic(Long topicId) {
        List<Evaluation> list = evaluationRepository.findByTopicIdWithEvaluator(topicId);
        for (Evaluation evaluation : list) {
            List<EvaluationScore> scores = scoreRepository.findByEvaluationId(evaluation.getId());
            scores.forEach(score -> score.getCriterion().getName());
            evaluation.setScores(scores);
        }
        return list;
    }

    @Transactional(readOnly = true)
    public List<EvaluationScore> getScores(Long evaluationId) {
        return scoreRepository.findByEvaluationId(evaluationId);
    }

    @Transactional(readOnly = true)
    public List<EvaluationCriterion> getActiveCriteria(Long periodId) {
        return criterionRepository.findByRegistrationPeriodIdAndIsActiveTrueOrderByDisplayOrderAsc(periodId);
    }

    private CouncilMember requireCouncilMember(Long topicId, Long userId) {
        List<CouncilAssignment> assignments = councilAssignmentRepository
                .findByTopicIdAndCouncilStatusIn(topicId, List.of(CouncilStatus.ACTIVE));
        for (CouncilAssignment assignment : assignments) {
            Optional<CouncilMember> member = councilMemberRepository
                    .findByCouncilIdAndMemberId(assignment.getCouncil().getId(), userId);
            if (member.isPresent()) {
                return member.get();
            }
        }
        throw new BusinessRuleException("Không được chấm đề tài chưa được phân công");
    }

    private EvaluationContext requireEditableContext(Long topicId, User evaluator, EvaluationType type) {
        EvaluationContext context = requireAssignedContext(topicId, evaluator, type);
        if (type == EvaluationType.REVIEWER) {
            ReviewerAssignment assignment = context.reviewerAssignment();
            if (assignment.getStatus() == ReviewerAssignmentStatus.OVERDUE
                    || (assignment.getDeadline() != null && LocalDateTime.now().isAfter(assignment.getDeadline()))) {
                throw new BusinessRuleException("Đã quá hạn nộp điểm phản biện");
            }
        } else {
            CouncilMember member = context.councilMember();
            if (member.getCouncil().getStatus() != CouncilStatus.ACTIVE) {
                throw new BusinessRuleException("Chỉ cho chấm điểm khi hội đồng đang hoạt động");
            }
            if (member.getCouncil().getReportDate() != null
                    && LocalDateTime.now().isAfter(member.getCouncil().getReportDate().toLocalDate().atTime(23, 59, 59))) {
                throw new BusinessRuleException("Đã quá hạn nộp điểm hội đồng");
            }
        }
        return context;
    }

    private EvaluationContext requireAssignedContext(Long topicId, User evaluator, EvaluationType type) {
        if (type == EvaluationType.REVIEWER) {
            ReviewerAssignment assignment = reviewerAssignmentService.requireActiveAssignment(topicId, evaluator.getId());
            return new EvaluationContext(assignment, null);
        }
        if (type == EvaluationType.COUNCIL_MEMBER) {
            return new EvaluationContext(null, requireCouncilMember(topicId, evaluator.getId()));
        }
        throw new BusinessRuleException("Loại nhiệm vụ chấm không hợp lệ");
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Bạn chưa đăng nhập");
        }
        return authentication.getName();
    }

    private record EvaluationContext(ReviewerAssignment reviewerAssignment, CouncilMember councilMember) { }
}
