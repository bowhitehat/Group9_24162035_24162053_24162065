package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.model.User;
import com.group9.topicmanagement.model.enums.RoleName;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findLockedById(Long id);

    @EntityGraph(attributePaths = {"roles", "department"})
    Optional<User> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    boolean existsByStudentCodeIgnoreCase(String studentCode);
    boolean existsByStudentCodeIgnoreCaseAndIdNot(String studentCode, Long id);
    @EntityGraph(attributePaths = {"roles", "department"})
    Optional<User> findOneById(Long id);
    @EntityGraph(attributePaths = {"roles", "department"})
    List<User> findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrderByFullName(String username, String fullName);
    @EntityGraph(attributePaths = {"roles", "department"})
    List<User> findAllByOrderByFullName();
    long countByDepartmentId(Long departmentId);

    @EntityGraph(attributePaths = {"roles", "department"})
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName ORDER BY u.fullName ASC")
    List<User> findByRoleName(@Param("roleName") RoleName roleName);
}
