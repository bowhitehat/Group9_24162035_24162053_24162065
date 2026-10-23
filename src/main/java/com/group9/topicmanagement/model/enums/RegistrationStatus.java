package com.group9.topicmanagement.model.enums;

public enum RegistrationStatus {
    PENDING, APPROVED, REJECTED, CANCELLED;

    public String getDisplayName() {
        return switch (this) {
            case PENDING -> "Chờ duyệt";
            case APPROVED -> "Đã duyệt";
            case REJECTED -> "Bị từ chối";
            case CANCELLED -> "Đã hủy";
        };
    }
}
