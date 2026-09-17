package com.group9.topicmanagement.web.form;

import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReportSubmissionForm {

    @NotNull(message = "ID đăng ký đề tài không được để trống")
    private Long topicRegistrationId;

    private MultipartFile file;

    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    private String note;

    public Long getTopicRegistrationId() { return topicRegistrationId; }
    public void setTopicRegistrationId(Long topicRegistrationId) { this.topicRegistrationId = topicRegistrationId; }

    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
