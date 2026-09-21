package com.group9.topicmanagement.domain.evaluation;

import com.group9.topicmanagement.domain.BaseEntity;
import com.group9.topicmanagement.domain.User;
import com.group9.topicmanagement.domain.council.CouncilAssignment;
import com.group9.topicmanagement.domain.enums.TopicResultStatus;
import com.group9.topicmanagement.domain.topic.Topic;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "topic_results")
public class TopicResult extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false, unique = true)
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "council_assignment_id", nullable = false)
    private CouncilAssignment councilAssignment;

    @Column(precision = 4, scale = 2)
    private BigDecimal finalScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TopicResultStatus status = TopicResultStatus.PENDING_CONFIRMATION;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmer_id")
    private User confirmer;

    private LocalDateTime confirmedTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id")
    private User publisher;

    private LocalDateTime publishedTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Topic getTopic() { return topic; }
    public void setTopic(Topic topic) { this.topic = topic; }
    public CouncilAssignment getCouncilAssignment() { return councilAssignment; }
    public void setCouncilAssignment(CouncilAssignment councilAssignment) { this.councilAssignment = councilAssignment; }
    public BigDecimal getFinalScore() { return finalScore; }
    public void setFinalScore(BigDecimal finalScore) { this.finalScore = finalScore; }
    public TopicResultStatus getStatus() { return status; }
    public void setStatus(TopicResultStatus status) { this.status = status; }
    public User getConfirmer() { return confirmer; }
    public void setConfirmer(User confirmer) { this.confirmer = confirmer; }
    public LocalDateTime getConfirmedTime() { return confirmedTime; }
    public void setConfirmedTime(LocalDateTime confirmedTime) { this.confirmedTime = confirmedTime; }
    public User getPublisher() { return publisher; }
    public void setPublisher(User publisher) { this.publisher = publisher; }
    public LocalDateTime getPublishedTime() { return publishedTime; }
    public void setPublishedTime(LocalDateTime publishedTime) { this.publishedTime = publishedTime; }
}
