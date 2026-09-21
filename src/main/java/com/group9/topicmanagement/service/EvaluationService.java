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
import com.group9.topicmanagement.domain.topic.Topic;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.exception.NotFoundException;
import com.group9.topicmanagement.repository.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

        ReviewerAssignment reviewerAssignment = null;
        CouncilMember councilMember = null;
        if (type == EvaluationType.REVIEWER) {
            reviewerAssignment = reviewerAssignmentService.requireActiveAssignment(topicId, evaluator.getId());
        } else if (type == EvaluationType.COUNCIL_MEMBER) {
            councilMember = requireCouncilMember(topicId, evaluator.getId());
        } else {
            throw new BusinessRuleException("Loại nhiệm vụ chấm không hợp lệ");
        }

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
            throw new BusinessRuleException("Đánh giá đã nộp, không thể sửa");
        }

        evaluation.setReviewerAssignment(reviewerAssignment);
        evaluation.setCouncilMember(councilMember);
        evaluation.setComments(comments);
        evaluation = evaluationRepository.save(evaluation);

        for (EvaluationScore scoreDto : scores) {
            if (scoreDto.getScore() == null
                    || scoreDto.getScore().compareTo(BigDecimal.ZERO) < 0
                    || scoreDto.getScore().compareTo(BigDecimal.TEN) > 0) {
                throw new BusinessRuleException("Điểm phải từ 0 đến 10");
            }
            EvaluationCriterion criterion = criterionRepository.findById(scoreDto.getCriterion().getId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy tiêu chí"));

            Optional<EvaluationScore> existingScore = scoreRepository.findByEvaluationId(evaluation.getId()).stream()
                    .filter(s -> s.getCriterion().getId().equals(criterion.getId())).findFirst();

            EvaluationScore score = existingScore.orElse(new EvaluationScore());
            score.setEvaluation(evaluation);
            score.setCriterion(criterion);
            score.setScore(scoreDto.getScore());
            score.setNote(scoreDto.getNote());
            scoreRepository.save(score);
        }

        return evaluation;
    }

    @PreAuthorize("hasRole('LECTURER')")
    public void submitEvaluation(Long evaluationId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiếu chấm"));

        if (evaluation.getStatus() != EvaluationStatus.DRAFT) {
            throw new BusinessRuleException("Chỉ phiếu DRAFT mới có thể nộp");
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
    public void lockEvaluation(Long evaluationId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiếu chấm"));
        if (evaluation.getStatus() == EvaluationStatus.DRAFT) {
            throw new BusinessRuleException("Chỉ khóa phiếu đã nộp");
        }
        evaluation.setStatus(EvaluationStatus.LOCKED);
        evaluation.setLockedTime(LocalDateTime.now());
        evaluationRepository.save(evaluation);
    }

    @Transactional(readOnly = true)
    public Evaluation getOrEmpty(Long topicId, Long evaluatorId, EvaluationType type) {
        return evaluationRepository.findByTopicIdAndEvaluatorIdAndEvaluationType(topicId, evaluatorId, type)
                .orElseGet(Evaluation::new);
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

    private CouncilMember requireCouncilMember(Long topicId, Long userId) {
        List<CouncilAssignment> assignments = councilAssignmentRepository
                .findByTopicIdAndCouncilStatusIn(topicId, List.of(CouncilStatus.ACTIVE, CouncilStatus.DRAFT, CouncilStatus.COMPLETED));
        for (CouncilAssignment assignment : assignments) {
            Optional<CouncilMember> member = councilMemberRepository
                    .findByCouncilIdAndMemberId(assignment.getCouncil().getId(), userId);
            if (member.isPresent()) {
                return member.get();
            }
        }
        throw new BusinessRuleException("Không được chấm đề tài chưa được phân công");
    }
}
