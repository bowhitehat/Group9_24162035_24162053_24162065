package com.group9.topicmanagement.model.enums;

public enum ReviewerAssignmentStatus {
    ASSIGNED, IN_PROGRESS, SUBMITTED, OVERDUE, CANCELLED;

    public String getDisplayName() {
        return switch (this) {
            case ASSIGNED -> "Đã phân công";
            case IN_PROGRESS -> "Đang chấm";
            case SUBMITTED -> "Đã nộp điểm";
            case OVERDUE -> "Quá hạn";
            case CANCELLED -> "Đã hủy";
        };
    }
}
