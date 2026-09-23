package com.group9.topicmanagement.service;

import com.group9.topicmanagement.domain.Department;
import com.group9.topicmanagement.domain.RegistrationPeriod;
import com.group9.topicmanagement.domain.User;
import com.group9.topicmanagement.domain.topic.Topic;
import com.group9.topicmanagement.domain.topic.TopicStatus;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.repository.DepartmentRepository;
import com.group9.topicmanagement.repository.RegistrationPeriodRepository;
import com.group9.topicmanagement.repository.TopicRepository;
import com.group9.topicmanagement.repository.UserRepository;
import com.group9.topicmanagement.controller.form.TopicForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class TopicService {

    private final TopicRepository topicRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public TopicService(TopicRepository topicRepository, 
                        RegistrationPeriodRepository periodRepository,
                        DepartmentRepository departmentRepository,
                        UserRepository userRepository,
                        Clock clock) {
        this.topicRepository = topicRepository;
        this.periodRepository = periodRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    public Page<Topic> findTopicsByProposer(Long proposerId, Pageable pageable) {
        return topicRepository.findByProposerId(proposerId, pageable);
    }
    
    public Page<Topic> findTopicsWithFilters(Long periodId, Long departmentId, TopicStatus status, String keyword, Pageable pageable) {
        return topicRepository.findWithFilters(periodId, departmentId, status, keyword, pageable);
    }

    public Page<Topic> findTopicsForLecturer(Long proposerId, Long periodId, Long departmentId, TopicStatus status, String keyword, Pageable pageable) {
        return topicRepository.findByProposerWithFilters(proposerId, periodId, departmentId, status, normalizeKeyword(keyword), pageable);
    }
    
    public Page<Topic> findAvailableTopicsForStudents(Long periodId, Long departmentId, String keyword, Pageable pageable) {
        return topicRepository.findAvailableTopicsForStudents(periodId, departmentId, normalizeKeyword(keyword), pageable);
    }

    public Topic getTopicById(Long id) {
        return topicRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề tài"));
    }

    public boolean isAdvisor(Long topicId, Long userId) {
        Topic topic = getTopicById(topicId);
        return topic.getAdvisors().stream().anyMatch(advisor -> advisor.getId().equals(userId));
    }

    public long countByStatus(TopicStatus status) {
        return topicRepository.findAll().stream().filter(t -> t.getStatus() == status).count();
    }

    public Topic createTopic(TopicForm form, String proposerUsername) {
        User proposer = userRepository.findByUsernameIgnoreCase(proposerUsername)
                .orElseThrow(() -> new BusinessRuleException("Không tìm thấy tài khoản giảng viên"));
        boolean isLecturer = proposer.getRoles().stream()
                .anyMatch(role -> role.getName() == com.group9.topicmanagement.domain.enums.RoleName.LECTURER);
        if (!isLecturer) throw new BusinessRuleException("Chỉ giảng viên mới được đề xuất đề tài");
                
        RegistrationPeriod period = periodRepository.findById(form.getRegistrationPeriodId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đợt đăng ký"));
                
        Department department = departmentRepository.findById(form.getDepartmentId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khoa"));

        requireLecturerWindow(period);

        Topic topic = new Topic();
        topic.setCode(form.getCode());
        topic.setTitle(form.getTitle());
        topic.setDescription(form.getDescription());
        topic.setRequirement(form.getRequirement());
        topic.setDepartment(department);
        topic.setRegistrationPeriod(period);
        topic.setProposer(proposer);
        topic.setStatus(TopicStatus.DRAFT);
        
        setAdvisors(topic, form.getAdvisorIds());
        
        return topicRepository.save(topic);
    }

    public Topic updateTopic(Long id, TopicForm form, String username) {
        Topic topic = getTopicById(id);
        
        // Kiểm tra quyền sửa
        if (!topic.getProposer().getUsername().equalsIgnoreCase(username)) {
            throw new BusinessRuleException("Chỉ giảng viên đề xuất mới được sửa đề tài");
        }
        
        if (topic.getStatus() != TopicStatus.DRAFT && topic.getStatus() != TopicStatus.REJECTED) {
            throw new BusinessRuleException("Không thể sửa đề tài khi đã gửi duyệt hoặc công bố");
        }

        RegistrationPeriod period = periodRepository.findById(form.getRegistrationPeriodId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đợt đăng ký"));
                
        Department department = departmentRepository.findById(form.getDepartmentId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khoa"));

        requireLecturerWindow(period);

        topic.setCode(form.getCode());
        topic.setTitle(form.getTitle());
        topic.setDescription(form.getDescription());
        topic.setRequirement(form.getRequirement());
        topic.setDepartment(department);
        topic.setRegistrationPeriod(period);
        
        setAdvisors(topic, form.getAdvisorIds());
        
        return topicRepository.save(topic);
    }

    public void submitTopic(Long id, String username) {
        Topic topic = getTopicById(id);
        if (!topic.getProposer().getUsername().equalsIgnoreCase(username)) {
            throw new BusinessRuleException("Chỉ giảng viên đề xuất mới được gửi duyệt đề tài");
        }
        if (topic.getStatus() != TopicStatus.DRAFT && topic.getStatus() != TopicStatus.REJECTED) {
            throw new BusinessRuleException("Chỉ có thể gửi duyệt đề tài ở trạng thái nháp hoặc bị từ chối");
        }
        requireLecturerWindow(topic.getRegistrationPeriod());
        topic.setStatus(TopicStatus.PENDING);
        topicRepository.save(topic);
    }

    public void approveTopic(Long id) {
        Topic topic = getTopicById(id);
        if (topic.getStatus() != TopicStatus.PENDING) {
            throw new BusinessRuleException("Chỉ có thể duyệt đề tài đang chờ duyệt");
        }
        topic.setStatus(TopicStatus.APPROVED);
        topic.setRejectionReason(null);
        topicRepository.save(topic);
    }

    public void rejectTopic(Long id, String reason) {
        Topic topic = getTopicById(id);
        if (topic.getStatus() != TopicStatus.PENDING) {
            throw new BusinessRuleException("Chỉ có thể từ chối đề tài đang chờ duyệt");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("Lý do từ chối không được để trống");
        }
        if (reason.trim().length() > 500) throw new BusinessRuleException("Lý do từ chối không được vượt quá 500 ký tự");
        topic.setStatus(TopicStatus.REJECTED);
        topic.setRejectionReason(reason.trim());
        topicRepository.save(topic);
    }

    public Topic getVisibleTopic(Long id, String username, boolean manager, boolean lecturer, boolean student) {
        Topic topic = getTopicById(id);
        if (manager) return topic;
        if (student && topic.getStatus() == TopicStatus.PUBLISHED) return topic;
        if (lecturer) {
            User viewer = userRepository.findByUsernameIgnoreCase(username)
                    .orElseThrow(() -> new AccessDeniedException("Bạn không có quyền xem đề tài này"));
            if (topicRepository.existsAccessibleToLecturer(id, viewer.getId())) return topic;
        }
        if (student || lecturer) throw new AccessDeniedException("Bạn không có quyền xem đề tài này");
        throw new AccessDeniedException("Bạn không có quyền xem đề tài này");
    }

    public Topic getEditableTopic(Long id, String username) {
        Topic topic = getTopicById(id);
        if (!canEdit(topic, username)) throw new AccessDeniedException("Bạn không có quyền chỉnh sửa đề tài này");
        return topic;
    }

    public boolean canEdit(Topic topic, String username) {
        return topic.getProposer().getUsername().equalsIgnoreCase(username)
                && (topic.getStatus() == TopicStatus.DRAFT || topic.getStatus() == TopicStatus.REJECTED)
                && isInsideLecturerWindow(topic.getRegistrationPeriod());
    }

    public boolean canSubmit(Topic topic, String username) {
        return canEdit(topic, username);
    }

    public boolean canApprove(Topic topic, boolean manager) {
        return manager && topic.getStatus() == TopicStatus.PENDING;
    }

    public boolean canReject(Topic topic, boolean manager) {
        return canApprove(topic, manager);
    }

    public boolean canPublish(Topic topic, boolean manager) {
        return manager && topic.getStatus() == TopicStatus.APPROVED;
    }

    private void requireLecturerWindow(RegistrationPeriod period) {
        if (!isInsideLecturerWindow(period)) {
            throw new BusinessRuleException("Ngoài thời gian giảng viên đề xuất đề tài");
        }
    }

    private boolean isInsideLecturerWindow(RegistrationPeriod period) {
        LocalDateTime now = LocalDateTime.now(clock);
        return !now.isBefore(period.getLecturerStart()) && !now.isAfter(period.getLecturerEnd());
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    public void publishTopic(Long id) {
        Topic topic = getTopicById(id);
        if (topic.getStatus() != TopicStatus.APPROVED) {
            throw new BusinessRuleException("Chỉ có thể công bố đề tài đã được duyệt");
        }
        topic.setStatus(TopicStatus.PUBLISHED);
        topicRepository.save(topic);
    }

    private void setAdvisors(Topic topic, List<Long> advisorIds) {
        if (advisorIds != null && advisorIds.size() > 2) {
            throw new BusinessRuleException("Mỗi đề tài có tối đa 2 giảng viên hướng dẫn");
        }
        
        Set<User> advisors = new HashSet<>();
        if (advisorIds != null) {
            for (Long advisorId : advisorIds) {
                User advisor = userRepository.findById(advisorId)
                        .orElseThrow(() -> new BusinessRuleException("Không tìm thấy giảng viên hướng dẫn id: " + advisorId));
                
                boolean isLecturer = advisor.getRoles().stream()
                        .anyMatch(role -> role.getName() == com.group9.topicmanagement.domain.enums.RoleName.LECTURER);
                        
                if (!isLecturer) {
                    throw new BusinessRuleException("Tài khoản " + advisor.getUsername() + " không có vai trò giảng viên");
                }
                
                if (!advisors.add(advisor)) {
                    throw new BusinessRuleException("Giảng viên hướng dẫn không được trùng lặp");
                }
            }
        }
        topic.setAdvisors(advisors);
    }
}
