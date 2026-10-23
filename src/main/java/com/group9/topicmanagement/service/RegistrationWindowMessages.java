package com.group9.topicmanagement.service;

import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.model.RegistrationPeriod;
import com.group9.topicmanagement.model.enums.PeriodStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Shared wording only: status and calendar eligibility remain separate checks. */
public final class RegistrationWindowMessages {
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private RegistrationWindowMessages() { }

    public static String date(LocalDateTime value) {
        return value == null ? "chưa thiết lập" : DISPLAY.format(value);
    }

    public static void requireWindow(LocalDateTime start, LocalDateTime end, Clock clock, String action) {
        if (start == null || end == null) {
            throw new BusinessRuleException("Đợt chưa được thiết lập đủ thời gian " + action + ". Vui lòng liên hệ khoa.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.isBefore(start)) {
            throw new BusinessRuleException("Chưa đến thời gian " + action + ". Mở từ " + date(start) + ".");
        }
        if (now.isAfter(end)) {
            throw new BusinessRuleException("Đã quá thời hạn " + action + ". Hạn cuối là " + date(end) + ".");
        }
    }

    public static void requireStudentPhase(RegistrationPeriod period, String action) {
        PeriodStatus status = period.getStatus();
        if (status == null) throw new BusinessRuleException("Đợt chưa được thiết lập trạng thái. Vui lòng liên hệ khoa.");
        if (status == PeriodStatus.STUDENT_REGISTRATION) return;
        boolean notOpened = status == PeriodStatus.DRAFT || status == PeriodStatus.LECTURER_REGISTRATION
                || status == PeriodStatus.TOPIC_PUBLISHED;
        throw new BusinessRuleException((notOpened ? "Khoa chưa mở" : "Đã kết thúc")
                + " giai đoạn sinh viên đăng ký đề tài; không thể " + action
                + ". Trạng thái đợt: " + (status == null ? "chưa thiết lập" : status.getDisplayName())
                + ". Vui lòng liên hệ khoa nếu cần hỗ trợ.");
    }

    public static String reviewerPhase(PeriodStatus status) {
        boolean ended = status == PeriodStatus.RESULT_PUBLISHED || status == PeriodStatus.CLOSED;
        return (ended ? "Đợt đã kết thúc giai đoạn phản biện." : "Đợt chưa mở giai đoạn phản biện.")
                + " Chỉ được phân công khi đợt đang thực hiện hoặc đang chấm điểm. Trạng thái hiện tại: "
                + (status == null ? "chưa thiết lập" : status.getDisplayName()) + ".";
    }
}
