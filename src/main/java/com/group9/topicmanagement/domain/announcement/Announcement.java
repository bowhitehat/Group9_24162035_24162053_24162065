package com.group9.topicmanagement.domain.announcement;
import com.group9.topicmanagement.domain.BaseEntity;
import com.group9.topicmanagement.domain.User;
import com.group9.topicmanagement.domain.enums.AnnouncementStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "announcements")
public class Announcement extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String title;
    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String content;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "creator_id", nullable = false)
    private User creator;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private AnnouncementStatus status = AnnouncementStatus.DRAFT;
    private String targetRoles;
    private LocalDateTime publishedTime;
    private LocalDateTime expirationTime;
    
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; } public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; } public void setContent(String content) { this.content = content; }
    public User getCreator() { return creator; } public void setCreator(User creator) { this.creator = creator; }
    public AnnouncementStatus getStatus() { return status; } public void setStatus(AnnouncementStatus status) { this.status = status; }
    public String getTargetRoles() { return targetRoles; } public void setTargetRoles(String targetRoles) { this.targetRoles = targetRoles; }
    public LocalDateTime getPublishedTime() { return publishedTime; } public void setPublishedTime(LocalDateTime publishedTime) { this.publishedTime = publishedTime; }
    public LocalDateTime getExpirationTime() { return expirationTime; } public void setExpirationTime(LocalDateTime expirationTime) { this.expirationTime = expirationTime; }
}