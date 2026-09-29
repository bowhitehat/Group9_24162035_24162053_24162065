package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.domain.studentgroup.StudentGroup;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {

    @Override
    @EntityGraph(attributePaths = {"registrationPeriod", "leader", "members", "members.member"})
    Optional<StudentGroup> findById(Long id);
    
    @EntityGraph(attributePaths = {"registrationPeriod", "leader", "members", "members.member"})
    Optional<StudentGroup> findByRegistrationPeriodIdAndLeaderId(Long periodId, Long leaderId);
    
    @EntityGraph(attributePaths = {"registrationPeriod", "leader", "members", "members.member"})
    Optional<StudentGroup> findDistinctByMembers_Member_IdAndRegistrationPeriod_Id(Long studentId, Long periodId);
}
