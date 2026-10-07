package com.group9.topicmanagement.config;

import com.group9.topicmanagement.model.Department;
import com.group9.topicmanagement.repository.DepartmentRepository;
import com.group9.topicmanagement.repository.RoleRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:group9_initializer;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "app.demo-password=",
        "app.mail.enabled=false"
})
@ActiveProfiles("test")
@Transactional
class DemoDataInitializerIntegrationTest {
    @Autowired DemoDataInitializer initializer;
    @Autowired DepartmentRepository departments;
    @Autowired RoleRepository roles;
    @Autowired EntityManager entityManager;

    @Test void repeatedStartupPreservesAdminEditsAndDoesNotDuplicateSeedData() {
        Map<String, Long> ids = departments.findAll().stream().collect(
                java.util.stream.Collectors.toMap(Department::getCode, Department::getId));
        long departmentCount = departments.count();
        long roleCount = roles.count();
        for (String code : new String[]{"CNPM", "HTTT", "ATTT"}) {
            Department department = departments.findByCodeIgnoreCase(code).orElseThrow();
            department.setName("Tên đã chỉnh " + code);
            department.setDescription("Mô tả đã chỉnh " + code);
            department.setActive(false);
        }
        entityManager.flush();
        entityManager.clear();

        initializer.run();
        initializer.run();
        entityManager.flush();
        entityManager.clear();

        for (String code : ids.keySet()) {
            Department department = departments.findByCodeIgnoreCase(code).orElseThrow();
            assertThat(department.getId()).isEqualTo(ids.get(code));
            assertThat(department.getName()).isEqualTo("Tên đã chỉnh " + code);
            assertThat(department.getDescription()).isEqualTo("Mô tả đã chỉnh " + code);
            assertThat(department.isActive()).isFalse();
        }
        assertThat(departments.count()).isEqualTo(departmentCount);
        assertThat(roles.count()).isEqualTo(roleCount);
    }

    @Test void legacyCodeMigrationKeepsDatabaseIdentityAndSubsequentEdits() {
        Department legacy = departments.findByCodeIgnoreCase("ATTT").orElseThrow();
        Long originalId = legacy.getId();
        long originalCount = departments.count();
        legacy.setCode("MMT");
        legacy.setName("Mạng máy tính");
        legacy.setDescription("Phụ trách mạng máy tính và an toàn thông tin.");
        legacy.setActive(false);
        entityManager.flush();
        entityManager.clear();

        initializer.run();
        entityManager.flush();
        entityManager.clear();

        Department renamed = departments.findByCodeIgnoreCase("ATTT").orElseThrow();
        assertThat(renamed.getId()).isEqualTo(originalId);
        assertThat(renamed.getName()).isEqualTo("An toàn thông tin");
        assertThat(renamed.isActive()).isFalse();
        renamed.setName("Tên được sửa sau chuyển mã");
        entityManager.flush();
        entityManager.clear();
        initializer.run();
        entityManager.flush();
        entityManager.clear();

        assertThat(departments.findByCodeIgnoreCase("ATTT").orElseThrow().getName())
                .isEqualTo("Tên được sửa sau chuyển mã");
        assertThat(departments.findByCodeIgnoreCase("MMT")).isEmpty();
        assertThat(departments.count()).isEqualTo(originalCount);
    }
}
