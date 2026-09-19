package com.group9.topicmanagement.domain.studentgroup;

import com.group9.topicmanagement.domain.BaseEntity;
import com.group9.topicmanagement.domain.User;
import jakarta.persistence.*;

@Entity
@Table(name = "group_member",
       uniqueConstraints = {@UniqueConstraint(columnNames = {"group_id", "member_id"})})
public class GroupMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private StudentGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private User member;

    @Column(nullable = false)
    private boolean isLeader;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public StudentGroup getGroup() { return group; }
    public void setGroup(StudentGroup group) { this.group = group; }
    public User getMember() { return member; }
    public void setMember(User member) { this.member = member; }
    public boolean isLeader() { return isLeader; }
    public void setLeader(boolean leader) { isLeader = leader; }
}
