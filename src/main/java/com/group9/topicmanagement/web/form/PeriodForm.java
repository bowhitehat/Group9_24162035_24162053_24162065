package com.group9.topicmanagement.web.form;

import com.group9.topicmanagement.domain.RegistrationPeriod;
import com.group9.topicmanagement.domain.enums.PeriodType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PeriodForm {
    @NotBlank(message = "Tên đợt là bắt buộc") private String name;
    @NotNull(message = "Loại đợt là bắt buộc") private PeriodType type;
    @NotNull @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") private LocalDateTime lecturerStart;
    @NotNull @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") private LocalDateTime lecturerEnd;
    @NotNull @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") private LocalDateTime studentStart;
    @NotNull @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") private LocalDateTime studentEnd;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") private LocalDateTime reviewDeadline;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate councilDate;
    public RegistrationPeriod toEntity() { RegistrationPeriod p = new RegistrationPeriod(); p.setName(name); p.setType(type); p.setLecturerStart(lecturerStart); p.setLecturerEnd(lecturerEnd); p.setStudentStart(studentStart); p.setStudentEnd(studentEnd); p.setReviewDeadline(reviewDeadline); p.setCouncilDate(councilDate); return p; }
    public static PeriodForm from(RegistrationPeriod p) { PeriodForm f = new PeriodForm(); f.name=p.getName(); f.type=p.getType(); f.lecturerStart=p.getLecturerStart(); f.lecturerEnd=p.getLecturerEnd(); f.studentStart=p.getStudentStart(); f.studentEnd=p.getStudentEnd(); f.reviewDeadline=p.getReviewDeadline(); f.councilDate=p.getCouncilDate(); return f; }
    public String getName(){return name;} public void setName(String v){name=v;} public PeriodType getType(){return type;} public void setType(PeriodType v){type=v;}
    public LocalDateTime getLecturerStart(){return lecturerStart;} public void setLecturerStart(LocalDateTime v){lecturerStart=v;} public LocalDateTime getLecturerEnd(){return lecturerEnd;} public void setLecturerEnd(LocalDateTime v){lecturerEnd=v;}
    public LocalDateTime getStudentStart(){return studentStart;} public void setStudentStart(LocalDateTime v){studentStart=v;} public LocalDateTime getStudentEnd(){return studentEnd;} public void setStudentEnd(LocalDateTime v){studentEnd=v;}
    public LocalDateTime getReviewDeadline(){return reviewDeadline;} public void setReviewDeadline(LocalDateTime v){reviewDeadline=v;} public LocalDate getCouncilDate(){return councilDate;} public void setCouncilDate(LocalDate v){councilDate=v;}
}
