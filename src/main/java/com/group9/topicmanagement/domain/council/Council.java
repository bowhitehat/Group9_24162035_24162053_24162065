package com.group9.topicmanagement.domain.council;
import com.group9.topicmanagement.domain.BaseEntity;
import com.group9.topicmanagement.domain.RegistrationPeriod;
import com.group9.topicmanagement.domain.enums.CouncilStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "councils")
public class Council extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "registration_period_id", nullable = false)
    private RegistrationPeriod registrationPeriod;
    private LocalDateTime reportDate;
    private String location;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private CouncilStatus status = CouncilStatus.DRAFT;
    
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public RegistrationPeriod getRegistrationPeriod() { return registrationPeriod; } public void setRegistrationPeriod(RegistrationPeriod registrationPeriod) { this.registrationPeriod = registrationPeriod; }
    public LocalDateTime getReportDate() { return reportDate; } public void setReportDate(LocalDateTime reportDate) { this.reportDate = reportDate; }
    public String getLocation() { return location; } public void setLocation(String location) { this.location = location; }
    public CouncilStatus getStatus() { return status; } public void setStatus(CouncilStatus status) { this.status = status; }
}