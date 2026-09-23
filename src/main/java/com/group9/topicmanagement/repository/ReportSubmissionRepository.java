package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.domain.registration.ReportSubmission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    
    @EntityGraph(attributePaths = {"submitter"})
    @Query("SELECT r FROM ReportSubmission r WHERE r.topicRegistration.id = :registrationId ORDER BY r.version DESC")
    List<ReportSubmission> findByRegistrationIdOrderByVersionDesc(@Param("registrationId") Long registrationId);
    
    @Query("SELECT r FROM ReportSubmission r WHERE r.topicRegistration.id = :registrationId ORDER BY r.version DESC LIMIT 1")
    Optional<ReportSubmission> findLatestByRegistrationId(@Param("registrationId") Long registrationId);
}
