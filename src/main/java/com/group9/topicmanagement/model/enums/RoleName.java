package com.group9.topicmanagement.model.enums;

public enum RoleName {
    ADMIN, FACULTY_MANAGER, LECTURER, STUDENT;

    public String getDisplayName() {
        return switch (this) {
            case ADMIN -> "Quản trị viên";
            case FACULTY_MANAGER -> "Cán bộ khoa";
            case LECTURER -> "Giảng viên";
            case STUDENT -> "Sinh viên";
        };
    }
}
