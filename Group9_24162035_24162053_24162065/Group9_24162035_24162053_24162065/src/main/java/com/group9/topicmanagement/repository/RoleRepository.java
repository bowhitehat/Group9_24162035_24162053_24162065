package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.domain.Role;
import com.group9.topicmanagement.domain.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
