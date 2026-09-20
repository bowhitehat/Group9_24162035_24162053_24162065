package com.group9.topicmanagement.domain.council;
import com.group9.topicmanagement.domain.BaseEntity;
import com.group9.topicmanagement.domain.topic.Topic;
import jakarta.persistence.*;

@Entity
@Table(name = "council_assignments")
public class CouncilAssignment extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "council_id", nullable = false)
    private Council council;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;
    
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Council getCouncil() { return council; } public void setCouncil(Council council) { this.council = council; }
    public Topic getTopic() { return topic; } public void setTopic(Topic topic) { this.topic = topic; }
}