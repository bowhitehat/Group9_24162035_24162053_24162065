package com.group9.topicmanagement.model.studentgroup;

import com.group9.topicmanagement.model.BaseEntity;
import com.group9.topicmanagement.model.RegistrationPeriod;
import com.group9.topicmanagement.model.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "student_group",
       uniqueConstraints = {@UniqueConstraint(columnNames = {"registration_period_id", "leader_id"})})
public class StudentGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_period_id", nullable = false, foreignKey = @ForeignKey(name = "fk_student_group_registration_period_id"))
    private RegistrationPeriod registrationPeriod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_id", nullable = false, foreignKey = @ForeignKey(name = "fk_student_group_leader_id"))
    private User leader;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<GroupMember> members = new HashSet<>();

    // audit fields inherited from BaseEntity

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public RegistrationPeriod getRegistrationPeriod() { return registrationPeriod; }
    public void setRegistrationPeriod(RegistrationPeriod registrationPeriod) { this.registrationPeriod = registrationPeriod; }
    public User getLeader() { return leader; }
    public void setLeader(User leader) { this.leader = leader; }
    public Set<GroupMember> getMembers() { return members; }
    public void setMembers(Set<GroupMember> members) { this.members = members; }
}
