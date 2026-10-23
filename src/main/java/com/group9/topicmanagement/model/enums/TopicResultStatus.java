package com.group9.topicmanagement.model.enums;

public enum TopicResultStatus {
    PENDING_CONFIRMATION, CONFIRMED, PUBLISHED;

    public String getDisplayName() {
        return switch (this) {
            case PENDING_CONFIRMATION -> "Chờ Chủ tịch xác nhận";
            case CONFIRMED -> "Đã xác nhận, chờ khoa công bố";
            case PUBLISHED -> "Đã công bố";
        };
    }
}
