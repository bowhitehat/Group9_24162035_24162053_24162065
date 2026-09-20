package com.group9.topicmanagement.service;

import com.group9.topicmanagement.domain.RegistrationPeriod;
import com.group9.topicmanagement.domain.User;
import com.group9.topicmanagement.domain.council.Council;
import com.group9.topicmanagement.domain.council.CouncilAssignment;
import com.group9.topicmanagement.domain.council.CouncilMember;
import com.group9.topicmanagement.domain.enums.CouncilMemberRole;
import com.group9.topicmanagement.domain.enums.CouncilStatus;
import com.group9.topicmanagement.domain.topic.Topic;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.exception.NotFoundException;
import com.group9.topicmanagement.repository.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CouncilService {
    private final CouncilRepository councilRepository;
    private final CouncilMemberRepository memberRepository;
    private final CouncilAssignmentRepository assignmentRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final TopicRegistrationRepository topicRegistrationRepository;

    public CouncilService(CouncilRepository councilRepository,
                          CouncilMemberRepository memberRepository,
                          CouncilAssignmentRepository assignmentRepository,
                          TopicRepository topicRepository,
                          UserRepository userRepository,
                          RegistrationPeriodRepository periodRepository,
                          TopicRegistrationRepository topicRegistrationRepository) {
        this.councilRepository = councilRepository;
        this.memberRepository = memberRepository;
        this.assignmentRepository = assignmentRepository;
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
        this.periodRepository = periodRepository;
        this.topicRegistrationRepository = topicRegistrationRepository;
    }

    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public Council createCouncil(String name, Long periodId, LocalDateTime reportDate, String location) {
        RegistrationPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đợt đăng ký"));
        Council council = new Council();
        council.setName(name);
        council.setRegistrationPeriod(period);
        council.setReportDate(reportDate);
        council.setLocation(location);
        council.setStatus(CouncilStatus.DRAFT);
        return councilRepository.save(council);
    }

    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public void addMember(Long councilId, Long userId, CouncilMemberRole role) {
        Council council = getCouncil(councilId);
        checkNotCompleted(council);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giảng viên"));

        if (memberRepository.existsByCouncilIdAndMemberId(councilId, userId)) {
            throw new BusinessRuleException("Giảng viên đã có trong hội đồng");
        }
        
        List<CouncilMember> currentMembers = memberRepository.findByCouncilId(councilId);
        if (currentMembers.size() >= 5) {
            throw new BusinessRuleException("Hội đồng tối đa 5 thành viên");
        }

        if (role == CouncilMemberRole.CHAIR || role == CouncilMemberRole.SECRETARY) {
            boolean hasRole = currentMembers.stream().anyMatch(m -> m.getRole() == role);
            if (hasRole) {
                throw new BusinessRuleException("Hội đồng đã có " + role);
            }
        }

        CouncilMember member = new CouncilMember();
        member.setCouncil(council);
        member.setMember(user);
        member.setRole(role);
        memberRepository.save(member);
    }

    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public void assignTopic(Long councilId, Long topicId) {
        Council council = getCouncil(councilId);
        checkNotCompleted(council);

        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đề tài"));

        topicRegistrationRepository.findApprovedRegistrationForTopic(topicId)
                .orElseThrow(() -> new BusinessRuleException("Đề tài chưa được duyệt đăng ký"));

        if (assignmentRepository.existsByCouncilIdAndTopicId(councilId, topicId)) {
            throw new BusinessRuleException("Đề tài đã có trong hội đồng");
        }

        Optional<CouncilAssignment> existingAssignment = assignmentRepository.findByTopicId(topicId)
                .stream().filter(a -> a.getCouncil().getStatus() == CouncilStatus.ACTIVE || a.getCouncil().getStatus() == CouncilStatus.DRAFT)
                .findFirst();
        if (existingAssignment.isPresent()) {
            throw new BusinessRuleException("Đề tài đã được phân công vào hội đồng khác");
        }
        
        List<CouncilMember> members = memberRepository.findByCouncilId(councilId);
        for (CouncilMember m : members) {
            if (topic.getAdvisors().contains(m.getMember())) {
                throw new BusinessRuleException("Không thể thêm đề tài: Giảng viên " + m.getMember().getFullName() + " trong hội đồng đang hướng dẫn đề tài này.");
            }
        }

        CouncilAssignment assignment = new CouncilAssignment();
        assignment.setCouncil(council);
        assignment.setTopic(topic);
        assignmentRepository.save(assignment);
    }

    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public void activateCouncil(Long councilId) {
        Council council = getCouncil(councilId);
        List<CouncilMember> members = memberRepository.findByCouncilId(councilId);
        if (members.size() < 3 || members.size() > 5) {
            throw new BusinessRuleException("Hội đồng phải có từ 3 đến 5 thành viên");
        }
        boolean hasChair = members.stream().anyMatch(m -> m.getRole() == CouncilMemberRole.CHAIR);
        boolean hasSec = members.stream().anyMatch(m -> m.getRole() == CouncilMemberRole.SECRETARY);
        if (!hasChair) throw new BusinessRuleException("Hội đồng chưa có Chủ tịch");
        if (!hasSec) throw new BusinessRuleException("Hội đồng chưa có Thư ký");

        council.setStatus(CouncilStatus.ACTIVE);
        councilRepository.save(council);
    }

    private Council getCouncil(Long id) {
        return councilRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy hội đồng"));
    }

    private void checkNotCompleted(Council council) {
        if (council.getStatus() == CouncilStatus.COMPLETED) {
            throw new BusinessRuleException("Không thể thay đổi hội đồng đã hoàn tất");
        }
    }
}
