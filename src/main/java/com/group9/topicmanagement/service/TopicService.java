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
import com.group9.topicmanagement.web.form.TopicForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public TopicService(TopicRepository topicRepository, 
                        RegistrationPeriodRepository periodRepository,
                        DepartmentRepository departmentRepository,
                        UserRepository userRepository) {
        this.topicRepository = topicRepository;
        this.periodRepository = periodRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
    }

    public Page<Topic> findTopicsByProposer(Long proposerId, Pageable pageable) {
        return topicRepository.findByProposerId(proposerId, pageable);
    }
    
    public Page<Topic> findTopicsWithFilters(Long periodId, Long departmentId, TopicStatus status, String keyword, Pageable pageable) {
        return topicRepository.findWithFilters(periodId, departmentId, status, keyword, pageable);
    }
    
    public Page<Topic> findAvailableTopicsForStudents(Long periodId, String keyword, Pageable pageable) {
        return topicRepository.findAvailableTopicsForStudents(periodId, keyword, pageable);
    }

    public Topic getTopicById(Long id) {
        return topicRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề tài"));
    }

    public Topic createTopic(TopicForm form, String proposerUsername) {
        User proposer = userRepository.findByUsernameIgnoreCase(proposerUsername)
                .orElseThrow(() -> new BusinessRuleException("Không tìm thấy tài khoản giảng viên"));
                
        RegistrationPeriod period = periodRepository.findById(form.getRegistrationPeriodId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đợt đăng ký"));
                
        Department department = departmentRepository.findById(form.getDepartmentId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khoa"));

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
        topic.setStatus(TopicStatus.REJECTED);
        topic.setRejectionReason(reason);
        topicRepository.save(topic);
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
