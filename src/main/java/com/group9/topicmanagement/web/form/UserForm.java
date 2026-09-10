package com.group9.topicmanagement.web.form;

import com.group9.topicmanagement.domain.enums.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashSet;
import java.util.Set;

public class UserForm {
    @NotBlank(message = "Tên đăng nhập là bắt buộc") @Size(max = 80) private String username;
    @NotBlank(message = "Họ tên là bắt buộc") @Size(max = 160) private String fullName;
    @NotBlank(message = "Email là bắt buộc") @Email(message = "Email không hợp lệ") private String email;
    @Size(max = 30) private String studentCode;
    private Long departmentId;
    @NotEmpty(message = "Chọn ít nhất một vai trò") private Set<RoleName> roles = new LinkedHashSet<>();
    @NotBlank(message = "Mật khẩu là bắt buộc") @Size(min = 8, message = "Mật khẩu tối thiểu 8 ký tự") private String password;
    public String getUsername() { return username; } public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; } public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; } public void setEmail(String email) { this.email = email; }
    public String getStudentCode() { return studentCode; } public void setStudentCode(String studentCode) { this.studentCode = studentCode; }
    public Long getDepartmentId() { return departmentId; } public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public Set<RoleName> getRoles() { return roles; } public void setRoles(Set<RoleName> roles) { this.roles = roles; }
    public String getPassword() { return password; } public void setPassword(String password) { this.password = password; }
}
