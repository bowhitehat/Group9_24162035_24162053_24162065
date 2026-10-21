package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.model.studentgroup.StudentGroup;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<StudentGroup> findLockedById(Long id);

    @Override
    @EntityGraph(attributePaths = {"registrationPeriod", "leader", "members", "members.member"})
    Optional<StudentGroup> findById(Long id);
    
    @EntityGraph(attributePaths = {"registrationPeriod", "leader", "members", "members.member"})
    Optional<StudentGroup> findByRegistrationPeriodIdAndLeaderId(Long periodId, Long leaderId);
    
    @EntityGraph(attributePaths = {"registrationPeriod", "leader", "members", "members.member"})
    Optional<StudentGroup> findDistinctByMembers_Member_IdAndRegistrationPeriod_Id(Long studentId, Long periodId);

    @EntityGraph(attributePaths = {"registrationPeriod", "leader", "members", "members.member"})
    @Query("SELECT g FROM StudentGroup g JOIN g.members m WHERE m.member.id = :memberId ORDER BY g.id DESC")
    List<StudentGroup> findByMemberId(@Param("memberId") Long memberId);
}
