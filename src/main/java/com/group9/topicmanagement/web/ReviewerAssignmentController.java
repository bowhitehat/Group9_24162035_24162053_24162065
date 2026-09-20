package com.group9.topicmanagement.web;

import com.group9.topicmanagement.domain.User;
import com.group9.topicmanagement.domain.enums.RoleName;
import com.group9.topicmanagement.domain.evaluation.ReviewerAssignment;
import com.group9.topicmanagement.domain.registration.TopicRegistration;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.repository.ReviewerAssignmentRepository;
import com.group9.topicmanagement.repository.TopicRegistrationRepository;
import com.group9.topicmanagement.repository.UserRepository;
import com.group9.topicmanagement.service.ReviewerAssignmentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/reviewer-assignments")
public class ReviewerAssignmentController {
    private final ReviewerAssignmentService assignmentService;
    private final ReviewerAssignmentRepository assignmentRepository;
    private final TopicRegistrationRepository registrationRepository;
    private final UserRepository userRepository;

    public ReviewerAssignmentController(ReviewerAssignmentService assignmentService,
                                        ReviewerAssignmentRepository assignmentRepository,
                                        TopicRegistrationRepository registrationRepository,
                                        UserRepository userRepository) {
        this.assignmentService = assignmentService;
        this.assignmentRepository = assignmentRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public String listAll(Model model) {
        model.addAttribute("assignments", assignmentRepository.findAll());
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
        List<TopicRegistration> approvedRegistrations = registrationRepository.findAll().stream()
                .filter(r -> "APPROVED".equals(r.getStatus().name()))
                .toList();
        
        List<User> lecturers = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName() == RoleName.LECTURER))
                .toList();

        model.addAttribute("approvedRegistrations", approvedRegistrations);
        model.addAttribute("lecturers", lecturers);
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
