package com.group9.topicmanagement.web.form;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class ReviewerAssignmentForm {
    @NotNull(message = "Phải chọn đề tài")
    private Long topicId;

    @NotNull(message = "Phải chọn giảng viên phản biện")
    private Long reviewerId;

    @NotNull(message = "Phải nhập hạn nộp")
    @Future(message = "Hạn nộp phải ở tương lai")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime deadline;

    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }
    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
}
