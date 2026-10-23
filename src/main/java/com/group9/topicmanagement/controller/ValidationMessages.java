package com.group9.topicmanagement.controller;

import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import java.util.Map;
import java.util.List;

/** Public field labels; never echo rejected values, Java types or exception details. */
public final class ValidationMessages {
    private ValidationMessages() { }
    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("lecturerStart", "Thời gian giảng viên đăng ký từ"),
            Map.entry("lecturerEnd", "Thời gian giảng viên đăng ký đến"),
            Map.entry("studentStart", "Thời gian sinh viên đăng ký từ"),
            Map.entry("studentEnd", "Thời gian sinh viên đăng ký đến"),
            Map.entry("reportSubmissionDeadline", "Hạn nộp báo cáo"), Map.entry("reviewDeadline", "Hạn phản biện"),
            Map.entry("councilDate", "Ngày hội đồng"), Map.entry("reportDate", "Ngày giờ báo cáo"),
            Map.entry("deadline", "Hạn nộp điểm"), Map.entry("expireAt", "Thời gian hết hạn thông báo"),
            Map.entry("username", "Tên đăng nhập"), Map.entry("fullName", "Họ tên"),
            Map.entry("email", "Email"), Map.entry("studentCode", "MSSV"),
            Map.entry("type", "Loại"), Map.entry("periodId", "Đợt đăng ký"),
            Map.entry("registrationPeriodId", "Đợt đăng ký"), Map.entry("topicId", "Đề tài"),
            Map.entry("registrationId", "Đăng ký đề tài"), Map.entry("reviewerId", "Giảng viên phản biện"),
            Map.entry("status", "Trạng thái"), Map.entry("memberId", "Giảng viên hội đồng"),
            Map.entry("role", "Vai trò"), Map.entry("roles", "Vai trò"));

    public static List<String> describe(Errors errors, Map<String, String> labels) {
        return errors.getAllErrors().stream().map(error -> {
            if (error instanceof FieldError field) {
                String label = labels.getOrDefault(field.getField(), label(field.getField()));
                if (field.isBindingFailure()) {
                    boolean date = field.getField().matches(".*(Start|End|Date|Deadline)$")
                            || field.getField().equals("deadline") || field.getField().equals("expireAt");
                    return label + (field.getField().equals("councilDate") ? " không hợp lệ. Vui lòng nhập ngày/tháng/năm hợp lệ."
                            : date ? " không hợp lệ. Vui lòng nhập ngày/tháng/năm và giờ hợp lệ."
                            : " không hợp lệ. Vui lòng chọn hoặc nhập lại.");
                }
                String message = field.getDefaultMessage();
                return label + ": " + (message == null ? "Vui lòng kiểm tra lại." : message);
            }
            return error.getDefaultMessage() == null ? "Chưa thể lưu. Vui lòng kiểm tra thông tin đã nhập."
                    : error.getDefaultMessage();
        }).distinct().toList();
    }

    public static String label(String field) {
        return LABELS.getOrDefault(field, field.startsWith("scores[") ? "Điểm tiêu chí" : "Thông tin đã nhập");
    }
}
