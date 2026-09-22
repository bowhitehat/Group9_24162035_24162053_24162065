package com.group9.topicmanagement.web;

import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.service.AnnouncementService;
import com.group9.topicmanagement.web.form.AnnouncementForm;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.stream.Collectors;

@Controller
@RequestMapping("/announcements")
public class AnnouncementController {
    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    public String listAnnouncements(Authentication auth, Model model) {
        var roles = auth.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toSet());
        if (roles.contains("ADMIN") || roles.contains("FACULTY_MANAGER")) {
            var announcements = announcementService.listForManager();
            model.addAttribute("announcements", announcements);
            model.addAttribute("publishableAnnouncementIds", announcements.stream()
                    .filter(announcementService::canPublish).map(a -> a.getId()).collect(Collectors.toSet()));
            model.addAttribute("archivableAnnouncementIds", announcements.stream()
                    .filter(announcementService::canArchive).map(a -> a.getId()).collect(Collectors.toSet()));
            model.addAttribute("managerView", true);
        } else {
            model.addAttribute("announcements", announcementService.getActiveAnnouncementsForRoles(roles));
            model.addAttribute("managerView", false);
            model.addAttribute("publishableAnnouncementIds", java.util.Set.of());
            model.addAttribute("archivableAnnouncementIds", java.util.Set.of());
        }
        return "announcements/list";
    }

    @GetMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY_MANAGER')")
    public String showCreateForm(Model model) {
        model.addAttribute("announcementForm", new AnnouncementForm());
        return "announcements/form";
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY_MANAGER')")
    public String createAnnouncement(@Valid @ModelAttribute("announcementForm") AnnouncementForm form,
                                     BindingResult bindingResult,
                                     Authentication auth,
                                     RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/announcements/create";
        }
        try {
            announcementService.saveAnnouncement(form.getTitle(), form.getContent(),
                    form.getTargetRoles().stream().map(Enum::name).toList(), auth.getName(),
                    form.isPublish(), form.getExpirationTime());
            redirectAttributes.addFlashAttribute("successMessage", "Lưu thông báo thành công");
        } catch (BusinessRuleException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
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
