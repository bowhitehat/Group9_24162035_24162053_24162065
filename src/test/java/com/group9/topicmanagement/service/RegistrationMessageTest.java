package com.group9.topicmanagement.service;

import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.model.RegistrationPeriod;
import com.group9.topicmanagement.model.enums.PeriodStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.*;
import static org.assertj.core.api.Assertions.*;

class RegistrationMessageTest {
    private final LocalDateTime now = LocalDateTime.of(2026, 10, 23, 18, 0);
    private final Clock clock = Clock.fixed(now.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(), ZoneId.of("Asia/Ho_Chi_Minh"));

    @ParameterizedTest @ValueSource(strings={"đăng ký đề tài", "giảng viên đề xuất đề tài", "sinh viên quản lý nhóm", "duyệt đăng ký đề tài"})
    void distinguishesEarlyAndExpiredWithVietnameseDate(String action) {
        assertThatThrownBy(() -> RegistrationWindowMessages.requireWindow(now.plusHours(1), now.plusHours(2), clock, action))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("Chưa đến thời gian " + action)
                .hasMessageContaining("23/10/2026 19:00").hasMessageNotContaining("Đã quá");
        assertThatThrownBy(() -> RegistrationWindowMessages.requireWindow(now.minusHours(2), now.minusHours(1), clock, action))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("Đã quá thời hạn " + action)
                .hasMessageContaining("23/10/2026 17:00").hasMessageNotContaining("Chưa đến");
    }

    @Test void bothBoundaryInstantsRemainAllowed() {
        RegistrationWindowMessages.requireWindow(now, now.plusHours(1), clock, "đăng ký đề tài");
        RegistrationWindowMessages.requireWindow(now.minusHours(1), now, clock, "đăng ký đề tài");
    }

    @ParameterizedTest @EnumSource(value=PeriodStatus.class, names={"DRAFT","LECTURER_REGISTRATION","TOPIC_PUBLISHED"})
    void unopenedPhaseIsNotReportedAsExpired(PeriodStatus status) {
        RegistrationPeriod p = new RegistrationPeriod(); p.setStatus(status);
        assertThatThrownBy(() -> RegistrationWindowMessages.requireStudentPhase(p, "tạo nhóm"))
                .hasMessageContaining("Khoa chưa mở").hasMessageContaining(status.getDisplayName()).hasMessageNotContaining("hạn");
    }

    @ParameterizedTest @EnumSource(value=PeriodStatus.class, names={"IN_PROGRESS","GRADING","RESULT_PUBLISHED","CLOSED"})
    void closedStudentPhaseIsNotReportedAsNotYetOpen(PeriodStatus status) {
        RegistrationPeriod p = new RegistrationPeriod(); p.setStatus(status);
        assertThatThrownBy(() -> RegistrationWindowMessages.requireStudentPhase(p, "duyệt đăng ký"))
                .hasMessageContaining("Đã kết thúc").hasMessageNotContaining("chưa mở");
    }

    @ParameterizedTest @EnumSource(value=PeriodStatus.class, names={"RESULT_PUBLISHED","CLOSED"})
    void reviewerMessageDoesNotSayNotYetForFinishedPhases(PeriodStatus status) {
        assertThat(RegistrationWindowMessages.reviewerPhase(status)).contains("đã kết thúc").doesNotContain("chưa mở");
    }

    @Test void allowedStudentPhaseAndNextStepHaveClearLabels() {
        RegistrationPeriod p = new RegistrationPeriod(); p.setStatus(PeriodStatus.STUDENT_REGISTRATION);
        RegistrationWindowMessages.requireStudentPhase(p, "đăng ký đề tài");
        assertThat(PeriodStatus.DRAFT.transitionHint()).contains("Giảng viên đăng ký đề tài");
        assertThat(PeriodStatus.CLOSED.transitionHint()).contains("không còn bước chuyển tiếp");
    }
}
