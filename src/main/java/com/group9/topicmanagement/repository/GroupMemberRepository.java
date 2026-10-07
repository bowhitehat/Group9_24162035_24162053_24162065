package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.model.studentgroup.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    
    boolean existsByMember_IdAndGroup_RegistrationPeriod_Id(Long studentId, Long periodId);

}
