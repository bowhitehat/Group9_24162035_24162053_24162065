package com.group9.topicmanagement.domain.evaluation;
import com.group9.topicmanagement.domain.BaseEntity;
import com.group9.topicmanagement.domain.RegistrationPeriod;
import jakarta.persistence.*;

@Entity
@Table(name = "evaluation_criteria")
public class EvaluationCriterion extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "registration_period_id", nullable = false)
    private RegistrationPeriod registrationPeriod;
    private Integer displayOrder = 0;
    private Boolean isMandatory = true;
    private Boolean isActive = true;
    
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getDescription() { return description; } public void setDescription(String description) { this.description = description; }
    public RegistrationPeriod getRegistrationPeriod() { return registrationPeriod; } public void setRegistrationPeriod(RegistrationPeriod registrationPeriod) { this.registrationPeriod = registrationPeriod; }
    public Integer getDisplayOrder() { return displayOrder; } public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public Boolean getIsMandatory() { return isMandatory; } public void setIsMandatory(Boolean isMandatory) { this.isMandatory = isMandatory; }
    public Boolean getIsActive() { return isActive; } public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}