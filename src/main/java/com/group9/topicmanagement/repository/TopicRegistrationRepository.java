package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.model.enums.RegistrationStatus;
import com.group9.topicmanagement.model.registration.TopicRegistration;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TopicRegistrationRepository extends JpaRepository<TopicRegistration, Long> {

    @Override
    @EntityGraph(attributePaths = {
            "topic", "topic.department", "topic.proposer", "topic.advisors",
            "studentGroup", "studentGroup.leader", "studentGroup.members", "studentGroup.members.member",
            "registrationPeriod", "approver"
    })
    Optional<TopicRegistration> findById(Long id);
    
    @EntityGraph(attributePaths = {"topic", "studentGroup", "studentGroup.leader", "registrationPeriod"})
    Optional<TopicRegistration> findByStudentGroupIdAndRegistrationPeriodId(Long groupId, Long periodId);
    
    @EntityGraph(attributePaths = {"topic", "studentGroup", "studentGroup.members", "studentGroup.members.member"})
    Optional<TopicRegistration> findFirstByTopic_IdAndStatus(Long topicId, RegistrationStatus status);

    Optional<TopicRegistration> findFirstByTopic_IdAndRegistrationPeriod_IdAndStatus(Long topicId, Long periodId,
                                                                                      RegistrationStatus status);

    @EntityGraph(attributePaths = {
            "topic", "topic.department", "studentGroup", "studentGroup.leader",
            "studentGroup.members", "studentGroup.members.member", "registrationPeriod"
    })
    List<TopicRegistration> findAllByOrderByIdDesc();

    @EntityGraph(attributePaths = {"topic", "studentGroup", "studentGroup.members", "studentGroup.members.member"})
    List<TopicRegistration> findByStatusOrderByIdAsc(RegistrationStatus status);

    long countByStatus(RegistrationStatus status);

    @EntityGraph(attributePaths = {"topic", "studentGroup", "studentGroup.leader", "registrationPeriod"})
    List<TopicRegistration> findByStudentGroup_Leader_UsernameIgnoreCaseAndStatusOrderByIdDesc(
            String username, RegistrationStatus status);
}
