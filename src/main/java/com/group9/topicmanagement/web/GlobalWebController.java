package com.group9.topicmanagement.web;

import com.group9.topicmanagement.repository.DepartmentRepository;
import com.group9.topicmanagement.repository.RegistrationPeriodRepository;
import com.group9.topicmanagement.repository.UserRepository;
import com.group9.topicmanagement.service.UserService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class GlobalWebController {
    private final UserRepository users; private final DepartmentRepository departments; private final RegistrationPeriodRepository periods; private final UserService userService;
    public GlobalWebController(UserRepository users, DepartmentRepository departments, RegistrationPeriodRepository periods, UserService userService) { this.users=users; this.departments=departments; this.periods=periods; this.userService=userService; }
    @GetMapping("/login") String login() { return "login"; }
    @GetMapping({"/", "/dashboard"}) String dashboard(Model model) { model.addAttribute("userCount", users.count()); model.addAttribute("departmentCount", departments.count()); model.addAttribute("periodCount", periods.count()); return "dashboard"; }
    @GetMapping("/profile") String profile(Authentication authentication, Model model) { model.addAttribute("user", userService.getByUsername(authentication.getName())); return "profile"; }
    @PostMapping("/profile/password") String changePassword(Authentication authentication, @RequestParam @NotBlank String currentPassword, @RequestParam @Size(min=8) String newPassword, RedirectAttributes redirect) { userService.changePassword(authentication.getName(), currentPassword, newPassword); redirect.addFlashAttribute("success", "Đã đổi mật khẩu"); return "redirect:/profile"; }
    @GetMapping("/error/403") String forbidden() { return "error/403"; }
}
