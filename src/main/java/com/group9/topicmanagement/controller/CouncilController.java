package com.group9.topicmanagement.controller;

import com.group9.topicmanagement.domain.council.Council;
import com.group9.topicmanagement.domain.enums.CouncilMemberRole;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.service.CouncilService;
import com.group9.topicmanagement.service.RegistrationPeriodService;
import com.group9.topicmanagement.service.TopicRegistrationService;
import com.group9.topicmanagement.service.UserService;
import com.group9.topicmanagement.controller.form.CouncilCreateForm;
import com.group9.topicmanagement.controller.form.CouncilMemberForm;
import com.group9.topicmanagement.controller.form.CouncilTopicForm;
import com.group9.topicmanagement.controller.form.CouncilUpdateForm;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

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
    public String listCouncils(org.springframework.security.core.Authentication auth, Model model) {
        boolean isManager = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY_MANAGER"));
        if (isManager) {
            model.addAttribute("councils", councilService.listCouncils());
        } else {
            Long userId = userService.getByUsername(auth.getName()).getId();
            model.addAttribute("councils", councilService.listCouncilsForUser(userId));
        }
        return "councils/list";
    }

    @GetMapping("/create")
    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public String showCreateForm(Model model) {
        model.addAttribute("periods", periodService.findAllPeriods());
        model.addAttribute("councilForm", new CouncilCreateForm());
        return "councils/form";
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public String createCouncil(@Valid @ModelAttribute("councilForm") CouncilCreateForm form,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", firstError(bindingResult));
            return "redirect:/councils/create";
        }
        try {
            Council council = councilService.createCouncil(form.getName(), form.getPeriodId(),
                    form.getReportDate(), form.getLocation());
            redirectAttributes.addFlashAttribute("successMessage", "Tạo hội đồng thành công");
            return "redirect:/councils/" + council.getId();
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/councils/create";
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FACULTY_MANAGER', 'LECTURER')")
    public String viewCouncil(@PathVariable Long id, Authentication auth, Model model) {
        boolean isManager = hasRole(auth, "FACULTY_MANAGER");
        Council council = councilService.getCouncilForViewer(id, auth.getName(), isManager);
        model.addAttribute("council", council);
        model.addAttribute("members", councilService.membersOf(id));
        model.addAttribute("assignments", councilService.assignmentsOf(id));
        model.addAttribute("allLecturers", isManager ? userService.findUsersByRole("LECTURER") : List.of());
        model.addAttribute("memberRoles", CouncilMemberRole.values());
        model.addAttribute("approvedRegistrations", isManager ? registrationService.listApprovedRegistrations().stream()
                .filter(r -> r.getRegistrationPeriod().getId().equals(council.getRegistrationPeriod().getId()))
                .toList() : List.of());
        model.addAttribute("updateForm", toUpdateForm(council));
        model.addAttribute("memberForm", new CouncilMemberForm());
        model.addAttribute("topicForm", new CouncilTopicForm());
        model.addAttribute("canEditCouncil", councilService.canEditCouncil(council, isManager));
        model.addAttribute("canEditStructure", councilService.canEditStructure(council, isManager));
        model.addAttribute("canActivate", councilService.canActivate(council, isManager));
        model.addAttribute("canComplete", councilService.canComplete(council, isManager));
        model.addAttribute("canCancel", councilService.canCancel(council, isManager));
        model.addAttribute("canGrade", councilService.canGradeCouncil(id, auth.getName()));
        return "councils/detail";
    }

    @PostMapping("/{id}/update")
    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public String updateCouncil(@PathVariable Long id,
                                @Valid @ModelAttribute("updateForm") CouncilUpdateForm form,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", firstError(bindingResult));
            return "redirect:/councils/" + id;
        }
        try {
            councilService.updateCouncil(id, form.getName(), form.getReportDate(), form.getLocation());
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hội đồng thành công");
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/councils/" + id;
    }

    @PostMapping("/{id}/add-member")
    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public String addMember(@PathVariable Long id,
                            @Valid @ModelAttribute("memberForm") CouncilMemberForm form,
                            BindingResult bindingResult,
                            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", firstError(bindingResult));
            return "redirect:/councils/" + id;
        }
        try {
            councilService.addMember(id, form.getUserId(), form.getRole());
            redirectAttributes.addFlashAttribute("successMessage", "Thêm thành viên thành công");
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/councils/" + id;
    }

    @PostMapping("/{id}/members/{memberId}/remove")
    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public String removeMember(@PathVariable Long id,
                               @PathVariable Long memberId,
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
                              @Valid @ModelAttribute("topicForm") CouncilTopicForm form,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", firstError(bindingResult));
            return "redirect:/councils/" + id;
        }
        try {
            councilService.assignTopic(id, form.getTopicId());
            redirectAttributes.addFlashAttribute("successMessage", "Gán đề tài thành công");
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/councils/" + id;
    }

    @PostMapping("/{id}/assignments/{assignmentId}/remove")
    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public String removeTopic(@PathVariable Long id,
                              @PathVariable Long assignmentId,
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

    private CouncilUpdateForm toUpdateForm(Council council) {
        CouncilUpdateForm form = new CouncilUpdateForm();
        form.setName(council.getName());
        form.setReportDate(council.getReportDate());
        form.setLocation(council.getLocation());
        return form;
    }

    private String firstError(BindingResult bindingResult) {
        return bindingResult.getAllErrors().isEmpty()
                ? "Thông tin nhập không hợp lệ"
                : bindingResult.getAllErrors().get(0).getDefaultMessage();
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }
}
