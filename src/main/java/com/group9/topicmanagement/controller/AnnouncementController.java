package com.group9.topicmanagement.controller;

import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.service.AnnouncementService;
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
@RequestMapping("/announcements")
public class AnnouncementController {
    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    public String listAnnouncements(Authentication auth, Model model) {
        String primaryRole = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        if ("ADMIN".equals(primaryRole) || "FACULTY_MANAGER".equals(primaryRole)) {
            model.addAttribute("announcements", announcementService.listForManager());
            model.addAttribute("managerView", true);
        } else {
            model.addAttribute("announcements", announcementService.getActiveAnnouncementsForRole(primaryRole));
            model.addAttribute("managerView", false);
        }
        return "announcements/list";
    }

    @GetMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY_MANAGER')")
    public String showCreateForm() {
        return "announcements/form";
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY_MANAGER')")
    public String createAnnouncement(@RequestParam String title,
                                     @RequestParam String content,
                                     @RequestParam(required = false) String targetRoles,
                                     @RequestParam(required = false) boolean publish,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expirationTime,
                                     Authentication auth,
                                     RedirectAttributes redirectAttributes) {
        try {
            announcementService.saveAnnouncement(title, content, targetRoles, auth.getName(), publish, expirationTime);
            redirectAttributes.addFlashAttribute("successMessage", "Lưu thông báo thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage() == null ? "Có lỗi xảy ra" : e.getMessage());
            return "redirect:/announcements/create";
        }
        return "redirect:/announcements";
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY_MANAGER')")
    public String publish(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            announcementService.publish(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã công bố thông báo");
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/announcements";
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY_MANAGER')")
    public String archive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        announcementService.archive(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã lưu trữ thông báo");
        return "redirect:/announcements";
    }
}
