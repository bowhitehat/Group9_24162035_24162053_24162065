package com.group9.topicmanagement.web;

import com.group9.topicmanagement.domain.enums.ReviewerAssignmentStatus;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.service.RegistrationPeriodService;
import com.group9.topicmanagement.service.ReviewerAssignmentService;
import com.group9.topicmanagement.service.TopicRegistrationService;
import com.group9.topicmanagement.service.UserService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/reviewer-assignments")
public class ReviewerAssignmentController {
    private final ReviewerAssignmentService assignmentService;
    private final TopicRegistrationService registrationService;
    private final UserService userService;
    private final RegistrationPeriodService periodService;

    public ReviewerAssignmentController(ReviewerAssignmentService assignmentService,
                                        TopicRegistrationService registrationService,
                                        UserService userService,
                                        RegistrationPeriodService periodService) {
        this.assignmentService = assignmentService;
        this.registrationService = registrationService;
        this.userService = userService;
        this.periodService = periodService;
    }

    @GetMapping
    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public String listAll(@RequestParam(required = false) Long periodId,
                          @RequestParam(required = false) Long departmentId,
                          @RequestParam(required = false) Long reviewerId,
                          @RequestParam(required = false) ReviewerAssignmentStatus status,
                          @RequestParam(required = false) boolean missingReviewer,
                          @RequestParam(required = false) boolean pendingScore,
                          Model model) {
        model.addAttribute("assignments", assignmentService.listAssignments(
                periodId, departmentId, reviewerId, status, missingReviewer, pendingScore));
        model.addAttribute("topicsWithoutReviewer", assignmentService.topicsWithoutReviewer());
        model.addAttribute("periods", periodService.findAllPeriods());
        model.addAttribute("lecturers", userService.findUsersByRole("LECTURER"));
        model.addAttribute("statuses", ReviewerAssignmentStatus.values());
        model.addAttribute("periodId", periodId);
        model.addAttribute("reviewerId", reviewerId);
        model.addAttribute("status", status);
        model.addAttribute("pendingScore", pendingScore);
        model.addAttribute("missingReviewer", missingReviewer);
        return "reviewer_assignments/list";
    }

    @GetMapping("/my-assignments")
    @PreAuthorize("hasRole('LECTURER')")
    public String myAssignments(Authentication auth, Model model) {
        model.addAttribute("assignments", assignmentService.getMyAssignments(auth.getName()));
        return "reviewer_assignments/my-assignments";
    }

    @GetMapping("/create")
    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public String showCreateForm(Model model) {
        model.addAttribute("approvedRegistrations", registrationService.listApprovedRegistrations());
        model.addAttribute("lecturers", userService.findUsersByRole("LECTURER"));
        return "reviewer_assignments/form";
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public String createAssignment(@RequestParam Long topicId,
                                   @RequestParam Long reviewerId,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deadline,
                                   Authentication auth,
                                   RedirectAttributes redirectAttributes) {
        try {
            assignmentService.assignReviewer(topicId, reviewerId, auth.getName(), deadline);
            redirectAttributes.addFlashAttribute("successMessage", "Phân công phản biện thành công");
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/reviewer-assignments/create";
        }
        return "redirect:/reviewer-assignments";
    }

    @PostMapping("/{id}/change")
    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public String changeReviewer(@PathVariable Long id,
                                 @RequestParam Long reviewerId,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deadline,
                                 RedirectAttributes redirectAttributes) {
        try {
            assignmentService.changeReviewer(id, reviewerId, deadline);
            redirectAttributes.addFlashAttribute("successMessage", "Đã đổi giảng viên phản biện");
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/reviewer-assignments";
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public String cancelAssignment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            assignmentService.cancelAssignment(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy phân công");
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/reviewer-assignments";
    }
}
