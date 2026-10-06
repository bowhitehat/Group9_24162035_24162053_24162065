package com.group9.topicmanagement.controller;

import com.group9.topicmanagement.service.DepartmentService;
import com.group9.topicmanagement.service.UserService;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.dto.DepartmentForm;
import com.group9.topicmanagement.dto.UserForm;
import com.group9.topicmanagement.dto.UserUpdateForm;
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
    @PostMapping("/users") String createUser(@Valid @ModelAttribute UserForm userForm, BindingResult errors, Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try {
                users.create(userForm.getUsername(), userForm.getFullName(), userForm.getEmail(), userForm.getStudentCode(), userForm.getDepartmentId(), userForm.getRoles(), userForm.getPassword());
                redirect.addFlashAttribute("successMessage", "Đã tạo tài khoản");
                return "redirect:/admin/users";
            } catch (BusinessRuleException error) { errors.reject("user.invalid", error.getMessage()); }
        }
        userForm.setPassword("");
        model.addAttribute("users", users.search(""));
        model.addAttribute("departments", departments.search(""));
        model.addAttribute("roleNames", users.allRoleNames());
        return "admin/users";
    }
    @PostMapping("/users/{id}/toggle") String toggleUser(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) { users.toggle(id, authentication.getName()); redirect.addFlashAttribute("successMessage","Đã cập nhật trạng thái tài khoản"); return "redirect:/admin/users"; }

    @GetMapping("/users/{id}/edit") String editUser(@PathVariable Long id, Model model) {
        model.addAttribute("userUpdateForm", UserUpdateForm.from(users.get(id)));
        model.addAttribute("userId", id); model.addAttribute("departments", departments.search(""));
        model.addAttribute("roleNames", users.allRoleNames());
        return "admin/user-edit";
    }
    @PostMapping("/users/{id}/edit") String updateUser(@PathVariable Long id,
            @Valid @ModelAttribute UserUpdateForm userUpdateForm, BindingResult errors,
            Authentication authentication, Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try {
                users.update(id, userUpdateForm.getUsername(), userUpdateForm.getFullName(), userUpdateForm.getEmail(),
                        userUpdateForm.getStudentCode(), userUpdateForm.getDepartmentId(), userUpdateForm.getRoles(), authentication.getName());
                redirect.addFlashAttribute("successMessage", "Đã cập nhật thông tin tài khoản");
                return "redirect:/admin/users";
            } catch (BusinessRuleException error) { errors.reject("user.invalid", error.getMessage()); }
        }
        users.get(id); model.addAttribute("userId", id);
        model.addAttribute("departments", departments.search("")); model.addAttribute("roleNames", users.allRoleNames());
        return "admin/user-edit";
    }

    @GetMapping("/departments") String departments(@RequestParam(defaultValue="") String q, Model model) { model.addAttribute("departments", departments.search(q)); model.addAttribute("departmentForm", new DepartmentForm()); model.addAttribute("q", q); return "admin/departments"; }
    @PostMapping("/departments") String createDepartment(@Valid @ModelAttribute DepartmentForm departmentForm, BindingResult errors, Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try {
                departments.create(departmentForm.getCode(), departmentForm.getName(), departmentForm.getDescription());
                redirect.addFlashAttribute("successMessage", "Đã tạo bộ môn");
                return "redirect:/admin/departments";
            } catch (BusinessRuleException error) { errors.reject("department.invalid", error.getMessage()); }
        }
        model.addAttribute("departments", departments.search(""));
        return "admin/departments";
    }
    @PostMapping("/departments/{id}/toggle") String toggleDepartment(@PathVariable Long id, RedirectAttributes redirect) { departments.toggle(id); redirect.addFlashAttribute("successMessage","Đã cập nhật trạng thái bộ môn"); return "redirect:/admin/departments"; }

    @GetMapping("/departments/{id}/edit") String editDepartment(@PathVariable Long id, Model model) {
        var department = departments.get(id);
        DepartmentForm form = new DepartmentForm();
        form.setCode(department.getCode()); form.setName(department.getName()); form.setDescription(department.getDescription());
        model.addAttribute("departmentForm", form); model.addAttribute("departmentId", id);
        return "admin/department-edit";
    }
    @PostMapping("/departments/{id}/edit") String updateDepartment(@PathVariable Long id,
            @Valid @ModelAttribute DepartmentForm departmentForm, BindingResult errors,
            Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try {
                departments.update(id, departmentForm.getCode(), departmentForm.getName(), departmentForm.getDescription());
                redirect.addFlashAttribute("successMessage", "Đã cập nhật bộ môn");
                return "redirect:/admin/departments";
            } catch (BusinessRuleException error) { errors.reject("department.invalid", error.getMessage()); }
        }
        departments.get(id); model.addAttribute("departmentId", id);
        return "admin/department-edit";
    }
}
