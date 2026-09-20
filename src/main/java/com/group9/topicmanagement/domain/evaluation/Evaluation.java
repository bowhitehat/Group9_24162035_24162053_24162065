package com.group9.topicmanagement.domain.evaluation;
import com.group9.topicmanagement.domain.BaseEntity;
import com.group9.topicmanagement.domain.User;
import com.group9.topicmanagement.domain.topic.Topic;
import com.group9.topicmanagement.domain.enums.EvaluationStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "evaluations")
public class Evaluation extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "evaluator_id", nullable = false)
    private User evaluator;
    @Column(nullable = false)
    private String evaluationType;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private EvaluationStatus status = EvaluationStatus.DRAFT;
    @Column(columnDefinition = "TEXT")
    private String comments;
    private LocalDateTime submissionTime;
    private LocalDateTime lockedTime;
    
    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EvaluationScore> scores;
    
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Topic getTopic() { return topic; } public void setTopic(Topic topic) { this.topic = topic; }
    public User getEvaluator() { return evaluator; } public void setEvaluator(User evaluator) { this.evaluator = evaluator; }
    public String getEvaluationType() { return evaluationType; } public void setEvaluationType(String evaluationType) { this.evaluationType = evaluationType; }
    public EvaluationStatus getStatus() { return status; } public void setStatus(EvaluationStatus status) { this.status = status; }
    public String getComments() { return comments; } public void setComments(String comments) { this.comments = comments; }
    public LocalDateTime getSubmissionTime() { return submissionTime; } public void setSubmissionTime(LocalDateTime submissionTime) { this.submissionTime = submissionTime; }
    public LocalDateTime getLockedTime() { return lockedTime; } public void setLockedTime(LocalDateTime lockedTime) { this.lockedTime = lockedTime; }
    public List<EvaluationScore> getScores() { return scores; } public void setScores(List<EvaluationScore> scores) { this.scores = scores; }
}