package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.domain.studentgroup.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    
    @Query("SELECT COUNT(m) > 0 FROM GroupMember m WHERE m.member.id = :studentId AND m.group.registrationPeriod.id = :periodId")
    boolean existsByStudentIdAndPeriodId(@Param("studentId") Long studentId, @Param("periodId") Long periodId);
}
