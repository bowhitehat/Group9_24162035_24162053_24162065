package com.group9.topicmanagement.service;

import com.group9.topicmanagement.domain.Department;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.exception.NotFoundException;
import com.group9.topicmanagement.repository.DepartmentRepository;
import com.group9.topicmanagement.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DepartmentService {
    private final DepartmentRepository departments;
    private final UserRepository users;

    public DepartmentService(DepartmentRepository departments, UserRepository users) {
        this.departments = departments;
        this.users = users;
    }

    public List<Department> search(String keyword) {
        String value = keyword == null ? "" : keyword.trim();
        return value.isBlank() ? departments.findAllByOrderByCode()
            : departments.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByCode(value, value);
    }

    public Department get(Long id) {
        return departments.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy bộ môn"));
    }

    @Transactional
    public Department create(String code, String name, String description) {
        String normalized = code.trim().toUpperCase();
        if (departments.findByCodeIgnoreCase(normalized).isPresent()) throw new BusinessRuleException("Mã bộ môn đã tồn tại");
        Department department = new Department();
        department.setCode(normalized);
        department.setName(name.trim());
        department.setDescription(description == null ? null : description.trim());
        return departments.save(department);
    }

    @Transactional
    public Department update(Long id, String code, String name, String description) {
        Department department = get(id);
        String normalized = code.trim().toUpperCase();
        departments.findByCodeIgnoreCase(normalized).filter(other -> !other.getId().equals(id))
            .ifPresent(other -> { throw new BusinessRuleException("Mã bộ môn đã tồn tại"); });
        department.setCode(normalized);
        department.setName(name.trim());
        department.setDescription(description == null ? null : description.trim());
        return department;
    }

    @Transactional
    public void toggle(Long id) {
        Department department = get(id);
        if (department.isActive() && users.countByDepartmentId(id) > 0) {
            throw new BusinessRuleException("Không thể ngừng bộ môn đang có người dùng liên quan");
        }
        department.setActive(!department.isActive());
    }
}
