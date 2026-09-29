package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.domain.registration.ReportSubmission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReportSubmissionRepository extends JpaRepository<ReportSubmission, Long> {

    @Override
    @EntityGraph(attributePaths = {
            "submitter", "topicRegistration", "topicRegistration.topic",
            "topicRegistration.studentGroup", "topicRegistration.studentGroup.leader",
            "topicRegistration.studentGroup.members", "topicRegistration.studentGroup.members.member"
    })
    Optional<ReportSubmission> findById(Long id);
    
    @EntityGraph(attributePaths = {"submitter", "topicRegistration"})
    List<ReportSubmission> findByTopicRegistration_IdOrderByVersionDesc(Long registrationId);
    
    @EntityGraph(attributePaths = {"submitter", "topicRegistration"})
    Optional<ReportSubmission> findFirstByTopicRegistration_IdOrderByVersionDesc(Long registrationId);
}
