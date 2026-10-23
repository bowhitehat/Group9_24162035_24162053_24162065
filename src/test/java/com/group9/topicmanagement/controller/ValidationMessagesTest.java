package com.group9.topicmanagement.controller;

import com.group9.topicmanagement.dto.PeriodForm;
import com.group9.topicmanagement.dto.UserForm;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.security.access.AccessDeniedException;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class ValidationMessagesTest {
    @Test void malformedValueDoesNotLeakRejectedTextOrJavaConversionMessage() {
        var errors = new BeanPropertyBindingResult(new PeriodForm(), "periodForm");
        errors.addError(new FieldError("periodForm", "lecturerStart", "secret-test-value", true,
                new String[]{"typeMismatch"}, null, "Failed to convert java.time.LocalDateTime: secret-test-value"));
        assertThat(ValidationMessages.describe(errors, Map.of())).singleElement().asString()
                .contains("Thời gian giảng viên đăng ký từ", "ngày/tháng/năm")
                .doesNotContain("secret-test-value", "java.time", "Failed to convert");
    }

    @Test void scoreValidationUsesCriterionName() {
        var errors = new BeanPropertyBindingResult(new Object(), "formDto");
        errors.addError(new FieldError("formDto", "scores[7]", "Điểm phải từ 0 đến 10"));
        assertThat(ValidationMessages.describe(errors, Map.of("scores[7]", "Điểm Kỹ thuật")))
                .containsExactly("Điểm Kỹ thuật: Điểm phải từ 0 đến 10");
    }

    @Test void requiredPeriodDatesAllHaveVietnameseMessages() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(new PeriodForm());
            assertThat(violations).hasSize(6);
            assertThat(violations).allMatch(v -> !v.getMessage().contains("must not be null"));
            assertThat(violations.stream().filter(v -> v.getPropertyPath().toString().endsWith("Start")
                    || v.getPropertyPath().toString().endsWith("End")).map(v -> v.getMessage()))
                    .hasSize(4).allMatch(message -> message.startsWith("Vui lòng nhập thời gian"));
        }
    }

    @Test void accountLengthErrorsDoNotUseDefaultEnglishSizeMessage() {
        UserForm form = new UserForm(); form.setUsername("x".repeat(81));
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(form).stream()
                    .filter(v -> v.getPropertyPath().toString().equals("username")).map(v -> v.getMessage()))
                    .containsExactly("Tên đăng nhập tối đa 80 ký tự");
        }
    }

    @Test void deniedBusinessMessageIsKeptAndFrameworkDefaultIsTranslated() {
        var handler = new GlobalExceptionHandler(); var model = new ExtendedModelMap();
        handler.forbidden(new AccessDeniedException("Chỉ trưởng nhóm mới được quyền nộp báo cáo"), model);
        assertThat(model.get("message")).isEqualTo("Chỉ trưởng nhóm mới được quyền nộp báo cáo");
        handler.forbidden(new AccessDeniedException("Access Denied"), model);
        assertThat(model.get("message").toString()).contains("không được phép").doesNotContain("Access Denied");
    }
}
