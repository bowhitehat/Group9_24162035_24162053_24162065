package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.model.RegistrationPeriod;
import com.group9.topicmanagement.model.enums.PeriodStatus;
import com.group9.topicmanagement.model.enums.PeriodType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RegistrationPeriodRepository extends JpaRepository<RegistrationPeriod, Long> {
    List<RegistrationPeriod> findAllByOrderByCreatedAtDesc();
    List<RegistrationPeriod> findByNameContainingIgnoreCaseAndTypeAndStatusOrderByCreatedAtDesc(String name, PeriodType type, PeriodStatus status);
    List<RegistrationPeriod> findByNameContainingIgnoreCaseAndTypeOrderByCreatedAtDesc(String name, PeriodType type);
    List<RegistrationPeriod> findByNameContainingIgnoreCaseAndStatusOrderByCreatedAtDesc(String name, PeriodStatus status);
    List<RegistrationPeriod> findByNameContainingIgnoreCaseOrderByCreatedAtDesc(String name);

    @Query("SELECT COUNT(p) FROM RegistrationPeriod p WHERE p.status NOT IN :closedStatuses")
    long countByStatusNotIn(@Param("closedStatuses") java.util.Collection<PeriodStatus> closedStatuses);
}
