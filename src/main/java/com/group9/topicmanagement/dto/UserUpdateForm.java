package com.group9.topicmanagement.dto;

import com.group9.topicmanagement.model.User;
import com.group9.topicmanagement.model.enums.RoleName;
import jakarta.validation.constraints.*;
import java.util.LinkedHashSet;
import java.util.Set;

/** Editing profile/roles does not accept or change a password hash. */
public class UserUpdateForm {
    @NotBlank(message="Tên đăng nhập là bắt buộc") @Size(max=80)
    private String username;
    @NotBlank(message="Họ tên là bắt buộc") @Size(max=160)
    private String fullName;
    @NotBlank(message="Email là bắt buộc") @Email(message="Email không hợp lệ") @Size(max=160)
    private String email;
    @Size(max=30) private String studentCode;
    private Long departmentId;
    @NotEmpty(message="Chọn ít nhất một vai trò")
    private Set<RoleName> roles = new LinkedHashSet<>();

    public static UserUpdateForm from(User user) {
        UserUpdateForm form = new UserUpdateForm();
        form.username=user.getUsername(); form.fullName=user.getFullName(); form.email=user.getEmail();
        form.studentCode=user.getStudentCode();
        form.departmentId=user.getDepartment()==null ? null : user.getDepartment().getId();
        user.getRoles().forEach(role -> form.roles.add(role.getName()));
        return form;
    }
    public String getUsername(){return username;} public void setUsername(String v){username=v;}
    public String getFullName(){return fullName;} public void setFullName(String v){fullName=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getStudentCode(){return studentCode;} public void setStudentCode(String v){studentCode=v;}
    public Long getDepartmentId(){return departmentId;} public void setDepartmentId(Long v){departmentId=v;}
    public Set<RoleName> getRoles(){return roles;} public void setRoles(Set<RoleName> v){roles=v;}
}
