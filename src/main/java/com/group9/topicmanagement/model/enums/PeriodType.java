package com.group9.topicmanagement.model.enums;

public enum PeriodType {
    MON_HOC, NCKH, TLCN, KLTN;

    public String getDisplayName() {
        return switch (this) {
            case MON_HOC -> "Môn học";
            case NCKH -> "Nghiên cứu khoa học";
            case TLCN -> "Tiểu luận chuyên ngành";
            case KLTN -> "Khóa luận tốt nghiệp";
        };
    }
}
