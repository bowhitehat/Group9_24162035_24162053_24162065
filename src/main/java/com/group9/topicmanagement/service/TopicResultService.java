package com.group9.topicmanagement.service;

import com.group9.topicmanagement.model.User;
import com.group9.topicmanagement.model.council.CouncilAssignment;
import com.group9.topicmanagement.model.enums.CouncilStatus;
import com.group9.topicmanagement.model.enums.EvaluationStatus;
import com.group9.topicmanagement.model.enums.TopicResultStatus;
import com.group9.topicmanagement.model.enums.EvaluationType;
import com.group9.topicmanagement.model.enums.RoleName;
import com.group9.topicmanagement.model.evaluation.Evaluation;
import com.group9.topicmanagement.model.evaluation.EvaluationCriterion;
import com.group9.topicmanagement.model.evaluation.EvaluationScore;
import com.group9.topicmanagement.model.evaluation.TopicResult;
import com.group9.topicmanagement.model.topic.Topic;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.exception.NotFoundException;
import com.group9.topicmanagement.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.group9.topicmanagement.model.studentgroup.GroupMember;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TopicResultService {
    private final TopicResultRepository resultRepository;
    private final TopicRepository topicRepository;
    private final EvaluationRepository evaluationRepository;
    private final EvaluationScoreRepository scoreRepository;
    private final EvaluationCriterionRepository criterionRepository;
    private final UserRepository userRepository;
    private final CouncilAssignmentRepository councilAssignmentRepository;
    private final CouncilService councilService;
    private final TopicRegistrationService topicRegistrationService;
    private final ReviewerAssignmentRepository reviewerAssignmentRepository;
    private final CouncilMemberRepository councilMemberRepository;
    private final TransactionalNotificationService notifications;

    public TopicResultService(TopicResultRepository resultRepository,
                              TopicRepository topicRepository,
                              EvaluationRepository evaluationRepository,
                              EvaluationScoreRepository scoreRepository,
                              EvaluationCriterionRepository criterionRepository,
                              UserRepository userRepository,
                              CouncilAssignmentRepository councilAssignmentRepository,
                              CouncilService councilService,
                              TopicRegistrationService topicRegistrationService,
                              ReviewerAssignmentRepository reviewerAssignmentRepository,
                              CouncilMemberRepository councilMemberRepository,
                              TransactionalNotificationService notifications) {
        this.resultRepository = resultRepository;
        this.topicRepository = topicRepository;
        this.evaluationRepository = evaluationRepository;
        this.scoreRepository = scoreRepository;
        this.criterionRepository = criterionRepository;
        this.userRepository = userRepository;
        this.councilAssignmentRepository = councilAssignmentRepository;
        this.councilService = councilService;
        this.topicRegistrationService = topicRegistrationService;
        this.reviewerAssignmentRepository = reviewerAssignmentRepository;
        this.councilMemberRepository = councilMemberRepository;
        this.notifications = notifications;
    }

    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public TopicResult calculateResult(Long topicId) {
        Topic topic = topicRepository.findLockedById(topicId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đề tài"));

        CouncilAssignment assignment = councilAssignmentRepository
                .findByTopicIdAndCouncil_StatusIn(topicId, List.of(CouncilStatus.ACTIVE, CouncilStatus.COMPLETED))
                .stream().findFirst()
                .orElseThrow(() -> new BusinessRuleException("Đề tài chưa được gán vào hội đồng"));

        if (!areAllEvaluationsSubmitted(topicId) || hasMissingMandatoryScores(topicId)) {
            throw new BusinessRuleException("Chưa thể tính kết quả vì còn phiếu chưa nộp hoặc thiếu điểm bắt buộc");
        }
        BigDecimal finalScore = averageOfValidEvaluations(topicId)
                .orElseThrow(() -> new BusinessRuleException("Chưa có đủ phiếu chấm hợp lệ"));

        TopicResult result = resultRepository.findByTopicId(topicId)
                .orElseGet(() -> {
                    TopicResult r = new TopicResult();
                    r.setTopic(topic);
                    r.setCouncilAssignment(assignment);
                    r.setStatus(TopicResultStatus.PENDING_CONFIRMATION);
                    return r;
                });

        if (result.getStatus() == TopicResultStatus.PUBLISHED) {
            throw new BusinessRuleException("Kết quả đã công bố không thể tính lại");
        }

        result.setCouncilAssignment(assignment);
        result.setFinalScore(finalScore);
        if (result.getStatus() == TopicResultStatus.CONFIRMED) {
            result.setStatus(TopicResultStatus.PENDING_CONFIRMATION);
            result.setConfirmer(null);
            result.setConfirmedTime(null);
        }
        return resultRepository.save(result);
    }

    @PreAuthorize("hasRole('LECTURER')")
    public void confirmResult(Long topicId, String confirmerUsername) {
        topicRepository.findLockedById(topicId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đề tài"));
        TopicResult result = resultRepository.findByTopicId(topicId)
                .orElseThrow(() -> new NotFoundException("Chưa có kết quả để xác nhận"));
        if (result.getFinalScore() == null) {
            throw new BusinessRuleException("Không thể xác nhận khi chưa có điểm");
        }
        if (result.getStatus() != TopicResultStatus.PENDING_CONFIRMATION) {
            throw new BusinessRuleException("Chỉ kết quả đang chờ xác nhận mới có thể xác nhận");
        }
        User confirmer = userRepository.findByUsernameIgnoreCase(confirmerUsername)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        if (!councilService.isChairOfTopic(topicId, confirmer.getId())) {
            throw new AccessDeniedException("Chỉ chủ tịch hội đồng của đề tài được xác nhận kết quả");
        }
        if (result.getStatus() == TopicResultStatus.PUBLISHED) {
            throw new BusinessRuleException("Kết quả đã công bố không thể sửa");
        }
        requireLockedAndCurrentResult(topicId, result);
        result.setStatus(TopicResultStatus.CONFIRMED);
        result.setConfirmer(confirmer);
        result.setConfirmedTime(LocalDateTime.now());
        resultRepository.save(result);
    }

    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public void publishResult(Long topicId, String publisherUsername) {
        topicRepository.findLockedById(topicId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đề tài"));
        TopicResult result = resultRepository.findByTopicId(topicId)
                .orElseThrow(() -> new NotFoundException("Chưa có kết quả để công bố"));
        if (result.getStatus() != TopicResultStatus.CONFIRMED) {
            throw new BusinessRuleException("Phải được Chủ tịch xác nhận trước khi công bố");
        }
        if (averageOfValidEvaluations(topicId).isEmpty() || hasMissingMandatoryScores(topicId)) {
            throw new BusinessRuleException("Không công bố khi thiếu điểm bắt buộc");
        }
        if (!areAllEvaluationsSubmitted(topicId)) {
            throw new BusinessRuleException("Chưa thể công bố vì còn phản biện hoặc thành viên hội đồng chưa nộp điểm");
        }
        requireLockedAndCurrentResult(topicId, result);
        User publisher = userRepository.findByUsernameIgnoreCase(publisherUsername)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        result.setStatus(TopicResultStatus.PUBLISHED);
        result.setPublisher(publisher);
        result.setPublishedTime(LocalDateTime.now());
        resultRepository.save(result);

        topicRegistrationService.findApprovedForTopic(topicId).ifPresent(reg -> {
            String subject = "Kết quả đề tài: " + result.getTopic().getCode();
            String body = "Kết quả đề tài " + result.getTopic().getTitle() + " đã được công bố.\nĐiểm tổng kết: " + result.getFinalScore() + "\n\nVui lòng đăng nhập hệ thống để xem chi tiết.";
            for (GroupMember member : reg.getStudentGroup().getMembers()) {
                if (member.getMember().getEmail() != null) {
                    notifications.sendAfterCommit(member.getMember().getEmail(), subject, body);
                }
            }
        });
    }

    @Transactional(readOnly = true)
    public TopicResult getPublishedResultForStudent(Long topicId, String username) {
        User student = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        if (!topicRegistrationService.isStudentOnApprovedTopic(topicId, student.getId())) {
            throw new AccessDeniedException("Sinh viên chỉ xem kết quả đề tài của nhóm mình");
        }
        TopicResult result = resultRepository.findByTopicId(topicId)
                .orElseThrow(() -> new AccessDeniedException("Sinh viên không xem được điểm chưa công bố"));
        if (result.getStatus() != TopicResultStatus.PUBLISHED) {
            throw new AccessDeniedException("Sinh viên không xem được điểm chưa công bố");
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Optional<TopicResult> findPublishedForStudent(Long topicId, String username) {
        User student = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        if (!topicRegistrationService.isStudentOnApprovedTopic(topicId, student.getId())) {
            return Optional.empty();
        }
        return resultRepository.findByTopicId(topicId)
                .filter(r -> r.getStatus() == TopicResultStatus.PUBLISHED);
    }

    @Transactional(readOnly = true)
    public List<TopicResult> listStudentResults(String username) {
        User student = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        return resultRepository.findAllByOrderByIdAsc().stream()
                .filter(r -> r.getStatus() == TopicResultStatus.PUBLISHED)
                .filter(r -> topicRegistrationService.isStudentOnApprovedTopic(r.getTopic().getId(), student.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TopicResult> listResults(org.springframework.security.core.Authentication auth) {
        boolean isManager = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY_MANAGER"));
        Long userId = userRepository.findByUsernameIgnoreCase(auth.getName()).orElseThrow().getId();

        List<TopicResult> dbResults = resultRepository.findAllByOrderByIdAsc();
        List<CouncilAssignment> assignments = councilAssignmentRepository.findAllByOrderByIdAsc();

        return assignments.stream()
            .filter(a -> isManager || councilService.isMemberOfCouncil(a.getCouncil().getId(), userId))
            .map(a -> {
                Optional<TopicResult> opt = dbResults.stream().filter(r -> r.getTopic().getId().equals(a.getTopic().getId())).findFirst();
                if (opt.isPresent()) {
                    return opt.get();
                } else {
                    TopicResult dummy = new TopicResult();
                    dummy.setTopic(a.getTopic());
                    dummy.setCouncilAssignment(a);
                    return dummy;
                }
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public TopicResult getResultForDetail(Long topicId, org.springframework.security.core.Authentication auth) {
        boolean isManager = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY_MANAGER"));
        Long userId = userRepository.findByUsernameIgnoreCase(auth.getName()).orElseThrow().getId();

        CouncilAssignment assignment = councilAssignmentRepository.findFirstByTopicIdOrderByIdDesc(topicId)
                .orElseThrow(() -> new NotFoundException("Đề tài chưa phân công hội đồng"));

        if (!isManager && !councilService.isMemberOfCouncil(assignment.getCouncil().getId(), userId)) {
            throw new AccessDeniedException("Không có quyền xem kết quả đề tài này");
        }

        return resultRepository.findByTopicId(topicId).orElseGet(() -> {
            TopicResult dummy = new TopicResult();
            dummy.setTopic(assignment.getTopic());
            dummy.setCouncilAssignment(assignment);
            return dummy;
        });
    }

    @Transactional(readOnly = true)
    public boolean canConfirm(Long topicId, String username) {
        Long userId = userRepository.findByUsernameIgnoreCase(username).orElseThrow().getId();
        if (!councilService.isChairOfTopic(topicId, userId)) return false;
        return resultRepository.findByTopicId(topicId)
            .map(r -> r.getStatus() == TopicResultStatus.PENDING_CONFIRMATION && isLockedAndCurrentResult(topicId, r))
            .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean canPublish(Long topicId, String username) {
        User user = userRepository.findByUsernameIgnoreCase(username).orElseThrow();
        if (!hasRole(user, RoleName.FACULTY_MANAGER)) return false;
        return resultRepository.findByTopicId(topicId)
            .map(r -> r.getStatus() == TopicResultStatus.CONFIRMED && isLockedAndCurrentResult(topicId, r))
            .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean canCalculate(Long topicId, String username) {
        User user = userRepository.findByUsernameIgnoreCase(username).orElseThrow();
        if (!hasRole(user, RoleName.FACULTY_MANAGER)) return false;
        if (resultRepository.findByTopicId(topicId)
                .map(r -> r.getStatus() == TopicResultStatus.PUBLISHED)
                .orElse(false)) {
            return false;
        }
        boolean hasEligibleCouncil = !councilAssignmentRepository.findByTopicIdAndCouncil_StatusIn(
                topicId, List.of(CouncilStatus.ACTIVE, CouncilStatus.COMPLETED)).isEmpty();
        return hasEligibleCouncil && areAllEvaluationsSubmitted(topicId) && !hasMissingMandatoryScores(topicId);
    }

    @Transactional(readOnly = true)
    public boolean hasMissingMandatoryScores(Long topicId) {
        List<Evaluation> evaluations = evaluationRepository.findByTopicId(topicId).stream()
                .filter(e -> e.getStatus() == EvaluationStatus.SUBMITTED || e.getStatus() == EvaluationStatus.LOCKED)
                .toList();
        if (evaluations.isEmpty()) {
            return true;
        }
        Topic topic = topicRepository.findById(topicId).orElseThrow();
        List<EvaluationCriterion> criteria = criterionRepository
                .findByRegistrationPeriodIdAndIsActiveTrueOrderByDisplayOrderAsc(topic.getRegistrationPeriod().getId())
                .stream().filter(c -> Boolean.TRUE.equals(c.getIsMandatory())).toList();
        for (Evaluation evaluation : evaluations) {
            List<EvaluationScore> scores = scoreRepository.findByEvaluationId(evaluation.getId());
            for (EvaluationCriterion criterion : criteria) {
                boolean has = scores.stream().anyMatch(s -> s.getCriterion().getId().equals(criterion.getId()));
                if (!has) {
                    return true;
                }
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public boolean areAllEvaluationsSubmitted(Long topicId) {
        for (var reviewerAssignment : reviewerAssignmentRepository.findByTopicId(topicId)) {
            if (reviewerAssignment.getStatus() == com.group9.topicmanagement.model.enums.ReviewerAssignmentStatus.CANCELLED) {
                continue;
            }
            Optional<Evaluation> reviewerEvaluation = evaluationRepository
                    .findByTopicIdAndEvaluatorIdAndEvaluationType(topicId,
                            reviewerAssignment.getReviewer().getId(), EvaluationType.REVIEWER);
            if (reviewerEvaluation.isEmpty() || !isSubmittedOrLocked(reviewerEvaluation.get())) {
                return false;
            }
        }

        // Check council members
        Optional<CouncilAssignment> assignmentOpt = councilAssignmentRepository.findByTopicIdAndCouncil_StatusIn(
            topicId, List.of(CouncilStatus.ACTIVE, CouncilStatus.COMPLETED)).stream().findFirst();

        if (assignmentOpt.isEmpty()) return false;

        Long councilId = assignmentOpt.get().getCouncil().getId();
        List<Evaluation> councilEvaluations = evaluationRepository
                .findByTopicIdAndEvaluationType(topicId, EvaluationType.COUNCIL_MEMBER);
        for (var member : councilMemberRepository.findByCouncilId(councilId)) {
            boolean submitted = councilEvaluations.stream()
                    .anyMatch(e -> e.getCouncilMember() != null
                            && e.getCouncilMember().getId().equals(member.getId())
                            && isSubmittedOrLocked(e));
            if (!submitted) return false;
        }
        return true;
    }

    public Optional<BigDecimal> averageOfValidEvaluations(Long topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đề tài"));
        List<Long> activeMandatoryCriterionIds = criterionRepository
                .findByRegistrationPeriodIdAndIsActiveTrueOrderByDisplayOrderAsc(
                        topic.getRegistrationPeriod().getId()).stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsMandatory()))
                .map(EvaluationCriterion::getId)
                .toList();
        List<Evaluation> evaluations = evaluationRepository.findByTopicId(topicId);
        BigDecimal totalScore = BigDecimal.ZERO;
        int validEvaluationCount = 0;

        for (Evaluation eval : evaluations) {
            if (eval.getStatus() != EvaluationStatus.SUBMITTED && eval.getStatus() != EvaluationStatus.LOCKED) {
                continue;
            }
            List<EvaluationScore> scores = scoreRepository.findByEvaluationId(eval.getId());
            boolean complete = activeMandatoryCriterionIds.stream()
                    .allMatch(criterionId -> scores.stream()
                            .anyMatch(score -> score.getCriterion().getId().equals(criterionId)));
            if (!complete || activeMandatoryCriterionIds.isEmpty()) {
                continue;
            }
            BigDecimal evalTotal = BigDecimal.ZERO;
            int mandatoryCount = 0;
            for (EvaluationScore score : scores) {
                if (activeMandatoryCriterionIds.contains(score.getCriterion().getId())) {
                    evalTotal = evalTotal.add(score.getScore());
                    mandatoryCount++;
                }
            }
            if (mandatoryCount > 0) {
                BigDecimal evalAvg = evalTotal.divide(BigDecimal.valueOf(mandatoryCount), 2, RoundingMode.HALF_UP);
                totalScore = totalScore.add(evalAvg);
                validEvaluationCount++;
            }
        }
        if (validEvaluationCount == 0) {
            return Optional.empty();
        }
        return Optional.of(totalScore.divide(BigDecimal.valueOf(validEvaluationCount), 2, RoundingMode.HALF_UP));
    }

    private boolean isSubmittedOrLocked(Evaluation evaluation) {
        return evaluation.getStatus() == EvaluationStatus.SUBMITTED
                || evaluation.getStatus() == EvaluationStatus.LOCKED;
    }

    private boolean isLockedAndCurrentResult(Long topicId, TopicResult result) {
        return areAllEvaluationsSubmitted(topicId) && !hasMissingMandatoryScores(topicId)
                && evaluationRepository.findByTopicId(topicId).stream().allMatch(e -> e.getStatus() == EvaluationStatus.LOCKED)
                && averageOfValidEvaluations(topicId)
                    .map(score -> result.getFinalScore() != null && score.compareTo(result.getFinalScore()) == 0).orElse(false);
    }

    private void requireLockedAndCurrentResult(Long topicId, TopicResult result) {
        if (!isLockedAndCurrentResult(topicId, result)) {
            throw new BusinessRuleException("Phải khóa tất cả phiếu chấm và tính lại điểm đúng trước khi xác nhận hoặc công bố");
        }
    }

    private boolean hasRole(User user, RoleName roleName) {
        return user.getRoles().stream().anyMatch(role -> role.getName() == roleName);
    }
}
