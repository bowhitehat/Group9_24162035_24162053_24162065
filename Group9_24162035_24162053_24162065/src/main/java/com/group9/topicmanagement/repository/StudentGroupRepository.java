package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.domain.studentgroup.StudentGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {
    
    Optional<StudentGroup> findByRegistrationPeriodIdAndLeaderId(Long periodId, Long leaderId);
    
    @Query("SELECT g FROM StudentGroup g JOIN g.members m WHERE m.member.id = :studentId AND g.registrationPeriod.id = :periodId")
    Optional<StudentGroup> findByStudentIdAndPeriodId(@Param("studentId") Long studentId, @Param("periodId") Long periodId);
}
