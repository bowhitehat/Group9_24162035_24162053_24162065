package com.group9.topicmanagement.controller.form;

import jakarta.validation.constraints.NotNull;

public class CouncilTopicForm {
    @NotNull(message = "Phải chọn đề tài")
    private Long topicId;

    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }
}
