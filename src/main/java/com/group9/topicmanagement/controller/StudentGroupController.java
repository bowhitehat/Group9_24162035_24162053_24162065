package com.group9.topicmanagement.controller;

import com.group9.topicmanagement.domain.RegistrationPeriod;
import com.group9.topicmanagement.domain.User;
import com.group9.topicmanagement.domain.studentgroup.StudentGroup;
import com.group9.topicmanagement.service.RegistrationPeriodService;
import com.group9.topicmanagement.service.StudentGroupService;
import com.group9.topicmanagement.service.UserService;
import com.group9.topicmanagement.controller.form.GroupMemberForm;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/groups")
@PreAuthorize("hasRole('STUDENT')")
public class StudentGroupController {

    private final StudentGroupService groupService;
    private final RegistrationPeriodService periodService;
    private final UserService userService;

    public StudentGroupController(StudentGroupService groupService,
                                  RegistrationPeriodService periodService,
                                  UserService userService) {
        this.groupService = groupService;
        this.periodService = periodService;
        this.userService = userService;
    }

    @GetMapping("/my-group")
    public String myGroup(@RequestParam(required = false) Long periodId, Model model, Principal principal) {
        User currentUser = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sinh viên"));

        List<RegistrationPeriod> periods = periodService.findAllPeriods();
        if (periodId == null && !periods.isEmpty()) {
            periodId = periods.get(0).getId();
        }

        Optional<StudentGroup> groupOpt = (periodId != null) 
                ? groupService.findStudentGroupByStudentAndPeriod(currentUser.getId(), periodId) 
                : Optional.empty();

        model.addAttribute("periods", periods);
        model.addAttribute("selectedPeriodId", periodId);
        StudentGroup group = groupOpt.orElse(null);
        model.addAttribute("group", group);
        model.addAttribute("canCreateGroup", group == null && groupService.canCreateGroup(periodId, principal.getName()));
        model.addAttribute("canAddMember", group != null && groupService.canAddMember(group, principal.getName()));
        model.addAttribute("removableMemberIds", group == null ? java.util.Set.of() : groupService.removableMemberIds(group, principal.getName()));
        model.addAttribute("groupMemberForm", new GroupMemberForm());

        return "groups/my-group";
    }

    @PostMapping("/create")
    public String createGroup(@RequestParam Long periodId, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            StudentGroup group = groupService.createGroup(periodId, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Tạo nhóm sinh viên thành công!");
            return "redirect:/groups/my-group?periodId=" + periodId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/groups/my-group?periodId=" + periodId;
        }
    }

    @PostMapping("/add-member")
    public String addMember(@RequestParam Long groupId,
                            @RequestParam String memberUsername,
                            Principal principal,
                            RedirectAttributes redirectAttributes) {
        try {
            groupService.addMember(groupId, memberUsername, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Thêm thành viên thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        StudentGroup group = groupService.getGroupById(groupId);
        return "redirect:/groups/my-group?periodId=" + group.getRegistrationPeriod().getId();
    }

    @PostMapping("/remove-member")
    public String removeMember(@RequestParam Long groupId,
                               @RequestParam Long memberId,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        try {
            groupService.removeMember(groupId, memberId, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa thành viên khỏi nhóm!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        StudentGroup group = groupService.getGroupById(groupId);
        return "redirect:/groups/my-group?periodId=" + group.getRegistrationPeriod().getId();
    }
}
