package com.group9.topicmanagement.config;

import com.group9.topicmanagement.model.Department;
import com.group9.topicmanagement.repository.DepartmentRepository;
import com.group9.topicmanagement.repository.RoleRepository;
import com.group9.topicmanagement.service.UserService;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DemoDataInitializerTest {
    @Test void renamesLegacyDepartmentWithoutChangingItsIdentity() {
        DepartmentRepository departments = mock(DepartmentRepository.class);
        Department legacy = new Department();
        legacy.setCode("MMT");
        legacy.setName("Mạng máy tính");
        legacy.setDescription("Phụ trách mạng máy tính và an toàn thông tin.");
        legacy.setActive(false);
        when(departments.findByCodeIgnoreCase("MMT")).thenReturn(Optional.of(legacy));
        new DemoDataInitializer(mock(UserService.class), mock(RoleRepository.class), departments, "").run();
        assertThat(legacy.getCode()).isEqualTo("ATTT");
        assertThat(legacy.getName()).isEqualTo("An toàn thông tin");
        assertThat(legacy.getDescription()).isEqualTo("Phụ trách đào tạo và nghiên cứu an toàn thông tin.");
        assertThat(legacy.isActive()).isFalse();
        verify(departments).save(legacy);
    }

    @Test void existingCorrectCodeDoesNotCreateAnotherDepartment() {
        DepartmentRepository departments = mock(DepartmentRepository.class);
        Department current = new Department();
        current.setCode("ATTT");
        current.setName("Tên bộ môn đã chỉnh");
        current.setDescription("Mô tả do quản trị viên cập nhật");
        current.setActive(false);
        when(departments.findByCodeIgnoreCase("ATTT")).thenReturn(Optional.of(current));
        new DemoDataInitializer(mock(UserService.class), mock(RoleRepository.class), departments, "").run();
        assertThat(current.getName()).isEqualTo("Tên bộ môn đã chỉnh");
        assertThat(current.getDescription()).isEqualTo("Mô tả do quản trị viên cập nhật");
        assertThat(current.isActive()).isFalse();
        verify(departments, never()).save(current);
        verify(departments, never()).findByCodeIgnoreCase("MMT");
    }

    @Test void legacyRenamePreservesCustomFieldsAndInactiveState() {
        DepartmentRepository departments = mock(DepartmentRepository.class);
        Department legacy = new Department();
        legacy.setCode("MMT");
        legacy.setName("Bộ môn do quản trị viên đặt tên");
        legacy.setDescription("Mô tả riêng cần giữ lại");
        legacy.setActive(false);
        when(departments.findByCodeIgnoreCase("MMT")).thenReturn(Optional.of(legacy));

        new DemoDataInitializer(mock(UserService.class), mock(RoleRepository.class), departments, "").run();

        assertThat(legacy.getCode()).isEqualTo("ATTT");
        assertThat(legacy.getName()).isEqualTo("Bộ môn do quản trị viên đặt tên");
        assertThat(legacy.getDescription()).isEqualTo("Mô tả riêng cần giữ lại");
        assertThat(legacy.isActive()).isFalse();
        verify(departments).save(legacy);
    }

    @Test void freshDatabaseSeedsCorrectCode() {
        DepartmentRepository departments = mock(DepartmentRepository.class);
        new DemoDataInitializer(mock(UserService.class), mock(RoleRepository.class), departments, "").run();
        verify(departments).save(argThat(d -> "ATTT".equals(d.getCode())
                && "An toàn thông tin".equals(d.getName()) && d.isActive()));
    }
}
