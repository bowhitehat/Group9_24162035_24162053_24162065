package com.group9.topicmanagement.controller;

import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.exception.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(AccessDeniedException.class) @ResponseStatus(HttpStatus.FORBIDDEN)
    String forbidden(AccessDeniedException error, Model model) { model.addAttribute("message", error.getMessage()); return "error/403"; }
    @ExceptionHandler(BusinessRuleException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    String business(BusinessRuleException error, Model model) { model.addAttribute("message", error.getMessage()); return "error/400"; }
    @ExceptionHandler(NotFoundException.class) @ResponseStatus(HttpStatus.NOT_FOUND)
    String notFound(NotFoundException error, Model model) { model.addAttribute("message", error.getMessage()); return "error/404"; }
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String missingResource(Exception error, Model model) { model.addAttribute("message", "Không tìm thấy trang hoặc tài nguyên yêu cầu"); return "error/404"; }

    @ExceptionHandler({MissingServletRequestParameterException.class, ServletRequestBindingException.class,
            MethodArgumentTypeMismatchException.class, HandlerMethodValidationException.class,
            BindException.class, ConstraintViolationException.class, IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String invalidInput(Exception error, Model model) {
        model.addAttribute("message", "Dữ liệu không hợp lệ hoặc thiếu thông tin bắt buộc. Vui lòng kiểm tra lại.");
        return "error/400";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    String uploadTooLarge(MaxUploadSizeExceededException error, Model model) {
        model.addAttribute("message", "Tập tin vượt giới hạn 10 MB. Vui lòng chọn file nhỏ hơn và nộp lại.");
        return "error/413";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    String conflictingData(DataIntegrityViolationException error, Model model) {
        model.addAttribute("message", "Dữ liệu đã tồn tại hoặc đang được sử dụng. Vui lòng kiểm tra và thử lại.");
        return "error/409";
    }
    @ExceptionHandler(Exception.class) @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    String unexpected(Exception error, Model model) { log.error("Unhandled request error", error); model.addAttribute("message", "Đã xảy ra lỗi không mong muốn"); return "error/500"; }
}
