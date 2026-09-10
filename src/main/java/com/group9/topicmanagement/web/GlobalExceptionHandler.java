package com.group9.topicmanagement.web;

import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.exception.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessRuleException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    String business(BusinessRuleException error, Model model) { model.addAttribute("message", error.getMessage()); return "error/400"; }
    @ExceptionHandler(NotFoundException.class) @ResponseStatus(HttpStatus.NOT_FOUND)
    String notFound(NotFoundException error, Model model) { model.addAttribute("message", error.getMessage()); return "error/404"; }
    @ExceptionHandler(Exception.class) @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    String unexpected(Exception error, Model model) { model.addAttribute("message", "Đã xảy ra lỗi không mong muốn"); return "error/500"; }
}
