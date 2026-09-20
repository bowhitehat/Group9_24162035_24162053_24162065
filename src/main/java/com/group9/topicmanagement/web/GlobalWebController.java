package com.group9.topicmanagement.web;

import com.group9.topicmanagement.service.DepartmentService;
import com.group9.topicmanagement.service.RegistrationPeriodService;
import com.group9.topicmanagement.service.UserService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import com.group9.topicmanagement.repository.TopicRegistrationRepository;
import com.group9.topicmanagement.repository.TopicRepository;
import com.group9.topicmanagement.repository.ReviewerAssignmentRepository;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class GlobalWebController {
    private final UserService userService; private final DepartmentService departmentService; private final RegistrationPeriodService periodService; private final TopicRegistrationRepository registrationRepository; private final TopicRepository topicRepository; private final ReviewerAssignmentRepository reviewerAssignmentRepository;
    
public GlobalWebController(UserService userService, DepartmentService departmentService, RegistrationPeriodService periodService,
                           TopicRegistrationRepository registrationRepository, TopicRepository topicRepository, ReviewerAssignmentRepository reviewerAssignmentRepository) {
    this.userService = userService;
    this.departmentService = departmentService;
    this.periodService = periodService;
    this.registrationRepository = registrationRepository;
    this.topicRepository = topicRepository;
    this.reviewerAssignmentRepository = reviewerAssignmentRepository;
}

    @GetMapping("/login") String login() { return "login"; }
    
@GetMapping({"/", "/dashboard"})
String dashboard(Authentication auth, Model model) {
    model.addAttribute("userCount", userService.countUsers());
    model.addAttribute("departmentCount", departmentService.countDepartments());
    model.addAttribute("periodCount", periodService.countPeriods());
    
    if (auth != null) {
        String role = auth.getAuthorities().iterator().next().getAuthority();
        if (role.contains("FACULTY_MANAGER")) {
            model.addAttribute("pendingRegistrations", registrationRepository.count()); // simplified
        } else if (role.contains("LECTURER")) {
            // Simplified counters
            model.addAttribute("advisingTopics", topicRepository.count());
            model.addAttribute("reviewerAssignments", reviewerAssignmentRepository.count());
        } else if (role.contains("STUDENT")) {
            model.addAttribute("studentGroupStatus", "Đã tham gia nhóm"); // Simplified
        }
    }
    return "dashboard";
}

    @GetMapping("/profile") String profile(Authentication authentication, Model model) { model.addAttribute("user", userService.getByUsername(authentication.getName())); return "profile"; }
    @PostMapping("/profile/password") String changePassword(Authentication authentication, @RequestParam @NotBlank String currentPassword, @RequestParam @Size(min=8) String newPassword, RedirectAttributes redirect) { userService.changePassword(authentication.getName(), currentPassword, newPassword); redirect.addFlashAttribute("success", "Đã đổi mật khẩu"); return "redirect:/profile"; }
    @GetMapping("/error/403") String forbidden() { return "error/403"; }
}
