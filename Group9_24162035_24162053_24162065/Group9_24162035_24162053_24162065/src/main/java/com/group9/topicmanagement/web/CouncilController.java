package com.group9.topicmanagement.web;

import com.group9.topicmanagement.domain.RegistrationPeriod;
import com.group9.topicmanagement.domain.User;
import com.group9.topicmanagement.domain.council.Council;
import com.group9.topicmanagement.domain.council.CouncilAssignment;
import com.group9.topicmanagement.domain.council.CouncilMember;
import com.group9.topicmanagement.domain.enums.CouncilMemberRole;
import com.group9.topicmanagement.domain.enums.RoleName;
import com.group9.topicmanagement.domain.registration.TopicRegistration;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.repository.*;
import com.group9.topicmanagement.service.CouncilService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/councils")
public class CouncilController {

    private final CouncilService councilService;
    private final CouncilRepository councilRepository;
    private final CouncilMemberRepository memberRepository;
    private final CouncilAssignmentRepository assignmentRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final UserRepository userRepository;
    private final TopicRegistrationRepository registrationRepository;

    public CouncilController(CouncilService councilService, CouncilRepository councilRepository,
                             CouncilMemberRepository memberRepository, CouncilAssignmentRepository assignmentRepository,
                             RegistrationPeriodRepository periodRepository, UserRepository userRepository,
                             TopicRegistrationRepository registrationRepository) {
        this.councilService = councilService;
        this.councilRepository = councilRepository;
        this.memberRepository = memberRepository;
        this.assignmentRepository = assignmentRepository;
        this.periodRepository = periodRepository;
        this.userRepository = userRepository;
        this.registrationRepository = registrationRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FACULTY_MANAGER', 'LECTURER')")
    public String listCouncils(Model model) {
        model.addAttribute("councils", councilRepository.findAll());
        return "councils/list";
    }

    @GetMapping("/create")
    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public String showCreateForm(Model model) {
        model.addAttribute("periods", periodRepository.findAll());
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
        Council council = councilRepository.findById(id).orElseThrow();
        List<CouncilMember> members = memberRepository.findByCouncilId(id);
        List<CouncilAssignment> assignments = assignmentRepository.findByCouncilId(id);
        
        List<User> allLecturers = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName() == RoleName.LECTURER))
                .toList();

        List<TopicRegistration> approvedRegistrations = registrationRepository.findAll().stream()
                .filter(r -> "APPROVED".equals(r.getStatus().name()) && r.getRegistrationPeriod().getId().equals(council.getRegistrationPeriod().getId()))
                .toList();

        model.addAttribute("council", council);
        model.addAttribute("members", members);
        model.addAttribute("assignments", assignments);
        model.addAttribute("allLecturers", allLecturers);
        model.addAttribute("memberRoles", CouncilMemberRole.values());
        model.addAttribute("approvedRegistrations", approvedRegistrations);
        
        return "councils/detail";
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
}
