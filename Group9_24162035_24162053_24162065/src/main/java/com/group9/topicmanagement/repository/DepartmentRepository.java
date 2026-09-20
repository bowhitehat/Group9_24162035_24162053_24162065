package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByCodeIgnoreCase(String code);
    List<Department> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByCode(String code, String name);
    List<Department> findAllByOrderByCode();
}
