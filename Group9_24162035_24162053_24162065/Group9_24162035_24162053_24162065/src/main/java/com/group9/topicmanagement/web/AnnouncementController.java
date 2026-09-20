package com.group9.topicmanagement.web;

import com.group9.topicmanagement.domain.announcement.Announcement;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.repository.AnnouncementRepository;
import com.group9.topicmanagement.service.AnnouncementService;
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
@RequestMapping("/announcements")
public class AnnouncementController {
    private final AnnouncementService announcementService;
    private final AnnouncementRepository announcementRepository;

    public AnnouncementController(AnnouncementService announcementService, AnnouncementRepository announcementRepository) {
        this.announcementService = announcementService;
        this.announcementRepository = announcementRepository;
    }

    @GetMapping
    public String listAnnouncements(Authentication auth, Model model) {
        // Find role to show appropriate announcements
        String primaryRole = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        if (primaryRole.equals("ADMIN") || primaryRole.equals("FACULTY_MANAGER")) {
            model.addAttribute("announcements", announcementRepository.findAll());
        } else {
            model.addAttribute("announcements", announcementService.getActiveAnnouncementsForRole(primaryRole));
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
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra");
            return "redirect:/announcements/create";
        }
        return "redirect:/announcements";
    }
}
