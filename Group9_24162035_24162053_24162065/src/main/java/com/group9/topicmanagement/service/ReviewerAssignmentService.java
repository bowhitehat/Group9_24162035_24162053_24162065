package com.group9.topicmanagement.service;

import com.group9.topicmanagement.domain.User;
import com.group9.topicmanagement.domain.evaluation.ReviewerAssignment;
import com.group9.topicmanagement.domain.enums.ReviewerAssignmentStatus;
import com.group9.topicmanagement.domain.registration.TopicRegistration;
import com.group9.topicmanagement.domain.topic.Topic;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.exception.NotFoundException;
import com.group9.topicmanagement.repository.ReviewerAssignmentRepository;
import com.group9.topicmanagement.repository.TopicRegistrationRepository;
import com.group9.topicmanagement.repository.TopicRepository;
import com.group9.topicmanagement.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ReviewerAssignmentService {
    private final ReviewerAssignmentRepository assignmentRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final TopicRegistrationRepository registrationRepository;

    public ReviewerAssignmentService(ReviewerAssignmentRepository assignmentRepository,
                                     TopicRepository topicRepository,
                                     UserRepository userRepository,
                                     TopicRegistrationRepository registrationRepository) {
        this.assignmentRepository = assignmentRepository;
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
        this.registrationRepository = registrationRepository;
    }

    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public ReviewerAssignment assignReviewer(Long topicId, Long reviewerId, String assignerUsername, LocalDateTime deadline) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đề tài"));
        User reviewer = userRepository.findById(reviewerId).orElseThrow(() -> new NotFoundException("Không tìm thấy giảng viên"));
        User assigner = userRepository.findByUsernameIgnoreCase(assignerUsername)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người phân công"));

        TopicRegistration registration = registrationRepository.findApprovedRegistrationForTopic(topicId)
                .orElseThrow(() -> new BusinessRuleException("Chỉ đề tài đã được duyệt đăng ký (APPROVED) mới được phân công phản biện"));

        if (assignmentRepository.existsByTopicIdAndReviewerId(topicId, reviewerId)) {
            throw new BusinessRuleException("Giảng viên đã được phân công phản biện đề tài này");
        }

        if (topic.getAdvisors().contains(reviewer)) {
            throw new BusinessRuleException("Giảng viên không được phản biện đề tài mình đang hướng dẫn");
        }

        String periodStatus = topic.getRegistrationPeriod().getStatus().name();
        if (!"IN_PROGRESS".equals(periodStatus) && !"GRADING".equals(periodStatus)) {
            throw new BusinessRuleException("Đợt đăng ký chưa trong giai đoạn cho phép phản biện");
        }

        ReviewerAssignment assignment = new ReviewerAssignment();
        assignment.setTopic(topic);
        assignment.setReviewer(reviewer);
        assignment.setAssigner(assigner);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setDeadline(deadline);
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
        
        List<ReviewerAssignment> assignments = assignmentRepository.findByReviewerId(reviewer.getId());
        LocalDateTime now = LocalDateTime.now();
        for (ReviewerAssignment assignment : assignments) {
            if ((assignment.getStatus() == ReviewerAssignmentStatus.ASSIGNED || assignment.getStatus() == ReviewerAssignmentStatus.IN_PROGRESS) 
                && assignment.getDeadline() != null && now.isAfter(assignment.getDeadline())) {
                assignment.setStatus(ReviewerAssignmentStatus.OVERDUE);
                assignmentRepository.save(assignment);
            }
        }
        return assignments;
    }
}
