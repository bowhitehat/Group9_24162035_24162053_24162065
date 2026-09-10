package com.group9.topicmanagement.domain;

import com.group9.topicmanagement.domain.enums.PeriodStatus;
import com.group9.topicmanagement.domain.enums.PeriodType;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "registration_periods")
public class RegistrationPeriod extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 180)
    private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private PeriodType type;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private PeriodStatus status = PeriodStatus.DRAFT;
    @Column(nullable = false) private LocalDateTime lecturerStart;
    @Column(nullable = false) private LocalDateTime lecturerEnd;
    @Column(nullable = false) private LocalDateTime studentStart;
    @Column(nullable = false) private LocalDateTime studentEnd;
    private LocalDateTime reviewDeadline;
    private LocalDate councilDate;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public PeriodType getType() { return type; }
    public void setType(PeriodType type) { this.type = type; }
    public PeriodStatus getStatus() { return status; }
    public void setStatus(PeriodStatus status) { this.status = status; }
    public LocalDateTime getLecturerStart() { return lecturerStart; }
    public void setLecturerStart(LocalDateTime lecturerStart) { this.lecturerStart = lecturerStart; }
    public LocalDateTime getLecturerEnd() { return lecturerEnd; }
    public void setLecturerEnd(LocalDateTime lecturerEnd) { this.lecturerEnd = lecturerEnd; }
    public LocalDateTime getStudentStart() { return studentStart; }
    public void setStudentStart(LocalDateTime studentStart) { this.studentStart = studentStart; }
    public LocalDateTime getStudentEnd() { return studentEnd; }
    public void setStudentEnd(LocalDateTime studentEnd) { this.studentEnd = studentEnd; }
    public LocalDateTime getReviewDeadline() { return reviewDeadline; }
    public void setReviewDeadline(LocalDateTime reviewDeadline) { this.reviewDeadline = reviewDeadline; }
    public LocalDate getCouncilDate() { return councilDate; }
    public void setCouncilDate(LocalDate councilDate) { this.councilDate = councilDate; }
}
