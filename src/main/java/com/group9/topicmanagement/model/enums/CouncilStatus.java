package com.group9.topicmanagement.model.enums;

public enum CouncilStatus {
    DRAFT, ACTIVE, COMPLETED, CANCELLED;

    public String getDisplayName() {
        return switch (this) {
            case DRAFT -> "Nháp";
            case ACTIVE -> "Đang hoạt động";
            case COMPLETED -> "Đã hoàn tất";
            case CANCELLED -> "Đã hủy";
        };
    }
}
