package com.group9.topicmanagement.controller.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

public class TopicForm {

    private Long id;

    @NotBlank(message = "Mã đề tài không được để trống")
    @Size(max = 20, message = "Mã đề tài không được vượt quá 20 ký tự")
    private String code;

    @NotBlank(message = "Tên đề tài không được để trống")
    @Size(max = 255, message = "Tên đề tài không được vượt quá 255 ký tự")
    private String title;

    private String description;
    
    private String requirement;

    @NotNull(message = "Khoa phụ trách không được để trống")
    private Long departmentId;

    @NotNull(message = "Đợt đăng ký không được để trống")
    private Long registrationPeriodId;

    private List<Long> advisorIds = new ArrayList<>();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRequirement() { return requirement; }
    public void setRequirement(String requirement) { this.requirement = requirement; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public Long getRegistrationPeriodId() { return registrationPeriodId; }
    public void setRegistrationPeriodId(Long registrationPeriodId) { this.registrationPeriodId = registrationPeriodId; }
    public List<Long> getAdvisorIds() { return advisorIds; }
    public void setAdvisorIds(List<Long> advisorIds) { this.advisorIds = advisorIds; }
}
