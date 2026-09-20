package com.group9.topicmanagement.web;

import com.group9.topicmanagement.service.DepartmentService;
import com.group9.topicmanagement.service.UserService;
import com.group9.topicmanagement.web.form.DepartmentForm;
import com.group9.topicmanagement.web.form.UserForm;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/admin") @PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final UserService users; private final DepartmentService departments;
    public AdminController(UserService users, DepartmentService departments) { this.users=users; this.departments=departments; }

    @GetMapping("/users") String users(@RequestParam(defaultValue="") String q, Model model) { model.addAttribute("users", users.search(q)); model.addAttribute("departments", departments.search("")); model.addAttribute("roleNames", users.allRoleNames()); model.addAttribute("userForm", new UserForm()); model.addAttribute("q", q); return "admin/users"; }
    @PostMapping("/users") String createUser(@Valid @ModelAttribute UserForm userForm, BindingResult errors, Model model, RedirectAttributes redirect) { if(errors.hasErrors()){model.addAttribute("users",users.search(""));model.addAttribute("departments",departments.search(""));model.addAttribute("roleNames",users.allRoleNames());return "admin/users";} users.create(userForm.getUsername(),userForm.getFullName(),userForm.getEmail(),userForm.getStudentCode(),userForm.getDepartmentId(),userForm.getRoles(),userForm.getPassword()); redirect.addFlashAttribute("success","Đã tạo tài khoản"); return "redirect:/admin/users"; }
    @PostMapping("/users/{id}/toggle") String toggleUser(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) { users.toggle(id, authentication.getName()); redirect.addFlashAttribute("success","Đã cập nhật trạng thái tài khoản"); return "redirect:/admin/users"; }

    @GetMapping("/departments") String departments(@RequestParam(defaultValue="") String q, Model model) { model.addAttribute("departments", departments.search(q)); model.addAttribute("departmentForm", new DepartmentForm()); model.addAttribute("q", q); return "admin/departments"; }
    @PostMapping("/departments") String createDepartment(@Valid @ModelAttribute DepartmentForm departmentForm, BindingResult errors, Model model, RedirectAttributes redirect) { if(errors.hasErrors()){model.addAttribute("departments",departments.search(""));return "admin/departments";} departments.create(departmentForm.getCode(),departmentForm.getName(),departmentForm.getDescription()); redirect.addFlashAttribute("success","Đã tạo bộ môn"); return "redirect:/admin/departments"; }
    @PostMapping("/departments/{id}/toggle") String toggleDepartment(@PathVariable Long id, RedirectAttributes redirect) { departments.toggle(id); redirect.addFlashAttribute("success","Đã cập nhật trạng thái bộ môn"); return "redirect:/admin/departments"; }
}
