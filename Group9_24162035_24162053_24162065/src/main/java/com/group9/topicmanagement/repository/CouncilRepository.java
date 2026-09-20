package com.group9.topicmanagement.repository;
import com.group9.topicmanagement.domain.council.Council;
import com.group9.topicmanagement.domain.enums.CouncilStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CouncilRepository extends JpaRepository<Council, Long> {
    List<Council> findByRegistrationPeriodId(Long periodId);
}