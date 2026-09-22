package com.group9.topicmanagement.service;

import com.group9.topicmanagement.domain.RegistrationPeriod;
import com.group9.topicmanagement.domain.User;
import com.group9.topicmanagement.domain.enums.PeriodStatus;
import com.group9.topicmanagement.domain.enums.PeriodType;
import com.group9.topicmanagement.domain.enums.ReviewerAssignmentStatus;
import com.group9.topicmanagement.domain.enums.RoleName;
import com.group9.topicmanagement.domain.evaluation.ReviewerAssignment;
import com.group9.topicmanagement.domain.topic.Topic;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.exception.NotFoundException;
import com.group9.topicmanagement.repository.ReviewerAssignmentRepository;
import com.group9.topicmanagement.repository.TopicRepository;
import com.group9.topicmanagement.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
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

    public ReviewerAssignmentService(ReviewerAssignmentRepository assignmentRepository,
                                     TopicRepository topicRepository,
                                     UserRepository userRepository,
                                     TopicRegistrationService topicRegistrationService) {
        this.assignmentRepository = assignmentRepository;
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
        this.topicRegistrationService = topicRegistrationService;
    }

    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public ReviewerAssignment assignReviewer(Long topicId, Long reviewerId, String assignerUsername, LocalDateTime deadline) {
        Topic topic = topicRepository.findById(topicId)
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
        return assignmentRepository.save(assignment);
    }

    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public ReviewerAssignment changeReviewer(Long assignmentId, Long newReviewerId, LocalDateTime deadline) {
        ReviewerAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phân công"));
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
        return assignmentRepository.save(assignment);
    }

    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public void cancelAssignment(Long assignmentId) {
        ReviewerAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phân công"));
        if (assignment.getStatus() == ReviewerAssignmentStatus.SUBMITTED) {
            throw new BusinessRuleException("Không thể hủy khi giảng viên đã nộp điểm");
        }
        assignment.setStatus(ReviewerAssignmentStatus.CANCELLED);
        assignmentRepository.save(assignment);
    }

    @PreAuthorize("hasRole('LECTURER')")
    public List<ReviewerAssignment> getMyAssignments(String username) {
        User reviewer = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        List<ReviewerAssignment> assignments = assignmentRepository.findByReviewerIdWithTopic(reviewer.getId());
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
        List<ReviewerAssignment> all = assignmentRepository.findAllWithDetails();
        markOverdue(all);
        return all.stream()
                .filter(a -> periodId == null || a.getTopic().getRegistrationPeriod().getId().equals(periodId))
                .filter(a -> departmentId == null || a.getTopic().getDepartment().getId().equals(departmentId))
                .filter(a -> reviewerId == null || a.getReviewer().getId().equals(reviewerId))
                .filter(a -> status == null || a.getStatus() == status)
                .filter(a -> !pendingScore || EnumSet.of(ReviewerAssignmentStatus.ASSIGNED,
                        ReviewerAssignmentStatus.IN_PROGRESS, ReviewerAssignmentStatus.OVERDUE).contains(a.getStatus()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<com.group9.topicmanagement.domain.registration.TopicRegistration> topicsWithoutReviewer() {
        return topicRegistrationService.listApprovedRegistrations().stream()
                .filter(reg -> assignmentRepository.findByTopicId(reg.getTopic().getId()).stream()
                        .noneMatch(a -> a.getStatus() != ReviewerAssignmentStatus.CANCELLED))
                .toList();
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
