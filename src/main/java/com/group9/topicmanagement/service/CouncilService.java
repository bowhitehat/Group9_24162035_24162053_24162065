package com.group9.topicmanagement.service;

import com.group9.topicmanagement.domain.RegistrationPeriod;
import com.group9.topicmanagement.domain.User;
import com.group9.topicmanagement.domain.council.Council;
import com.group9.topicmanagement.domain.council.CouncilAssignment;
import com.group9.topicmanagement.domain.council.CouncilMember;
import com.group9.topicmanagement.domain.enums.CouncilMemberRole;
import com.group9.topicmanagement.domain.enums.CouncilStatus;
import com.group9.topicmanagement.domain.enums.PeriodType;
import com.group9.topicmanagement.domain.enums.RoleName;
import com.group9.topicmanagement.domain.topic.Topic;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.exception.NotFoundException;
import com.group9.topicmanagement.repository.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

@Service
@Transactional
public class CouncilService {
    private final CouncilRepository councilRepository;
    private final CouncilMemberRepository memberRepository;
    private final CouncilAssignmentRepository assignmentRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final TopicRegistrationService topicRegistrationService;

    public CouncilService(CouncilRepository councilRepository,
                          CouncilMemberRepository memberRepository,
                          CouncilAssignmentRepository assignmentRepository,
                          TopicRepository topicRepository,
                          UserRepository userRepository,
                          RegistrationPeriodRepository periodRepository,
                          TopicRegistrationService topicRegistrationService) {
        this.councilRepository = councilRepository;
        this.memberRepository = memberRepository;
        this.assignmentRepository = assignmentRepository;
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
        this.periodRepository = periodRepository;
        this.topicRegistrationService = topicRegistrationService;
    }

    @Transactional(readOnly = true)
    public List<Council> listCouncils() {
        return councilRepository.findAllWithPeriod();
    }

    @Transactional(readOnly = true)
    public List<Council> listCouncilsForUser(Long userId) {
        return councilRepository.findAllWithPeriod().stream()
                .filter(c -> memberRepository.findByCouncilIdAndMemberId(c.getId(), userId).isPresent())
                .toList();
    }

    @Transactional(readOnly = true)
    public Council getCouncil(Long id) {
        return councilRepository.findByIdWithPeriod(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy hội đồng"));
    }

    @Transactional(readOnly = true)
    public Council getCouncilForViewer(Long id, String username, boolean manager) {
        Council council = getCouncil(id);
        if (manager) {
            return council;
        }
        User viewer = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        if (!isMemberOfCouncil(id, viewer.getId())) {
            throw new AccessDeniedException("Bạn không được phân công vào hội đồng này");
        }
        return council;
    }

    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public Council createCouncil(String name, Long periodId, LocalDateTime reportDate, String location) {
        RegistrationPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đợt đăng ký"));
        validateCouncilDetails(name, location, reportDate);
        validateReportDate(period, reportDate);
        Council council = new Council();
        council.setName(name);
        council.setRegistrationPeriod(period);
        council.setReportDate(reportDate);
        council.setLocation(location);
        council.setStatus(CouncilStatus.DRAFT);
        return councilRepository.save(council);
    }

    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public Council updateCouncil(Long councilId, String name, LocalDateTime reportDate, String location) {
        Council council = getCouncil(councilId);
        checkEditable(council);
        validateCouncilDetails(name, location, reportDate);
        validateReportDate(council.getRegistrationPeriod(), reportDate);
        council.setName(name);
        council.setReportDate(reportDate);
        council.setLocation(location);
        return councilRepository.save(council);
    }

    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public void addMember(Long councilId, Long userId, CouncilMemberRole role) {
        Council council = getCouncil(councilId);
        checkDraftForMemberAndTopic(council);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giảng viên"));

        if (!user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.LECTURER)) {
            throw new BusinessRuleException("Người được thêm phải có vai trò Giảng viên");
        }

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

        for (CouncilAssignment assignment : assignmentRepository.findByCouncilId(councilId)) {
            if (assignment.getTopic().getAdvisors().stream().anyMatch(a -> a.getId().equals(user.getId()))) {
                throw new BusinessRuleException("Không phân công giảng viên chấm đề tài họ đang hướng dẫn");
            }
        }

        CouncilMember member = new CouncilMember();
        member.setCouncil(council);
        member.setMember(user);
        member.setRole(role);
        memberRepository.save(member);
    }

    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public void removeMember(Long councilId, Long memberRowId) {
        Council council = getCouncil(councilId);
        checkDraftForMemberAndTopic(council);
        CouncilMember member = memberRepository.findById(memberRowId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thành viên hội đồng"));
        if (!member.getCouncil().getId().equals(councilId)) {
            throw new BusinessRuleException("Thành viên không thuộc hội đồng này");
        }
        memberRepository.delete(member);
    }

    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public void assignTopic(Long councilId, Long topicId) {
        Council council = getCouncil(councilId);
        checkDraftForMemberAndTopic(council);

        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đề tài"));

        if (!topic.getRegistrationPeriod().getId().equals(council.getRegistrationPeriod().getId())) {
            throw new BusinessRuleException("Đề tài không thuộc đợt đăng ký của hội đồng");
        }

        topicRegistrationService.findApprovedForTopic(topicId)
                .orElseThrow(() -> new BusinessRuleException("Đề tài chưa được duyệt đăng ký"));

        if (assignmentRepository.existsByCouncilIdAndTopicId(councilId, topicId)) {
            throw new BusinessRuleException("Đề tài đã có trong hội đồng");
        }

        boolean assignedElsewhere = assignmentRepository.findByTopicIdAndCouncilStatusIn(
                topicId, List.of(CouncilStatus.DRAFT, CouncilStatus.ACTIVE)).stream()
                .anyMatch(a -> !a.getCouncil().getId().equals(councilId));
        if (assignedElsewhere) {
            throw new BusinessRuleException("Đề tài đã được phân công vào hội đồng khác");
        }

        List<CouncilMember> members = memberRepository.findByCouncilIdWithMember(councilId);
        for (CouncilMember m : members) {
            if (topic.getAdvisors().stream().anyMatch(a -> a.getId().equals(m.getMember().getId()))) {
                throw new BusinessRuleException("Không thể thêm đề tài: Giảng viên " + m.getMember().getFullName()
                        + " trong hội đồng đang hướng dẫn đề tài này.");
            }
        }

        CouncilAssignment assignment = new CouncilAssignment();
        assignment.setCouncil(council);
        assignment.setTopic(topic);
        assignmentRepository.save(assignment);
    }

    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public void removeTopic(Long councilId, Long assignmentId) {
        Council council = getCouncil(councilId);
        checkDraftForMemberAndTopic(council);
        CouncilAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phân công đề tài"));
        if (!assignment.getCouncil().getId().equals(councilId)) {
            throw new BusinessRuleException("Đề tài không thuộc hội đồng này");
        }
        assignmentRepository.delete(assignment);
    }

    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public void activateCouncil(Long councilId) {
        Council council = getCouncil(councilId);
        if (council.getStatus() != CouncilStatus.DRAFT) {
            throw new BusinessRuleException("Chỉ hội đồng nháp mới được kích hoạt");
        }
        validateCouncilDetails(council.getName(), council.getLocation(), council.getReportDate());
        validateReportDate(council.getRegistrationPeriod(), council.getReportDate());
        List<CouncilMember> members = memberRepository.findByCouncilId(councilId);
        if (members.size() < 3) {
            throw new BusinessRuleException("Hội đồng phải có từ 3 đến 5 thành viên");
        }
        if (members.size() > 5) {
            throw new BusinessRuleException("Hội đồng phải có từ 3 đến 5 thành viên");
        }
        boolean hasChair = members.stream().anyMatch(m -> m.getRole() == CouncilMemberRole.CHAIR);
        boolean hasSec = members.stream().anyMatch(m -> m.getRole() == CouncilMemberRole.SECRETARY);
        if (!hasChair) throw new BusinessRuleException("Hội đồng chưa có Chủ tịch");
        if (!hasSec) throw new BusinessRuleException("Hội đồng chưa có Thư ký");
        long chairs = members.stream().filter(m -> m.getRole() == CouncilMemberRole.CHAIR).count();
        long secs = members.stream().filter(m -> m.getRole() == CouncilMemberRole.SECRETARY).count();
        if (chairs != 1) throw new BusinessRuleException("Hội đồng phải có đúng một Chủ tịch");
        if (secs != 1) throw new BusinessRuleException("Hội đồng phải có đúng một Thư ký");

        council.setStatus(CouncilStatus.ACTIVE);
        councilRepository.save(council);
    }

    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public void completeCouncil(Long councilId) {
        Council council = getCouncil(councilId);
        if (council.getStatus() != CouncilStatus.ACTIVE) {
            throw new BusinessRuleException("Chỉ hội đồng đang hoạt động mới được hoàn tất");
        }
        council.setStatus(CouncilStatus.COMPLETED);
        councilRepository.save(council);
    }

    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public void cancelCouncil(Long councilId) {
        Council council = getCouncil(councilId);
        if (council.getStatus() == CouncilStatus.COMPLETED) {
            throw new BusinessRuleException("Không thể hủy hội đồng đã hoàn tất");
        }
        council.setStatus(CouncilStatus.CANCELLED);
        councilRepository.save(council);
    }

    @Transactional(readOnly = true)
    public List<CouncilMember> membersOf(Long councilId) {
        return memberRepository.findByCouncilIdWithMember(councilId);
    }

    @Transactional(readOnly = true)
    public List<CouncilAssignment> assignmentsOf(Long councilId) {
        return assignmentRepository.findByCouncilIdWithTopic(councilId);
    }

    @Transactional(readOnly = true)
    public boolean isChairOfTopic(Long topicId, Long userId) {
        return assignmentRepository.findByTopicIdWithCouncil(topicId).stream()
                .filter(a -> EnumSet.of(CouncilStatus.ACTIVE, CouncilStatus.COMPLETED)
                        .contains(a.getCouncil().getStatus()))
                .anyMatch(a -> memberRepository.findByCouncilIdAndMemberId(a.getCouncil().getId(), userId)
                        .filter(m -> m.getRole() == CouncilMemberRole.CHAIR)
                        .isPresent());
    }

    @Transactional(readOnly = true)
    public boolean isMemberOfCouncil(Long councilId, Long userId) {
        return memberRepository.findByCouncilIdAndMemberId(councilId, userId).isPresent();
    }

    @Transactional(readOnly = true)
    public boolean canGradeCouncil(Long councilId, String username) {
        Council council = getCouncil(councilId);
        if (council.getStatus() != CouncilStatus.ACTIVE) {
            return false;
        }
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        if (!isMemberOfCouncil(councilId, user.getId())) {
            return false;
        }
        return council.getReportDate() == null
                || !LocalDateTime.now().isAfter(council.getReportDate().toLocalDate().atTime(23, 59, 59));
    }

    public boolean canEditCouncil(Council council, boolean manager) {
        return manager && council.getStatus() != CouncilStatus.COMPLETED
                && council.getStatus() != CouncilStatus.CANCELLED;
    }

    public boolean canEditStructure(Council council, boolean manager) {
        return manager && council.getStatus() == CouncilStatus.DRAFT;
    }

    public boolean canActivate(Council council, boolean manager) {
        return manager && council.getStatus() == CouncilStatus.DRAFT;
    }

    public boolean canComplete(Council council, boolean manager) {
        return manager && council.getStatus() == CouncilStatus.ACTIVE;
    }

    public boolean canCancel(Council council, boolean manager) {
        return manager && council.getStatus() != CouncilStatus.COMPLETED
                && council.getStatus() != CouncilStatus.CANCELLED;
    }

    private void validateReportDate(RegistrationPeriod period, LocalDateTime reportDate) {
        if (period.getType() == PeriodType.KLTN && period.getCouncilDate() != null && reportDate != null
                && !reportDate.toLocalDate().equals(period.getCouncilDate())) {
            throw new BusinessRuleException("Ngày hội đồng phải khớp ngày báo cáo của đợt KLTN");
        }
    }

    private void validateCouncilDetails(String name, String location, LocalDateTime reportDate) {
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("Tên hội đồng không được để trống");
        }
        if (reportDate == null) {
            throw new BusinessRuleException("Phải nhập ngày báo cáo");
        }
        if (location == null || location.isBlank()) {
            throw new BusinessRuleException("Địa điểm không được để trống");
        }
    }

    private void checkEditable(Council council) {
        if (council.getStatus() == CouncilStatus.COMPLETED) {
            throw new BusinessRuleException("Không thể thay đổi hội đồng đã hoàn tất");
        }
        if (council.getStatus() == CouncilStatus.CANCELLED) {
            throw new BusinessRuleException("Không thể thay đổi hội đồng đã hủy");
        }
    }

    private void checkDraftForMemberAndTopic(Council council) {
        checkEditable(council);
        if (council.getStatus() == CouncilStatus.ACTIVE) {
            throw new BusinessRuleException("Không thể thay đổi thành viên hoặc đề tài sau khi hội đồng đã kích hoạt");
        }
    }
}
