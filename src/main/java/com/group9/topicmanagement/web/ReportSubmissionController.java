package com.group9.topicmanagement.web;

import com.group9.topicmanagement.config.UploadConfig;
import com.group9.topicmanagement.domain.registration.ReportSubmission;
import com.group9.topicmanagement.domain.registration.TopicRegistration;
import com.group9.topicmanagement.service.ReportSubmissionService;
import com.group9.topicmanagement.service.TopicRegistrationService;
import com.group9.topicmanagement.web.form.ReportSubmissionForm;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/reports")
public class ReportSubmissionController {

    private final ReportSubmissionService reportService;
    private final TopicRegistrationService registrationService;
    private final UploadConfig uploadConfig;

    public ReportSubmissionController(ReportSubmissionService reportService,
                                      TopicRegistrationService registrationService,
                                      UploadConfig uploadConfig) {
        this.reportService = reportService;
        this.registrationService = registrationService;
        this.uploadConfig = uploadConfig;
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/submit")
    public String showSubmitForm(@RequestParam Long registrationId, Model model) {
        TopicRegistration registration = registrationService.getRegistrationById(registrationId);
        ReportSubmissionForm form = new ReportSubmissionForm();
        form.setTopicRegistrationId(registrationId);

        List<ReportSubmission> submissions = reportService.getSubmissionHistory(registrationId);

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
            TopicRegistration registration = registrationService.getRegistrationById(form.getTopicRegistrationId());
            model.addAttribute("registration", registration);
            model.addAttribute("submissions", reportService.getSubmissionHistory(form.getTopicRegistrationId()));
            return "reports/submit";
        }

        try {
            reportService.submitReport(form, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Nộp báo cáo thành công!");
            return "redirect:/reports/submit?registrationId=" + form.getTopicRegistrationId();
        } catch (Exception e) {
            TopicRegistration registration = registrationService.getRegistrationById(form.getTopicRegistrationId());
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("registration", registration);
            model.addAttribute("submissions", reportService.getSubmissionHistory(form.getTopicRegistrationId()));
            return "reports/submit";
        }
    }

    @GetMapping("/history/{registrationId}")
    public String viewHistory(@PathVariable Long registrationId, Model model) {
        TopicRegistration registration = registrationService.getRegistrationById(registrationId);
        List<ReportSubmission> submissions = reportService.getSubmissionHistory(registrationId);

        model.addAttribute("registration", registration);
        model.addAttribute("submissions", submissions);
        return "reports/history";
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadReport(@PathVariable Long id) {
        ReportSubmission submission = reportService.getSubmissionById(id);
        Path filePath = uploadConfig.getUploadDirectory().resolve(submission.getStoredFileName());

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("Không tìm thấy tệp báo cáo");
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(submission.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + submission.getOriginalFileName() + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Lỗi tải tệp báo cáo", e);
        }
    }
}
