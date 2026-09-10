package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = {"roles", "department"})
    Optional<User> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByEmailIgnoreCase(String email);
    @EntityGraph(attributePaths = {"roles", "department"})
    List<User> findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrderByFullName(String username, String fullName);
    @EntityGraph(attributePaths = {"roles", "department"})
    List<User> findAllByOrderByFullName();
    long countByDepartmentId(Long departmentId);
}
