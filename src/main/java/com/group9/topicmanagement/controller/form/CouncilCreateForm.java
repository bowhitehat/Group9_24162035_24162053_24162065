package com.group9.topicmanagement.controller.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class CouncilCreateForm {
    @NotBlank(message = "Tên hội đồng không được để trống")
    @Size(max = 255, message = "Tên hội đồng không được vượt quá 255 ký tự")
    private String name;

    @NotNull(message = "Phải chọn đợt đăng ký")
    private Long periodId;

    @NotNull(message = "Phải nhập ngày báo cáo")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime reportDate;

    @NotBlank(message = "Địa điểm không được để trống")
    @Size(max = 255, message = "Địa điểm không được vượt quá 255 ký tự")
    private String location;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getPeriodId() { return periodId; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }
    public LocalDateTime getReportDate() { return reportDate; }
    public void setReportDate(LocalDateTime reportDate) { this.reportDate = reportDate; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
