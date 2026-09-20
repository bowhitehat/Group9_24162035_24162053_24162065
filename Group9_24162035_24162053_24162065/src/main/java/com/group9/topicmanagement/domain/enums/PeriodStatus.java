package com.group9.topicmanagement.domain.enums;

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
