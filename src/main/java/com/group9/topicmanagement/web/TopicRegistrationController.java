package com.group9.topicmanagement.web;

import com.group9.topicmanagement.domain.RegistrationPeriod;
import com.group9.topicmanagement.domain.enums.RegistrationStatus;
import com.group9.topicmanagement.domain.registration.TopicRegistration;
import com.group9.topicmanagement.service.DepartmentService;
import com.group9.topicmanagement.service.RegistrationPeriodService;
import com.group9.topicmanagement.service.TopicRegistrationService;
import com.group9.topicmanagement.web.form.RejectForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/registrations")
public class TopicRegistrationController {

    private final TopicRegistrationService registrationService;
    private final RegistrationPeriodService periodService;
    private final DepartmentService departmentService;

    public TopicRegistrationController(TopicRegistrationService registrationService,
                                       RegistrationPeriodService periodService,
                                       DepartmentService departmentService) {
        this.registrationService = registrationService;
        this.periodService = periodService;
        this.departmentService = departmentService;
    }

    @GetMapping
    public String listRegistrations(@RequestParam(required = false) Long periodId,
                                    @RequestParam(required = false) Long departmentId,
                                    @RequestParam(required = false) RegistrationStatus status,
                                    @RequestParam(required = false) String keyword,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    Model model) {
        Pageable pageable = PageRequest.of(page, size);
        List<RegistrationPeriod> periods = periodService.findAllPeriods();
        if (periodId == null && !periods.isEmpty()) {
            periodId = periods.get(0).getId();
        }

        Page<TopicRegistration> registrationPage = (periodId != null) 
                ? registrationService.findRegistrationsWithFilters(periodId, departmentId, status, keyword, pageable)
                : Page.empty();

        model.addAttribute("registrations", registrationPage.getContent());
        model.addAttribute("page", registrationPage);
        model.addAttribute("periods", periods);
        model.addAttribute("departments", departmentService.findAllDepartments());
        model.addAttribute("statuses", RegistrationStatus.values());
        model.addAttribute("selectedPeriodId", periodId);
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("keyword", keyword);
        model.addAttribute("rejectForm", new RejectForm());

        return "registrations/list";
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/register")
    public String registerTopic(@RequestParam Long groupId,
                                @RequestParam Long topicId,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            registrationService.registerTopic(groupId, topicId, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký đề tài thành công, vui lòng chờ duyệt!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/topics/detail/" + topicId;
    }

    @PreAuthorize("hasAnyRole('FACULTY_MANAGER', 'ADMIN')")
    @PostMapping("/approve/{id}")
    public String approveRegistration(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            registrationService.approveRegistration(id, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Phê duyệt đăng ký đề tài thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/registrations";
    }

    @PreAuthorize("hasAnyRole('FACULTY_MANAGER', 'ADMIN')")
    @PostMapping("/reject/{id}")
    public String rejectRegistration(@PathVariable Long id,
                                     @ModelAttribute RejectForm rejectForm,
                                     Principal principal,
                                     RedirectAttributes redirectAttributes) {
        try {
            registrationService.rejectRegistration(id, rejectForm.getReason(), principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối đăng ký đề tài!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/registrations";
    }
}
