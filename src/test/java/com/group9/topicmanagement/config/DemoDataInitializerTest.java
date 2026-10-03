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
        when(departments.findByCodeIgnoreCase("MMT")).thenReturn(Optional.of(legacy));
        new DemoDataInitializer(mock(UserService.class), mock(RoleRepository.class), departments, "").run();
        assertThat(legacy.getCode()).isEqualTo("ATTT");
        assertThat(legacy.getName()).isEqualTo("An toàn thông tin");
        verify(departments).save(legacy);
    }

    @Test void existingCorrectCodeDoesNotCreateAnotherDepartment() {
        DepartmentRepository departments = mock(DepartmentRepository.class);
        Department current = new Department();
        current.setCode("ATTT");
        when(departments.findByCodeIgnoreCase("ATTT")).thenReturn(Optional.of(current));
        new DemoDataInitializer(mock(UserService.class), mock(RoleRepository.class), departments, "").run();
        verify(departments).save(current);
        verify(departments, never()).findByCodeIgnoreCase("MMT");
    }

    @Test void freshDatabaseSeedsCorrectCode() {
        DepartmentRepository departments = mock(DepartmentRepository.class);
        new DemoDataInitializer(mock(UserService.class), mock(RoleRepository.class), departments, "").run();
        verify(departments).save(argThat(d -> "ATTT".equals(d.getCode())
                && "An toàn thông tin".equals(d.getName())));
    }
}
