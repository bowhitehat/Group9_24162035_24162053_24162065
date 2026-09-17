package com.group9.topicmanagement.service;

import com.group9.topicmanagement.domain.RegistrationPeriod;
import com.group9.topicmanagement.domain.User;
import com.group9.topicmanagement.domain.enums.PeriodStatus;
import com.group9.topicmanagement.domain.studentgroup.GroupMember;
import com.group9.topicmanagement.domain.studentgroup.StudentGroup;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.repository.GroupMemberRepository;
import com.group9.topicmanagement.repository.RegistrationPeriodRepository;
import com.group9.topicmanagement.repository.StudentGroupRepository;
import com.group9.topicmanagement.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class StudentGroupService {

    private final StudentGroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final UserRepository userRepository;

    public StudentGroupService(StudentGroupRepository groupRepository,
                               GroupMemberRepository groupMemberRepository,
                               RegistrationPeriodRepository periodRepository,
                               UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.periodRepository = periodRepository;
        this.userRepository = userRepository;
    }

    public StudentGroup createGroup(Long periodId, String leaderUsername) {
        User leader = userRepository.findByUsernameIgnoreCase(leaderUsername)
                .orElseThrow(() -> new BusinessRuleException("Không tìm thấy thông tin sinh viên"));

        RegistrationPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đợt đăng ký"));

        if (period.getStatus() != PeriodStatus.STUDENT_REGISTRATION && period.getStatus() != PeriodStatus.LECTURER_REGISTRATION) {
            throw new BusinessRuleException("Đợt đăng ký không trong thời gian cho phép tạo nhóm");
        }

        if (groupMemberRepository.existsByStudentIdAndPeriodId(leader.getId(), periodId)) {
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
        StudentGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm sinh viên"));

        if (!group.getLeader().getUsername().equalsIgnoreCase(currentUsername)) {
            throw new BusinessRuleException("Chỉ trưởng nhóm mới có quyền thêm thành viên vào nhóm");
        }

        if (group.getMembers().size() >= 3) {
            throw new BusinessRuleException("Mỗi nhóm chỉ được phép tối đa 3 sinh viên");
        }

        User newMember = userRepository.findByUsernameIgnoreCase(memberUsernameToAdd)
                .orElseThrow(() -> new BusinessRuleException("Không tìm thấy sinh viên có tên đăng nhập: " + memberUsernameToAdd));

        boolean isStudent = newMember.getRoles().stream()
                .anyMatch(role -> role.getName() == com.group9.topicmanagement.domain.enums.RoleName.STUDENT);
        if (!isStudent) {
            throw new BusinessRuleException("Tài khoản được thêm không có vai trò sinh viên");
        }

        if (groupMemberRepository.existsByStudentIdAndPeriodId(newMember.getId(), group.getRegistrationPeriod().getId())) {
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
        StudentGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm sinh viên"));

        if (!group.getLeader().getUsername().equalsIgnoreCase(currentUsername)) {
            throw new BusinessRuleException("Chỉ trưởng nhóm mới có quyền xóa thành viên khỏi nhóm");
        }

        if (group.getLeader().getId().equals(memberIdToRemove)) {
            throw new BusinessRuleException("Không thể xóa trưởng nhóm khỏi nhóm");
        }

        boolean removed = group.getMembers().removeIf(m -> m.getMember().getId().equals(memberIdToRemove));
        if (!removed) {
            throw new BusinessRuleException("Thành viên không thuộc nhóm này");
        }

        groupRepository.save(group);
    }

    public Optional<StudentGroup> findStudentGroupByStudentAndPeriod(Long studentId, Long periodId) {
        return groupRepository.findByStudentIdAndPeriodId(studentId, periodId);
    }

    public StudentGroup getGroupById(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm sinh viên"));
    }
}
