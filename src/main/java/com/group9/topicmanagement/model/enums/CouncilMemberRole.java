package com.group9.topicmanagement.model.enums;

public enum CouncilMemberRole {
    CHAIR, SECRETARY, MEMBER;

    public String getDisplayName() {
        return switch (this) {
            case CHAIR -> "Chủ tịch";
            case SECRETARY -> "Thư ký";
            case MEMBER -> "Thành viên";
        };
    }
}
