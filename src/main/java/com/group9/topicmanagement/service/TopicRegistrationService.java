package com.group9.topicmanagement.service;

import com.group9.topicmanagement.domain.RegistrationPeriod;
import com.group9.topicmanagement.domain.User;
import com.group9.topicmanagement.domain.enums.PeriodStatus;
import com.group9.topicmanagement.domain.enums.RegistrationStatus;
import com.group9.topicmanagement.domain.registration.TopicRegistration;
import com.group9.topicmanagement.domain.studentgroup.StudentGroup;
import com.group9.topicmanagement.domain.topic.Topic;
import com.group9.topicmanagement.domain.topic.TopicStatus;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class TopicRegistrationService {

    private final TopicRegistrationRepository registrationRepository;
    private final StudentGroupRepository groupRepository;
    private final TopicRepository topicRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public TopicRegistrationService(TopicRegistrationRepository registrationRepository,
                                     StudentGroupRepository groupRepository,
                                     TopicRepository topicRepository,
                                     RegistrationPeriodRepository periodRepository,
                                     UserRepository userRepository,
                                     Clock clock) {
        this.registrationRepository = registrationRepository;
        this.groupRepository = groupRepository;
        this.topicRepository = topicRepository;
        this.periodRepository = periodRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    public TopicRegistration registerTopic(Long groupId, Long topicId, String leaderUsername) {
        StudentGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm sinh viên"));

        if (!group.getLeader().getUsername().equalsIgnoreCase(leaderUsername)) {
            throw new BusinessRuleException("Chỉ trưởng nhóm mới được quyền đăng ký đề tài");
        }

        RegistrationPeriod period = group.getRegistrationPeriod();
        if (period.getStatus() != PeriodStatus.STUDENT_REGISTRATION) {
            throw new BusinessRuleException("Hiện tại không trong thời hạn đăng ký đề tài cho sinh viên");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (now.isBefore(period.getStudentStart()) || now.isAfter(period.getStudentEnd())) {
            throw new BusinessRuleException("Đã hết thời hạn đăng ký đề tài");
        }

        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề tài"));

        if (!topic.getRegistrationPeriod().getId().equals(period.getId())) {
            throw new BusinessRuleException("Đề tài không thuộc cùng đợt đăng ký của nhóm");
        }

        if (topic.getStatus() != TopicStatus.PUBLISHED) {
            throw new BusinessRuleException("Đề tài chưa được công bố để đăng ký");
        }

        // Kiểm tra đề tài đã có nhóm khác đăng ký thành công (APPROVED) chưa
        Optional<TopicRegistration> existingApproved = registrationRepository.findApprovedRegistrationForTopic(topicId);
        if (existingApproved.isPresent()) {
            throw new BusinessRuleException("Đề tài này đã được đăng ký thành công bởi một nhóm khác");
        }

        // Kiểm tra nhóm đã đăng ký đề tài nào trong đợt chưa
        Optional<TopicRegistration> groupRegistration = registrationRepository.findByStudentGroupIdAndRegistrationPeriodId(groupId, period.getId());
        if (groupRegistration.isPresent()) {
            TopicRegistration existingReg = groupRegistration.get();
            if (existingReg.getStatus() == RegistrationStatus.APPROVED || existingReg.getStatus() == RegistrationStatus.PENDING) {
                throw new BusinessRuleException("Nhóm đã đăng ký đề tài trong đợt này và đang chờ duyệt hoặc đã được phê duyệt");
            }
        }

        TopicRegistration registration = groupRegistration.orElseGet(TopicRegistration::new);
        registration.setStudentGroup(group);
        registration.setTopic(topic);
        registration.setRegistrationPeriod(period);
        registration.setStatus(RegistrationStatus.PENDING);
        registration.setRejectionReason(null);
        registration.setApprover(null);
        registration.setApprovedAt(null);

        return registrationRepository.save(registration);
    }

    public void approveRegistration(Long registrationId, String approverUsername) {
        TopicRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin đăng ký đề tài"));

        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new BusinessRuleException("Chỉ có thể phê duyệt đăng ký ở trạng thái chờ duyệt");
        }

        User approver = userRepository.findByUsernameIgnoreCase(approverUsername)
                .orElseThrow(() -> new BusinessRuleException("Không tìm thấy người phê duyệt"));

        // Ngăn 2 nhóm đăng ký thành công cùng 1 đề tài
        Optional<TopicRegistration> existingApproved = registrationRepository.findApprovedRegistrationForTopic(registration.getTopic().getId());
        if (existingApproved.isPresent() && !existingApproved.get().getId().equals(registrationId)) {
            throw new BusinessRuleException("Đề tài này đã được phê duyệt cho nhóm khác");
        }

        registration.setStatus(RegistrationStatus.APPROVED);
        registration.setApprover(approver);
        registration.setApprovedAt(LocalDateTime.now(clock));
        registration.setRejectionReason(null);

        registrationRepository.save(registration);
    }

    public void rejectRegistration(Long registrationId, String reason, String approverUsername) {
        TopicRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin đăng ký đề tài"));

        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new BusinessRuleException("Chỉ có thể từ chối đăng ký ở trạng thái chờ duyệt");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("Lý do từ chối không được để trống");
        }
        if (reason.trim().length() > 500) throw new BusinessRuleException("Lý do từ chối không được vượt quá 500 ký tự");

        User approver = userRepository.findByUsernameIgnoreCase(approverUsername)
                .orElseThrow(() -> new BusinessRuleException("Không tìm thấy người thực hiện từ chối"));

        registration.setStatus(RegistrationStatus.REJECTED);
        registration.setApprover(approver);
        registration.setRejectionReason(reason.trim());

        registrationRepository.save(registration);
    }

    public Page<TopicRegistration> findRegistrationsWithFilters(Long periodId, Long departmentId, RegistrationStatus status, String keyword, Pageable pageable) {
        return registrationRepository.findWithFilters(periodId, departmentId, status, normalizeKeyword(keyword), pageable);
    }

    public Page<TopicRegistration> findRegistrationsForStudent(String username, Long periodId, RegistrationStatus status, String keyword, Pageable pageable) {
        return registrationRepository.findForStudent(username, periodId, status, normalizeKeyword(keyword), pageable);
    }

    public Set<Long> reviewableRegistrationIds(List<TopicRegistration> registrations, boolean manager) {
        if (!manager) return Set.of();
        return registrations.stream()
                .filter(registration -> registration.getStatus() == RegistrationStatus.PENDING)
                .map(TopicRegistration::getId)
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean canRegisterTopic(Long groupId, Long topicId, String username) {
        if (groupId == null || topicId == null) return false;
        Optional<StudentGroup> groupOpt = groupRepository.findById(groupId);
        Optional<Topic> topicOpt = topicRepository.findById(topicId);
        if (groupOpt.isEmpty() || topicOpt.isEmpty()) return false;
        StudentGroup group = groupOpt.get();
        Topic topic = topicOpt.get();
        RegistrationPeriod period = group.getRegistrationPeriod();
        LocalDateTime now = LocalDateTime.now(clock);
        if (!group.getLeader().getUsername().equalsIgnoreCase(username)
                || topic.getStatus() != TopicStatus.PUBLISHED
                || !topic.getRegistrationPeriod().getId().equals(period.getId())
                || period.getStatus() != PeriodStatus.STUDENT_REGISTRATION
                || now.isBefore(period.getStudentStart())
                || now.isAfter(period.getStudentEnd())
                || registrationRepository.findApprovedRegistrationForTopic(topicId).isPresent()) return false;
        Optional<TopicRegistration> existing = registrationRepository.findByStudentGroupIdAndRegistrationPeriodId(groupId, period.getId());
        return existing.isEmpty() || (existing.get().getStatus() != RegistrationStatus.PENDING
                && existing.get().getStatus() != RegistrationStatus.APPROVED);
    }

    public TopicRegistration getRegistrationById(Long id) {
        return registrationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký đề tài"));
    }

    public Optional<TopicRegistration> findApprovedForTopic(Long topicId) {
        return registrationRepository.findApprovedRegistrationForTopic(topicId);
    }

    public List<TopicRegistration> listApprovedRegistrations() {
        return registrationRepository.findByStatusWithTopicAndGroup(RegistrationStatus.APPROVED);
    }

    public long countApprovedRegistrations() {
        return registrationRepository.countByStatus(RegistrationStatus.APPROVED);
    }

    public boolean isStudentOnApprovedTopic(Long topicId, Long studentId) {
        return findApprovedForTopic(topicId)
                .map(reg -> reg.getStudentGroup().getMembers().stream()
                        .anyMatch(m -> m.getMember().getId().equals(studentId)))
                .orElse(false);
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }
}
