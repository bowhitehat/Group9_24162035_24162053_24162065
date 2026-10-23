package com.group9.topicmanagement.model.enums;

public enum EvaluationType {
    REVIEWER, COUNCIL_MEMBER;

    public String getDisplayName() {
        return switch (this) {
            case REVIEWER -> "Giảng viên phản biện";
            case COUNCIL_MEMBER -> "Thành viên hội đồng";
        };
    }
}
