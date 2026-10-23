package com.group9.topicmanagement.model.enums;

public enum EvaluationStatus {
    DRAFT, SUBMITTED, LOCKED;

    public String getDisplayName() {
        return switch (this) {
            case DRAFT -> "Nháp";
            case SUBMITTED -> "Đã nộp";
            case LOCKED -> "Đã khóa";
        };
    }
}
