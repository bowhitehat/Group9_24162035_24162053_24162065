package com.group9.topicmanagement.service;

import com.group9.topicmanagement.domain.Department;
import com.group9.topicmanagement.domain.Role;
import com.group9.topicmanagement.domain.User;
import com.group9.topicmanagement.domain.enums.RoleName;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.exception.NotFoundException;
import com.group9.topicmanagement.repository.DepartmentRepository;
import com.group9.topicmanagement.repository.RoleRepository;
import com.group9.topicmanagement.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final DepartmentRepository departments;
    private final PasswordEncoder encoder;

    public UserService(UserRepository users, RoleRepository roles, DepartmentRepository departments, PasswordEncoder encoder) {
        this.users = users;
        this.roles = roles;
        this.departments = departments;
        this.encoder = encoder;
    }

    public List<User> search(String keyword) {
        String value = keyword == null ? "" : keyword.trim();
        return value.isBlank() ? users.findAllByOrderByFullName()
            : users.findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrderByFullName(value, value);
    }

    public User getByUsername(String username) {
        return users.findByUsernameIgnoreCase(username).orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản"));
    }

    @Transactional
    public User create(String username, String fullName, String email, String studentCode, Long departmentId, Set<RoleName> roleNames, String rawPassword) {
        if (users.existsByUsernameIgnoreCase(username)) throw new BusinessRuleException("Tên đăng nhập đã tồn tại");
        if (users.existsByEmailIgnoreCase(email)) throw new BusinessRuleException("Email đã tồn tại");
        if (roleNames == null || roleNames.isEmpty()) throw new BusinessRuleException("Tài khoản phải có ít nhất một vai trò");
        User user = new User();
        user.setUsername(username.trim());
        user.setFullName(fullName.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setStudentCode(studentCode == null || studentCode.isBlank() ? null : studentCode.trim());
        user.setPasswordHash(encoder.encode(rawPassword));
        if (departmentId != null) user.setDepartment(departments.findById(departmentId).orElseThrow(() -> new NotFoundException("Không tìm thấy bộ môn")));
        user.setRoles(resolveRoles(roleNames));
        return users.save(user);
    }

    @Transactional
    public void toggle(Long id, String currentUsername) {
        User user = users.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản"));
        if (user.getUsername().equalsIgnoreCase(currentUsername)) throw new BusinessRuleException("Không thể tự khóa tài khoản đang đăng nhập");
        user.setEnabled(!user.isEnabled());
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = getByUsername(username);
        if (!encoder.matches(currentPassword, user.getPasswordHash())) throw new BusinessRuleException("Mật khẩu hiện tại không đúng");
        user.setPasswordHash(encoder.encode(newPassword));
    }

    @Transactional
    public void ensureDemoUsers(String demoPassword) {
        if (demoPassword == null || demoPassword.isBlank()) return;
        Department department = departments.findByCodeIgnoreCase("CNPM").orElse(null);
        createIfMissing("admin", "Quản trị hệ thống", "admin@group9.local", null, department, RoleName.ADMIN, demoPassword);
        createIfMissing("faculty", "Cán bộ khoa", "faculty@group9.local", null, department, RoleName.FACULTY_MANAGER, demoPassword);
        createIfMissing("lecturer1", "Giảng viên mẫu", "lecturer1@group9.local", null, department, RoleName.LECTURER, demoPassword);
        createIfMissing("student1", "Sinh viên mẫu", "student1@group9.local", "SV0001", department, RoleName.STUDENT, demoPassword);
    }

    private void createIfMissing(String username, String fullName, String email, String studentCode, Department department, RoleName roleName, String password) {
        if (users.existsByUsernameIgnoreCase(username)) return;
        Role role = roles.findByName(roleName).orElseThrow(() -> new NotFoundException("Thiếu vai trò " + roleName));
        User user = new User(); user.setUsername(username); user.setFullName(fullName); user.setEmail(email); user.setStudentCode(studentCode);
        user.setDepartment(department); user.setPasswordHash(encoder.encode(password)); user.getRoles().add(role); users.save(user);
    }

    private Set<Role> resolveRoles(Set<RoleName> roleNames) {
        Set<Role> result = new LinkedHashSet<>();
        roleNames.forEach(name -> result.add(roles.findByName(name).orElseThrow(() -> new NotFoundException("Không tìm thấy vai trò " + name))));
        return result;
    }

    public List<RoleName> allRoleNames() { return Arrays.asList(RoleName.values()); }
}
