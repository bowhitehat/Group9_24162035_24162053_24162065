package com.group9.topicmanagement.model.topic;

public enum TopicStatus {
    DRAFT, PENDING, APPROVED, REJECTED, PUBLISHED, ASSIGNED, COMPLETED;

    public String getDisplayName() {
        return switch (this) {
            case DRAFT -> "Nháp";
            case PENDING -> "Chờ duyệt";
            case APPROVED -> "Đã duyệt";
            case REJECTED -> "Bị từ chối";
            case PUBLISHED -> "Đã công bố";
            case ASSIGNED -> "Đã phân công";
            case COMPLETED -> "Đã hoàn tất";
        };
    }
}
