package com.group9.topicmanagement.domain.council;
import com.group9.topicmanagement.domain.BaseEntity;
import com.group9.topicmanagement.domain.User;
import com.group9.topicmanagement.domain.enums.CouncilMemberRole;
import jakarta.persistence.*;

@Entity
@Table(name = "council_members")
public class CouncilMember extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "council_id", nullable = false)
    private Council council;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "member_id", nullable = false)
    private User member;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private CouncilMemberRole role;
    
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Council getCouncil() { return council; } public void setCouncil(Council council) { this.council = council; }
    public User getMember() { return member; } public void setMember(User member) { this.member = member; }
    public CouncilMemberRole getRole() { return role; } public void setRole(CouncilMemberRole role) { this.role = role; }
}