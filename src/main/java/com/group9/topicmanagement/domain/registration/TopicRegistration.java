package com.group9.topicmanagement.domain.registration;

import com.group9.topicmanagement.domain.BaseEntity;
import com.group9.topicmanagement.domain.studentgroup.StudentGroup;
import com.group9.topicmanagement.domain.topic.Topic;
import com.group9.topicmanagement.domain.RegistrationPeriod;
import com.group9.topicmanagement.domain.User;
import jakarta.persistence.*;
import com.group9.topicmanagement.domain.enums.RegistrationStatus;
import java.time.LocalDateTime;

@Entity
@Table(name = "topic_registration",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"student_group_id", "registration_period_id"}),
           @UniqueConstraint(columnNames = {"topic_id", "registration_period_id"})
       },
       indexes = {@Index(name = "idx_reg_status", columnList = "status")})
public class TopicRegistration extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_group_id", nullable = false)
    private StudentGroup studentGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_period_id", nullable = false)
    private RegistrationPeriod registrationPeriod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegistrationStatus status = RegistrationStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private User approver;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public StudentGroup getStudentGroup() { return studentGroup; }
    public void setStudentGroup(StudentGroup studentGroup) { this.studentGroup = studentGroup; }
    public Topic getTopic() { return topic; }
    public void setTopic(Topic topic) { this.topic = topic; }
    public RegistrationPeriod getRegistrationPeriod() { return registrationPeriod; }
    public void setRegistrationPeriod(RegistrationPeriod registrationPeriod) { this.registrationPeriod = registrationPeriod; }
    public RegistrationStatus getStatus() { return status; }
    public void setStatus(RegistrationStatus status) { this.status = status; }
    public User getApprover() { return approver; }
    public void setApprover(User approver) { this.approver = approver; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
}
