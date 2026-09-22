package com.group9.topicmanagement.controller;

import com.group9.topicmanagement.domain.council.Council;
import com.group9.topicmanagement.domain.enums.CouncilMemberRole;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.service.CouncilService;
import com.group9.topicmanagement.service.RegistrationPeriodService;
import com.group9.topicmanagement.service.TopicRegistrationService;
import com.group9.topicmanagement.service.UserService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/councils")
public class CouncilController {
    private final CouncilService councilService;
    private final RegistrationPeriodService periodService;
    private final UserService userService;
    private final TopicRegistrationService registrationService;

    public CouncilController(CouncilService councilService,
                             RegistrationPeriodService periodService,
                             UserService userService,
                             TopicRegistrationService registrationService) {
        this.councilService = councilService;
        this.periodService = periodService;
        this.userService = userService;
        this.registrationService = registrationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FACULTY_MANAGER', 'LECTURER')")
    public String listCouncils(Model model) {
        model.addAttribute("councils", councilService.listCouncils());
        return "councils/list";
    }

    @GetMapping("/create")
    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public String showCreateForm(Model model) {
        model.addAttribute("periods", periodService.findAllPeriods());
        return "councils/form";
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public String createCouncil(@RequestParam String name,
                                @RequestParam Long periodId,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime reportDate,
                                @RequestParam String location,
                                RedirectAttributes redirectAttributes) {
        try {
            Council council = councilService.createCouncil(name, periodId, reportDate, location);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo hội đồng thành công");
            return "redirect:/councils/" + council.getId();
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/councils/create";
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FACULTY_MANAGER', 'LECTURER')")
    public String viewCouncil(@PathVariable Long id, Model model) {
        Council council = councilService.getCouncil(id);
        model.addAttribute("council", council);
        model.addAttribute("members", councilService.membersOf(id));
        model.addAttribute("assignments", councilService.assignmentsOf(id));
        model.addAttribute("allLecturers", userService.findUsersByRole("LECTURER"));
        model.addAttribute("memberRoles", CouncilMemberRole.values());
        model.addAttribute("approvedRegistrations", registrationService.listApprovedRegistrations().stream()
                .filter(r -> r.getRegistrationPeriod().getId().equals(council.getRegistrationPeriod().getId()))
                .toList());
        return "councils/detail";
    }

    @PostMapping("/{id}/update")
    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public String updateCouncil(@PathVariable Long id,
                                @RequestParam String name,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime reportDate,
                                @RequestParam String location,
                                RedirectAttributes redirectAttributes) {
        try {
            councilService.updateCouncil(id, name, reportDate, location);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hội đồng thành công");
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/councils/" + id;
    }

    @PostMapping("/{id}/add-member")
    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public String addMember(@PathVariable Long id,
                            @RequestParam Long userId,
                            @RequestParam CouncilMemberRole role,
                            RedirectAttributes redirectAttributes) {
        try {
            councilService.addMember(id, userId, role);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm thành viên thành công");
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/councils/" + id;
    }

    @PostMapping("/{id}/remove-member")
    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public String removeMember(@PathVariable Long id,
                               @RequestParam Long memberId,
                               RedirectAttributes redirectAttributes) {
        try {
            councilService.removeMember(id, memberId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa thành viên");
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/councils/" + id;
    }

    @PostMapping("/{id}/assign-topic")
    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public String assignTopic(@PathVariable Long id,
                              @RequestParam Long topicId,
                              RedirectAttributes redirectAttributes) {
        try {
            councilService.assignTopic(id, topicId);
            redirectAttributes.addFlashAttribute("successMessage", "Gán đề tài thành công");
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/councils/" + id;
    }

    @PostMapping("/{id}/remove-topic")
    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public String removeTopic(@PathVariable Long id,
                              @RequestParam Long assignmentId,
                              RedirectAttributes redirectAttributes) {
        try {
            councilService.removeTopic(id, assignmentId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã bỏ đề tài khỏi hội đồng");
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/councils/" + id;
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public String activateCouncil(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            councilService.activateCouncil(id);
            redirectAttributes.addFlashAttribute("successMessage", "Hội đồng đã được kích hoạt");
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/councils/" + id;
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public String completeCouncil(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            councilService.completeCouncil(id);
            redirectAttributes.addFlashAttribute("successMessage", "Hội đồng đã hoàn tất");
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/councils/" + id;
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public String cancelCouncil(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            councilService.cancelCouncil(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy hội đồng");
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/councils/" + id;
    }
}
