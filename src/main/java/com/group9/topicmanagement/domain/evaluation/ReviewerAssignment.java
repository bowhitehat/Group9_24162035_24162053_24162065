package com.group9.topicmanagement.domain.evaluation;
import com.group9.topicmanagement.domain.BaseEntity;
import com.group9.topicmanagement.domain.User;
import com.group9.topicmanagement.domain.topic.Topic;
import com.group9.topicmanagement.domain.enums.ReviewerAssignmentStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviewer_assignments")
public class ReviewerAssignment extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assigner_id")
    private User assigner;
    private LocalDateTime assignedAt;
    private LocalDateTime deadline;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ReviewerAssignmentStatus status = ReviewerAssignmentStatus.ASSIGNED;
    private LocalDateTime submissionTime;
    @Column(length = 500)
    private String note;
    
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Topic getTopic() { return topic; } public void setTopic(Topic topic) { this.topic = topic; }
    public User getReviewer() { return reviewer; } public void setReviewer(User reviewer) { this.reviewer = reviewer; }
    public User getAssigner() { return assigner; } public void setAssigner(User assigner) { this.assigner = assigner; }
    public LocalDateTime getAssignedAt() { return assignedAt; } public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
    public LocalDateTime getDeadline() { return deadline; } public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    public ReviewerAssignmentStatus getStatus() { return status; } public void setStatus(ReviewerAssignmentStatus status) { this.status = status; }
    public LocalDateTime getSubmissionTime() { return submissionTime; } public void setSubmissionTime(LocalDateTime submissionTime) { this.submissionTime = submissionTime; }
    public String getNote() { return note; } public void setNote(String note) { this.note = note; }
}