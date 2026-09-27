package com.group9.topicmanagement.controller.form;

import com.group9.topicmanagement.domain.enums.RoleName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

public class AnnouncementForm {
    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề không được vượt quá 255 ký tự")
    private String title;

    @NotBlank(message = "Nội dung không được để trống")
    private String content;

    private Set<RoleName> targetRoles = new LinkedHashSet<>();
    private boolean publish;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime expirationTime;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Set<RoleName> getTargetRoles() { return targetRoles; }
    public void setTargetRoles(Set<RoleName> targetRoles) { this.targetRoles = targetRoles; }
    public boolean isPublish() { return publish; }
    public void setPublish(boolean publish) { this.publish = publish; }
    public LocalDateTime getExpirationTime() { return expirationTime; }
    public void setExpirationTime(LocalDateTime expirationTime) { this.expirationTime = expirationTime; }
}
