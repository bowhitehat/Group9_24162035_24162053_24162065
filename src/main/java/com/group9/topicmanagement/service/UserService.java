package com.group9.topicmanagement.service;

import com.group9.topicmanagement.model.Department;
import com.group9.topicmanagement.model.Role;
import com.group9.topicmanagement.model.User;
import com.group9.topicmanagement.model.enums.RoleName;
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

    public long countUsers() { return users.count(); }

    public User get(Long id) {
        return users.findOneById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản"));
    }

    public User getByUsername(String username) {
        return users.findByUsernameIgnoreCase(username).orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản"));
    }

    public java.util.Optional<User> findByUsername(String username) {
        return users.findByUsernameIgnoreCase(username);
    }

    public List<User> findUsersByRole(String roleName) {
        return users.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName().name().equalsIgnoreCase(roleName) || r.getName().toString().equalsIgnoreCase(roleName)))
                .toList();
    }

    @Transactional
    public User create(String username, String fullName, String email, String studentCode, Long departmentId, Set<RoleName> roleNames, String rawPassword) {
        validateIdentity(null, username, fullName, email, studentCode, roleNames);
        if (rawPassword == null || rawPassword.isBlank() || rawPassword.length() < 8
                || rawPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new BusinessRuleException("Mật khẩu phải có ít nhất 8 ký tự và không vượt quá 72 byte");
        User user = new User();
        user.setUsername(username.trim());
        user.setFullName(fullName.trim());
        user.setEmail(email.trim().toLowerCase(java.util.Locale.ROOT));
        user.setStudentCode(studentCode == null || studentCode.isBlank() ? null : studentCode.trim());
        user.setPasswordHash(encoder.encode(rawPassword));
        if (departmentId != null) user.setDepartment(departments.findById(departmentId).orElseThrow(() -> new NotFoundException("Không tìm thấy bộ môn")));
        user.setRoles(resolveRoles(roleNames));
        return users.save(user);
    }

    @Transactional
    public User update(Long id, String username, String fullName, String email, String studentCode,
                       Long departmentId, Set<RoleName> roleNames, String actor) {
        User user = get(id);
        validateIdentity(id, username, fullName, email, studentCode, roleNames);
        boolean self = user.getUsername().equalsIgnoreCase(actor);
        if (self && (!user.getUsername().equals(username.trim()) || !roleNames.contains(RoleName.ADMIN)))
            throw new BusinessRuleException("Không thể đổi tên đăng nhập hoặc bỏ quyền ADMIN của chính mình");
        // Identity is referenced by authentication; relationships remain attached to the same user ID.
        user.setUsername(username.trim()); user.setFullName(fullName.trim());
        user.setEmail(email.trim().toLowerCase(java.util.Locale.ROOT));
        user.setStudentCode(studentCode == null || studentCode.isBlank() ? null : studentCode.trim());
        user.setDepartment(departmentId == null ? null : departments.findById(departmentId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ môn")));
        user.setRoles(resolveRoles(roleNames));
        return user;
    }

    private void validateIdentity(Long id, String username, String fullName, String email,
                                  String studentCode, Set<RoleName> roleNames) {
        if (username == null || username.isBlank() || username.trim().length() > 80)
            throw new BusinessRuleException("Tên đăng nhập là bắt buộc và tối đa 80 ký tự");
        if (fullName == null || fullName.isBlank() || fullName.trim().length() > 160)
            throw new BusinessRuleException("Họ tên là bắt buộc và tối đa 160 ký tự");
        if (email == null || email.trim().length() > 160
                || !email.trim().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new BusinessRuleException("Email không hợp lệ");
        String code = studentCode == null || studentCode.isBlank() ? null : studentCode.trim();
        if (code != null && code.length() > 30) throw new BusinessRuleException("MSSV tối đa 30 ký tự");
        if (id == null ? users.existsByUsernameIgnoreCase(username.trim())
                : users.existsByUsernameIgnoreCaseAndIdNot(username.trim(), id))
            throw new BusinessRuleException("Tên đăng nhập đã tồn tại");
        if (id == null ? users.existsByEmailIgnoreCase(email.trim())
                : users.existsByEmailIgnoreCaseAndIdNot(email.trim(), id))
            throw new BusinessRuleException("Email đã tồn tại");
        if (code != null && (id == null ? users.existsByStudentCodeIgnoreCase(code)
                : users.existsByStudentCodeIgnoreCaseAndIdNot(code, id)))
            throw new BusinessRuleException("MSSV đã tồn tại");
        if (roleNames == null || roleNames.isEmpty() || roleNames.stream().anyMatch(java.util.Objects::isNull))
            throw new BusinessRuleException("Tài khoản phải có ít nhất một vai trò hợp lệ");
    }

    @Transactional
    public void toggle(Long id, String currentUsername) {
        User user = users.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản"));
        if (user.getUsername().equalsIgnoreCase(currentUsername)) throw new BusinessRuleException("Không thể tự khóa tài khoản đang đăng nhập");
        user.setEnabled(!user.isEnabled());
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        if (newPassword == null || newPassword.isBlank() || newPassword.length() < 8
                || newPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new BusinessRuleException("Mật khẩu mới phải có ít nhất 8 ký tự và không vượt quá 72 byte");
        }
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
