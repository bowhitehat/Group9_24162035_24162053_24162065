package com.group9.topicmanagement.model.enums;

import java.util.EnumSet;
import java.util.Set;

public enum PeriodStatus {
    DRAFT,
    LECTURER_REGISTRATION,
    TOPIC_PUBLISHED,
    STUDENT_REGISTRATION,
    IN_PROGRESS,
    GRADING,
    RESULT_PUBLISHED,
    CLOSED;

    public String getDisplayName() {
        return switch (this) {
            case DRAFT -> "Nháp";
            case LECTURER_REGISTRATION -> "Giảng viên đăng ký đề tài";
            case TOPIC_PUBLISHED -> "Công bố đề tài";
            case STUDENT_REGISTRATION -> "Sinh viên đăng ký đề tài";
            case IN_PROGRESS -> "Đang thực hiện";
            case GRADING -> "Đang chấm điểm";
            case RESULT_PUBLISHED -> "Đã công bố kết quả";
            case CLOSED -> "Đã đóng đợt";
        };
    }

    public String transitionHint() {
        return allowedNext().isEmpty() ? "Đợt đã đóng, không còn bước chuyển tiếp."
                : "Bước tiếp theo hợp lệ: " + allowedNext().iterator().next().getDisplayName() + ".";
    }

    public boolean canTransitionTo(PeriodStatus next) {
        return allowedNext().contains(next);
    }

    private Set<PeriodStatus> allowedNext() {
        return switch (this) {
            case DRAFT -> EnumSet.of(LECTURER_REGISTRATION);
            case LECTURER_REGISTRATION -> EnumSet.of(TOPIC_PUBLISHED);
            case TOPIC_PUBLISHED -> EnumSet.of(STUDENT_REGISTRATION);
            case STUDENT_REGISTRATION -> EnumSet.of(IN_PROGRESS);
            case IN_PROGRESS -> EnumSet.of(GRADING);
            case GRADING -> EnumSet.of(RESULT_PUBLISHED);
            case RESULT_PUBLISHED -> EnumSet.of(CLOSED);
            case CLOSED -> EnumSet.noneOf(PeriodStatus.class);
        };
    }
}
