package com.group9.topicmanagement.controller;

import com.group9.topicmanagement.config.UploadConfig;
import com.group9.topicmanagement.domain.registration.ReportSubmission;
import com.group9.topicmanagement.domain.registration.TopicRegistration;
import com.group9.topicmanagement.service.ReportSubmissionService;
import com.group9.topicmanagement.controller.form.ReportSubmissionForm;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ContentDisposition;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/reports")
public class ReportSubmissionController {

    private final ReportSubmissionService reportService;
    private final UploadConfig uploadConfig;

    public ReportSubmissionController(ReportSubmissionService reportService,
                                      UploadConfig uploadConfig) {
        this.reportService = reportService;
        this.uploadConfig = uploadConfig;
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/submit")
    public String showSubmitForm(@RequestParam Long registrationId, Principal principal, Model model) {
        TopicRegistration registration = reportService.getRegistrationForSubmission(registrationId, principal.getName());
        ReportSubmissionForm form = new ReportSubmissionForm();
        form.setTopicRegistrationId(registrationId);

        List<ReportSubmission> submissions = reportService.getSubmissionHistoryForUser(registrationId, principal.getName());

        model.addAttribute("reportForm", form);
        model.addAttribute("registration", registration);
        model.addAttribute("submissions", submissions);
        return "reports/submit";
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/submit")
    public String submitReport(@Valid @ModelAttribute("reportForm") ReportSubmissionForm form,
                               BindingResult bindingResult,
                               Principal principal,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            TopicRegistration registration = reportService.getRegistrationForSubmission(form.getTopicRegistrationId(), principal.getName());
            model.addAttribute("registration", registration);
            model.addAttribute("submissions", reportService.getSubmissionHistoryForUser(form.getTopicRegistrationId(), principal.getName()));
            return "reports/submit";
        }

        try {
            reportService.submitReport(form, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Nộp báo cáo thành công!");
            return "redirect:/reports/submit?registrationId=" + form.getTopicRegistrationId();
        } catch (AccessDeniedException error) {
            throw error;
        } catch (Exception e) {
            TopicRegistration registration = reportService.getRegistrationForSubmission(form.getTopicRegistrationId(), principal.getName());
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("registration", registration);
            model.addAttribute("submissions", reportService.getSubmissionHistoryForUser(form.getTopicRegistrationId(), principal.getName()));
            return "reports/submit";
        }
    }

    @GetMapping("/history/{registrationId}")
    public String viewHistory(@PathVariable Long registrationId, Principal principal, Model model) {
        List<ReportSubmission> submissions = reportService.getSubmissionHistoryForUser(registrationId, principal.getName());
        TopicRegistration registration = reportService.getRegistrationForUser(registrationId, principal.getName());

        model.addAttribute("registration", registration);
        model.addAttribute("submissions", submissions);
        return "reports/history";
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadReport(@PathVariable Long id, Principal principal) {
        ReportSubmission submission = reportService.getSubmissionByIdForUser(id, principal.getName());
        Path uploadDirectory = uploadConfig.getUploadDirectory().toAbsolutePath().normalize();
        Path filePath = uploadDirectory.resolve(submission.getStoredFileName()).normalize();
        if (!filePath.startsWith(uploadDirectory)) throw new AccessDeniedException("Đường dẫn tập tin không hợp lệ");

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("Không tìm thấy tệp báo cáo");
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(submission.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(submission.getOriginalFileName(), StandardCharsets.UTF_8).build().toString())
                    .body(resource);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Lỗi tải tệp báo cáo", e);
        }
    }
}
