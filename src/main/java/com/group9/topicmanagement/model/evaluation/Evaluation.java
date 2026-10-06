package com.group9.topicmanagement.model.evaluation;

import com.group9.topicmanagement.model.BaseEntity;
import com.group9.topicmanagement.model.User;
import com.group9.topicmanagement.model.council.CouncilMember;
import com.group9.topicmanagement.model.enums.EvaluationStatus;
import com.group9.topicmanagement.model.enums.EvaluationType;
import com.group9.topicmanagement.model.topic.Topic;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evaluations", uniqueConstraints = @UniqueConstraint(
        name = "uk_evaluation_topic_evaluator_type",
        columnNames = {"topic_id", "evaluator_id", "evaluation_type"}))
public class Evaluation extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false, foreignKey = @ForeignKey(name = "fk_evaluations_topic_id"))
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluator_id", nullable = false, foreignKey = @ForeignKey(name = "fk_evaluations_evaluator_id"))
    private User evaluator;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_type", nullable = false, length = 30)
    private EvaluationType evaluationType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_assignment_id", foreignKey = @ForeignKey(name = "fk_evaluations_reviewer_assignment_id"))
    private ReviewerAssignment reviewerAssignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "council_member_id", foreignKey = @ForeignKey(name = "fk_evaluations_council_member_id"))
    private CouncilMember councilMember;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EvaluationStatus status = EvaluationStatus.DRAFT;

    @Lob
    private String comments;

    private LocalDateTime submissionTime;
    private LocalDateTime lockedTime;

    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EvaluationScore> scores = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Topic getTopic() { return topic; }
    public void setTopic(Topic topic) { this.topic = topic; }
    public User getEvaluator() { return evaluator; }
    public void setEvaluator(User evaluator) { this.evaluator = evaluator; }
    public EvaluationType getEvaluationType() { return evaluationType; }
    public void setEvaluationType(EvaluationType evaluationType) { this.evaluationType = evaluationType; }
    public ReviewerAssignment getReviewerAssignment() { return reviewerAssignment; }
    public void setReviewerAssignment(ReviewerAssignment reviewerAssignment) { this.reviewerAssignment = reviewerAssignment; }
    public CouncilMember getCouncilMember() { return councilMember; }
    public void setCouncilMember(CouncilMember councilMember) { this.councilMember = councilMember; }
    public EvaluationStatus getStatus() { return status; }
    public void setStatus(EvaluationStatus status) { this.status = status; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    public LocalDateTime getSubmissionTime() { return submissionTime; }
    public void setSubmissionTime(LocalDateTime submissionTime) { this.submissionTime = submissionTime; }
    public LocalDateTime getLockedTime() { return lockedTime; }
    public void setLockedTime(LocalDateTime lockedTime) { this.lockedTime = lockedTime; }
    public List<EvaluationScore> getScores() { return scores; }
    public void setScores(List<EvaluationScore> scores) { this.scores = scores; }
}
