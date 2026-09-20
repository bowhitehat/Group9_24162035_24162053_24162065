package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.domain.RegistrationPeriod;
import com.group9.topicmanagement.domain.enums.PeriodStatus;
import com.group9.topicmanagement.domain.enums.PeriodType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RegistrationPeriodRepository extends JpaRepository<RegistrationPeriod, Long> {
    List<RegistrationPeriod> findAllByOrderByCreatedAtDesc();
    List<RegistrationPeriod> findByNameContainingIgnoreCaseAndTypeAndStatusOrderByCreatedAtDesc(String name, PeriodType type, PeriodStatus status);
    List<RegistrationPeriod> findByNameContainingIgnoreCaseAndTypeOrderByCreatedAtDesc(String name, PeriodType type);
    List<RegistrationPeriod> findByNameContainingIgnoreCaseAndStatusOrderByCreatedAtDesc(String name, PeriodStatus status);
    List<RegistrationPeriod> findByNameContainingIgnoreCaseOrderByCreatedAtDesc(String name);
}
