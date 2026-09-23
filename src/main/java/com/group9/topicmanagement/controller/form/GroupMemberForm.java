package com.group9.topicmanagement.controller.form;

import jakarta.validation.constraints.NotNull;

public class GroupMemberForm {

    @NotNull(message = "ID sinh viên không được để trống")
    private Long studentId;

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
}
