package com.group9.topicmanagement.service;

import com.group9.topicmanagement.model.RegistrationPeriod;
import com.group9.topicmanagement.model.User;
import com.group9.topicmanagement.model.enums.PeriodStatus;
import com.group9.topicmanagement.model.enums.PeriodType;
import com.group9.topicmanagement.model.enums.ReviewerAssignmentStatus;
import com.group9.topicmanagement.model.enums.RoleName;
import com.group9.topicmanagement.model.evaluation.ReviewerAssignment;
import com.group9.topicmanagement.model.topic.Topic;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.exception.NotFoundException;
import com.group9.topicmanagement.repository.ReviewerAssignmentRepository;
import com.group9.topicmanagement.repository.TopicRepository;
import com.group9.topicmanagement.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReviewerAssignmentService {
    private final ReviewerAssignmentRepository assignmentRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final TopicRegistrationService topicRegistrationService;
    private final TransactionalNotificationService notifications;
    @org.springframework.beans.factory.annotation.Value("${app.base-url:http://localhost:8090}")
    private String baseUrl;

    public ReviewerAssignmentService(ReviewerAssignmentRepository assignmentRepository,
                                     TopicRepository topicRepository,
                                     UserRepository userRepository,
                                     TopicRegistrationService topicRegistrationService,
                                     TransactionalNotificationService notifications) {
        this.assignmentRepository = assignmentRepository;
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
        this.topicRegistrationService = topicRegistrationService;
        this.notifications = notifications;
    }

    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReviewerAssignment assignReviewer(Long topicId, Long reviewerId, String assignerUsername, LocalDateTime deadline) {
        Topic topic = topicRepository.findLockedById(topicId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đề tài"));
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giảng viên"));
        requireLecturer(reviewer);
        User assigner = userRepository.findByUsernameIgnoreCase(assignerUsername)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người phân công"));

        topicRegistrationService.findApprovedForTopic(topicId)
                .orElseThrow(() -> new BusinessRuleException("Chỉ đề tài đã được duyệt đăng ký (APPROVED) mới được phân công phản biện"));

        if (assignmentRepository.existsByTopicIdAndReviewerId(topicId, reviewerId)) {
            ReviewerAssignment existing = assignmentRepository.findByTopicIdAndReviewerId(topicId, reviewerId).orElseThrow();
            if (existing.getStatus() != ReviewerAssignmentStatus.CANCELLED) {
                throw new BusinessRuleException("Giảng viên đã được phân công phản biện đề tài này");
            }
        }

        if (topic.getAdvisors().stream().anyMatch(a -> a.getId().equals(reviewer.getId()))) {
            throw new BusinessRuleException("Giảng viên không được phản biện đề tài mình đang hướng dẫn");
        }

        PeriodStatus periodStatus = topic.getRegistrationPeriod().getStatus();
        if (periodStatus != PeriodStatus.IN_PROGRESS && periodStatus != PeriodStatus.GRADING) {
            throw new BusinessRuleException("Đợt đăng ký chưa trong giai đoạn cho phép phản biện");
        }

        validateDeadline(topic.getRegistrationPeriod(), deadline);

        ReviewerAssignment assignment = assignmentRepository.findByTopicIdAndReviewerId(topicId, reviewerId)
                .orElseGet(ReviewerAssignment::new);
        assignment.setTopic(topic);
        assignment.setReviewer(reviewer);
        assignment.setAssigner(assigner);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setDeadline(deadline);
        assignment.setStatus(ReviewerAssignmentStatus.ASSIGNED);
        assignment.setSubmissionTime(null);
        ReviewerAssignment saved = assignmentRepository.save(assignment);

        if (reviewer.getEmail() != null) {
            String subject = "Phân công phản biện đề tài " + topic.getCode();
            String body = "Bạn được phân công phản biện đề tài: " + topic.getTitle() + "\n"
                    + "Hạn nộp điểm: " + deadline + "\n"
                    + "Link truy cập: " + baseUrl.replaceAll("/+$", "") + "/evaluations/topic/" + topic.getId() + "?type=REVIEWER";
            notifications.sendAfterCommit(reviewer.getEmail(), subject, body);
        }

        return saved;
    }

    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReviewerAssignment changeReviewer(Long assignmentId, Long newReviewerId, LocalDateTime deadline) {
        ReviewerAssignment assignment = lockAssignment(assignmentId);
        if (assignment.getStatus() == ReviewerAssignmentStatus.SUBMITTED) {
            throw new BusinessRuleException("Không thể đổi giảng viên khi đã nộp điểm");
        }
        if (assignment.getStatus() == ReviewerAssignmentStatus.CANCELLED) {
            throw new BusinessRuleException("Không thể đổi giảng viên của phân công đã hủy");
        }
        User reviewer = userRepository.findById(newReviewerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giảng viên"));
        requireLecturer(reviewer);
        if (assignment.getTopic().getAdvisors().stream().anyMatch(a -> a.getId().equals(reviewer.getId()))) {
            throw new BusinessRuleException("Giảng viên không được phản biện đề tài mình đang hướng dẫn");
        }
        if (!reviewer.getId().equals(assignment.getReviewer().getId())
                && assignmentRepository.existsByTopicIdAndReviewerId(assignment.getTopic().getId(), newReviewerId)) {
            throw new BusinessRuleException("Giảng viên đã được phân công phản biện đề tài này");
        }
        if (deadline != null) {
            validateDeadline(assignment.getTopic().getRegistrationPeriod(), deadline);
            assignment.setDeadline(deadline);
        }
        assignment.setReviewer(reviewer);
        assignment.setStatus(ReviewerAssignmentStatus.ASSIGNED);
        ReviewerAssignment saved = assignmentRepository.save(assignment);

        if (reviewer.getEmail() != null) {
            String subject = "Phân công phản biện đề tài " + assignment.getTopic().getCode();
            String body = "Bạn được phân công phản biện đề tài: " + assignment.getTopic().getTitle() + "\n"
                    + "Hạn nộp điểm: " + assignment.getDeadline() + "\n"
                    + "Link truy cập: " + baseUrl.replaceAll("/+$", "") + "/evaluations/topic/" + assignment.getTopic().getId() + "?type=REVIEWER";
            notifications.sendAfterCommit(reviewer.getEmail(), subject, body);
        }

        return saved;
    }

    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cancelAssignment(Long assignmentId) {
        ReviewerAssignment assignment = lockAssignment(assignmentId);
        if (assignment.getStatus() == ReviewerAssignmentStatus.SUBMITTED) {
            throw new BusinessRuleException("Không thể hủy khi giảng viên đã nộp điểm");
        }
        assignment.setStatus(ReviewerAssignmentStatus.CANCELLED);
        assignmentRepository.save(assignment);
    }

    private ReviewerAssignment lockAssignment(Long id) {
        Long topicId = assignmentRepository.findTopicIdById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phân công"));
        topicRepository.findLockedById(topicId).orElseThrow(() -> new NotFoundException("Không tìm thấy đề tài"));
        return assignmentRepository.findLockedById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phân công"));
    }

    @PreAuthorize("hasRole('LECTURER')")
    public List<ReviewerAssignment> getMyAssignments(String username) {
        User reviewer = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        List<ReviewerAssignment> assignments = assignmentRepository.findByReviewerId(reviewer.getId());
        markOverdue(assignments);
        return assignments;
    }

    @Transactional(readOnly = true)
    public Set<Long> gradableAssignmentIds(List<ReviewerAssignment> assignments) {
        LocalDateTime now = LocalDateTime.now();
        return assignments.stream()
                .filter(a -> a.getStatus() != ReviewerAssignmentStatus.CANCELLED)
                .filter(a -> a.getStatus() != ReviewerAssignmentStatus.OVERDUE)
                .filter(a -> a.getDeadline() == null || !now.isAfter(a.getDeadline()))
                .map(ReviewerAssignment::getId)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public Set<Long> modifiableAssignmentIds(List<ReviewerAssignment> assignments) {
        return assignments.stream()
                .filter(a -> a.getStatus() != ReviewerAssignmentStatus.SUBMITTED)
                .filter(a -> a.getStatus() != ReviewerAssignmentStatus.CANCELLED)
                .map(ReviewerAssignment::getId)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public List<ReviewerAssignment> listAssignments(Long periodId, Long departmentId, Long reviewerId,
                                                    ReviewerAssignmentStatus status, boolean missingReviewer,
                                                    boolean pendingScore) {
        List<ReviewerAssignment> all = assignmentRepository.findWithFilters(
                periodId, departmentId, reviewerId, status, pendingScore, LocalDateTime.now());
        markOverdue(all);
        return all;
    }

    @Transactional(readOnly = true)
    public List<com.group9.topicmanagement.model.registration.TopicRegistration> topicsWithoutReviewer() {
        return topicRegistrationService.listApprovedWithoutReviewer();
    }

    public void markSubmitted(Long topicId, Long reviewerId) {
        assignmentRepository.findByTopicIdAndReviewerId(topicId, reviewerId).ifPresent(a -> {
            a.setStatus(ReviewerAssignmentStatus.SUBMITTED);
            a.setSubmissionTime(LocalDateTime.now());
            assignmentRepository.save(a);
        });
    }

    public void markInProgress(Long topicId, Long reviewerId) {
        assignmentRepository.findByTopicIdAndReviewerId(topicId, reviewerId).ifPresent(a -> {
            if (a.getStatus() == ReviewerAssignmentStatus.ASSIGNED) {
                a.setStatus(ReviewerAssignmentStatus.IN_PROGRESS);
                assignmentRepository.save(a);
            }
        });
    }

    public void markInProgressForEdit(Long topicId, Long reviewerId) {
        assignmentRepository.findByTopicIdAndReviewerId(topicId, reviewerId).ifPresent(a -> {
            if (a.getStatus() == ReviewerAssignmentStatus.ASSIGNED
                    || a.getStatus() == ReviewerAssignmentStatus.SUBMITTED) {
                a.setStatus(ReviewerAssignmentStatus.IN_PROGRESS);
                a.setSubmissionTime(null);
                assignmentRepository.save(a);
            }
        });
    }

    @Transactional(readOnly = true)
    public ReviewerAssignment requireActiveAssignment(Long topicId, Long reviewerId) {
        ReviewerAssignment assignment = assignmentRepository.findByTopicIdAndReviewerId(topicId, reviewerId)
                .orElseThrow(() -> new BusinessRuleException("Không được chấm đề tài chưa được phân công"));
        if (assignment.getStatus() == ReviewerAssignmentStatus.CANCELLED) {
            throw new BusinessRuleException("Không được chấm đề tài chưa được phân công");
        }
        return assignment;
    }

    private void markOverdue(List<ReviewerAssignment> assignments) {
        LocalDateTime now = LocalDateTime.now();
        for (ReviewerAssignment assignment : assignments) {
            if ((assignment.getStatus() == ReviewerAssignmentStatus.ASSIGNED
                    || assignment.getStatus() == ReviewerAssignmentStatus.IN_PROGRESS)
                    && assignment.getDeadline() != null && now.isAfter(assignment.getDeadline())) {
                assignment.setStatus(ReviewerAssignmentStatus.OVERDUE);
                assignmentRepository.save(assignment);
            }
        }
    }

    private void validateDeadline(RegistrationPeriod period, LocalDateTime deadline) {
        if (deadline == null) {
            throw new BusinessRuleException("Phải nhập hạn nộp điểm");
        }
        if (!deadline.isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Hạn nộp điểm phải ở tương lai");
        }
        if ((period.getType() == PeriodType.TLCN || period.getType() == PeriodType.KLTN)
                && period.getReviewDeadline() != null
                && deadline.isAfter(period.getReviewDeadline())) {
            throw new BusinessRuleException("Hạn nộp điểm không được sau hạn GVPB của đợt");
        }
    }

    private void requireLecturer(User user) {
        if (user.getRoles().stream().noneMatch(role -> role.getName() == RoleName.LECTURER)) {
            throw new BusinessRuleException("Người được phân công phản biện phải có vai trò Giảng viên");
        }
    }
}
