package com.group9.topicmanagement.service;

import com.group9.topicmanagement.model.RegistrationPeriod;
import com.group9.topicmanagement.model.User;
import com.group9.topicmanagement.model.enums.PeriodStatus;
import com.group9.topicmanagement.model.studentgroup.GroupMember;
import com.group9.topicmanagement.model.studentgroup.StudentGroup;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.repository.GroupMemberRepository;
import com.group9.topicmanagement.repository.RegistrationPeriodRepository;
import com.group9.topicmanagement.repository.StudentGroupRepository;
import com.group9.topicmanagement.repository.TopicRegistrationRepository;
import com.group9.topicmanagement.repository.UserRepository;
import com.group9.topicmanagement.model.enums.RegistrationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class StudentGroupService {

    public static final int MIN_MEMBERS_FOR_REGISTRATION = 3;
    public static final int MAX_MEMBERS = 5;

    private final StudentGroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final UserRepository userRepository;
    private final TopicRegistrationRepository registrationRepository;
    private final Clock clock;

    public StudentGroupService(StudentGroupRepository groupRepository,
                               GroupMemberRepository groupMemberRepository,
                               RegistrationPeriodRepository periodRepository,
                               UserRepository userRepository,
                               TopicRegistrationRepository registrationRepository,
                               Clock clock) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.periodRepository = periodRepository;
        this.userRepository = userRepository;
        this.registrationRepository = registrationRepository;
        this.clock = clock;
    }

    public StudentGroup createGroup(Long periodId, String leaderUsername) {
        User leaderCandidate = userRepository.findByUsernameIgnoreCase(leaderUsername)
                .orElseThrow(() -> new BusinessRuleException("Không tìm thấy thông tin sinh viên"));
        User leader = userRepository.findLockedById(leaderCandidate.getId())
                .orElseThrow(() -> new BusinessRuleException("Không tìm thấy thông tin sinh viên"));

        RegistrationPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đợt đăng ký"));

        if (period.getStatus() != PeriodStatus.STUDENT_REGISTRATION) {
            throw new BusinessRuleException("Đợt đăng ký không trong thời gian cho phép tạo nhóm");
        }
        requireStudentWindow(period);

        boolean isStudent = leader.getRoles().stream()
                .anyMatch(role -> role.getName() == com.group9.topicmanagement.model.enums.RoleName.STUDENT);
        if (!isStudent) throw new BusinessRuleException("Chỉ sinh viên mới được tạo nhóm");

        if (groupMemberRepository.existsByMember_IdAndGroup_RegistrationPeriod_Id(leader.getId(), periodId)) {
            throw new BusinessRuleException("Sinh viên đã thuộc một nhóm khác trong cùng đợt đăng ký");
        }

        StudentGroup group = new StudentGroup();
        group.setRegistrationPeriod(period);
        group.setLeader(leader);

        GroupMember leaderMember = new GroupMember();
        leaderMember.setGroup(group);
        leaderMember.setMember(leader);
        leaderMember.setLeader(true);

        group.getMembers().add(leaderMember);

        return groupRepository.save(group);
    }

    public void addMember(Long groupId, String memberUsernameToAdd, String currentUsername) {
        StudentGroup group = groupRepository.findLockedById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm sinh viên"));

        if (!group.getLeader().getUsername().equalsIgnoreCase(currentUsername)) {
            throw new BusinessRuleException("Chỉ trưởng nhóm mới có quyền thêm thành viên vào nhóm");
        }
        requireStudentWindow(group.getRegistrationPeriod());

        if (group.getMembers().size() >= MAX_MEMBERS) {
            throw new BusinessRuleException("Mỗi nhóm chỉ được phép tối đa 5 sinh viên");
        }

        User candidate = userRepository.findByUsernameIgnoreCase(memberUsernameToAdd)
                .orElseThrow(() -> new BusinessRuleException("Không tìm thấy sinh viên có tên đăng nhập: " + memberUsernameToAdd));
        User newMember = userRepository.findLockedById(candidate.getId())
                .orElseThrow(() -> new BusinessRuleException("Không tìm thấy sinh viên có tên đăng nhập: " + memberUsernameToAdd));

        boolean isStudent = newMember.getRoles().stream()
                .anyMatch(role -> role.getName() == com.group9.topicmanagement.model.enums.RoleName.STUDENT);
        if (!isStudent) {
            throw new BusinessRuleException("Tài khoản được thêm không có vai trò sinh viên");
        }

        if (groupMemberRepository.existsByMember_IdAndGroup_RegistrationPeriod_Id(newMember.getId(), group.getRegistrationPeriod().getId())) {
            throw new BusinessRuleException("Sinh viên " + memberUsernameToAdd + " đã thuộc một nhóm khác trong cùng đợt");
        }

        GroupMember groupMember = new GroupMember();
        groupMember.setGroup(group);
        groupMember.setMember(newMember);
        groupMember.setLeader(false);

        group.getMembers().add(groupMember);
        groupRepository.save(group);
    }

    public void removeMember(Long groupId, Long memberIdToRemove, String currentUsername) {
        StudentGroup group = groupRepository.findLockedById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm sinh viên"));

        if (!group.getLeader().getUsername().equalsIgnoreCase(currentUsername)) {
            throw new BusinessRuleException("Chỉ trưởng nhóm mới có quyền xóa thành viên khỏi nhóm");
        }
        requireStudentWindow(group.getRegistrationPeriod());

        if (group.getLeader().getId().equals(memberIdToRemove)) {
            throw new BusinessRuleException("Không thể xóa trưởng nhóm khỏi nhóm");
        }

        boolean hasApprovedRegistration = group.getId() != null
                && registrationRepository.existsByStudentGroup_IdAndStatus(group.getId(), RegistrationStatus.APPROVED);
        if (hasApprovedRegistration && group.getMembers().size() - 1 < MIN_MEMBERS_FOR_REGISTRATION) {
            throw new BusinessRuleException("Không thể thay đổi nhóm làm đăng ký đã duyệt còn dưới 3 sinh viên");
        }

        boolean removed = group.getMembers().removeIf(m -> m.getMember().getId().equals(memberIdToRemove));
        if (!removed) {
            throw new BusinessRuleException("Thành viên không thuộc nhóm này");
        }

        groupRepository.save(group);
    }

    public Optional<StudentGroup> findStudentGroupByStudentAndPeriod(Long studentId, Long periodId) {
        return groupRepository.findDistinctByMembers_Member_IdAndRegistrationPeriod_Id(studentId, periodId);
    }

    public StudentGroup getGroupById(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm sinh viên"));
    }

    public boolean canCreateGroup(Long periodId, String username) {
        if (periodId == null) return false;
        Optional<User> student = userRepository.findByUsernameIgnoreCase(username);
        Optional<RegistrationPeriod> period = periodRepository.findById(periodId);
        if (student.isEmpty() || period.isEmpty()) return false;
        return period.get().getStatus() == PeriodStatus.STUDENT_REGISTRATION
                && isInsideStudentWindow(period.get())
                && !groupMemberRepository.existsByMember_IdAndGroup_RegistrationPeriod_Id(student.get().getId(), periodId);
    }

    public boolean canManageGroup(StudentGroup group, String username) {
        return group != null
                && group.getLeader().getUsername().equalsIgnoreCase(username)
                && group.getRegistrationPeriod().getStatus() == PeriodStatus.STUDENT_REGISTRATION
                && isInsideStudentWindow(group.getRegistrationPeriod());
    }

    public boolean canAddMember(StudentGroup group, String username) {
        return canManageGroup(group, username) && group.getMembers().size() < MAX_MEMBERS;
    }

    public Set<Long> removableMemberIds(StudentGroup group, String username) {
        if (!canManageGroup(group, username)) return Set.of();
        boolean approvedAtMinimumSize = group.getId() != null
                && group.getMembers().size() <= MIN_MEMBERS_FOR_REGISTRATION
                && registrationRepository.existsByStudentGroup_IdAndStatus(group.getId(), RegistrationStatus.APPROVED);
        if (approvedAtMinimumSize) return Set.of();
        return group.getMembers().stream()
                .filter(member -> !member.isLeader())
                .map(member -> member.getMember().getId())
                .collect(Collectors.toUnmodifiableSet());
    }

    private void requireStudentWindow(RegistrationPeriod period) {
        if (period.getStatus() != PeriodStatus.STUDENT_REGISTRATION || !isInsideStudentWindow(period)) {
            throw new BusinessRuleException("Ngoài giai đoạn sinh viên được phép quản lý nhóm");
        }
    }

    private boolean isInsideStudentWindow(RegistrationPeriod period) {
        LocalDateTime now = LocalDateTime.now(clock);
        return !now.isBefore(period.getStudentStart()) && !now.isAfter(period.getStudentEnd());
    }
}
