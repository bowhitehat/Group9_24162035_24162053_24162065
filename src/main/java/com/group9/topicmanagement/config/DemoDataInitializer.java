package com.group9.topicmanagement.config;

import com.group9.topicmanagement.domain.Department;
import com.group9.topicmanagement.domain.Role;
import com.group9.topicmanagement.domain.enums.RoleName;
import com.group9.topicmanagement.repository.DepartmentRepository;
import com.group9.topicmanagement.repository.RoleRepository;
import com.group9.topicmanagement.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoDataInitializer implements CommandLineRunner {
    private final UserService users;
    private final RoleRepository roles;
    private final DepartmentRepository departments;
    private final String demoPassword;

    public DemoDataInitializer(UserService users, RoleRepository roles, DepartmentRepository departments,
                               @Value("${app.demo-password:}") String demoPassword) {
        this.users = users;
        this.roles = roles;
        this.departments = departments;
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        for (RoleName name : RoleName.values()) {
            roles.findByName(name).orElseGet(() -> {
                Role role = new Role();
                role.setName(name);
                return roles.save(role);
            });
        }
        ensureDepartment("CNPM", "Công nghệ phần mềm",
                "Phụ trách đào tạo và nghiên cứu công nghệ phần mềm.");
        ensureDepartment("HTTT", "Hệ thống thông tin",
                "Phụ trách hệ thống thông tin và dữ liệu.");
        ensureDepartment("MMT", "An toàn thông tin",
                "Phụ trách đào tạo và nghiên cứu an toàn thông tin.");
        users.ensureDemoUsers(demoPassword);
    }

    private void ensureDepartment(String code, String name, String description) {
        Department department = departments.findByCodeIgnoreCase(code).orElseGet(Department::new);
        department.setCode(code);
        department.setName(name);
        department.setDescription(description);
        department.setActive(true);
        departments.save(department);
    }
}
