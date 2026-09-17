package com.group9.topicmanagement.web;

import com.group9.topicmanagement.domain.Department;
import com.group9.topicmanagement.domain.RegistrationPeriod;
import com.group9.topicmanagement.domain.topic.Topic;
import com.group9.topicmanagement.domain.topic.TopicStatus;
import com.group9.topicmanagement.domain.User;
import com.group9.topicmanagement.service.DepartmentService;
import com.group9.topicmanagement.service.RegistrationPeriodService;
import com.group9.topicmanagement.service.TopicService;
import com.group9.topicmanagement.service.UserService;
import com.group9.topicmanagement.web.form.RejectForm;
import com.group9.topicmanagement.web.form.TopicForm;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/topics")
public class TopicController {

    private final TopicService topicService;
    private final RegistrationPeriodService periodService;
    private final DepartmentService departmentService;
    private final UserService userService;

    public TopicController(TopicService topicService,
                           RegistrationPeriodService periodService,
                           DepartmentService departmentService,
                           UserService userService) {
        this.topicService = topicService;
        this.periodService = periodService;
        this.departmentService = departmentService;
        this.userService = userService;
    }

    @GetMapping
    public String listTopics(@RequestParam(required = false) Long periodId,
                             @RequestParam(required = false) Long departmentId,
                             @RequestParam(required = false) TopicStatus status,
                             @RequestParam(required = false) String keyword,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             Model model) {
        Pageable pageable = PageRequest.of(page, size);
        
        List<RegistrationPeriod> periods = periodService.findAllPeriods();
        if (periodId == null && !periods.isEmpty()) {
            periodId = periods.get(0).getId();
        }

        Page<Topic> topicPage = (periodId != null) 
                ? topicService.findTopicsWithFilters(periodId, departmentId, status, keyword, pageable)
                : Page.empty();

        model.addAttribute("topics", topicPage.getContent());
        model.addAttribute("page", topicPage);
        model.addAttribute("periods", periods);
        model.addAttribute("departments", departmentService.findAllDepartments());
        model.addAttribute("statuses", TopicStatus.values());
        model.addAttribute("selectedPeriodId", periodId);
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("keyword", keyword);

        return "topics/list";
    }

    @GetMapping("/detail/{id}")
    public String topicDetail(@PathVariable Long id, Model model) {
        Topic topic = topicService.getTopicById(id);
        model.addAttribute("topic", topic);
        model.addAttribute("rejectForm", new RejectForm());
        return "topics/detail";
    }

    @PreAuthorize("hasRole('LECTURER')")
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        TopicForm form = new TopicForm();
        model.addAttribute("topicForm", form);
        model.addAttribute("periods", periodService.findAllPeriods());
        model.addAttribute("departments", departmentService.findAllDepartments());
        model.addAttribute("lecturers", userService.findUsersByRole("LECTURER"));
        return "topics/form";
    }

    @PreAuthorize("hasRole('LECTURER')")
    @PostMapping("/create")
    public String createTopic(@Valid @ModelAttribute("topicForm") TopicForm form,
                              BindingResult bindingResult,
                              Principal principal,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("periods", periodService.findAllPeriods());
            model.addAttribute("departments", departmentService.findAllDepartments());
            model.addAttribute("lecturers", userService.findUsersByRole("LECTURER"));
            return "topics/form";
        }

        try {
            topicService.createTopic(form, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đề xuất đề tài thành công!");
            return "redirect:/topics";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("periods", periodService.findAllPeriods());
            model.addAttribute("departments", departmentService.findAllDepartments());
            model.addAttribute("lecturers", userService.findUsersByRole("LECTURER"));
            return "topics/form";
        }
    }

    @PreAuthorize("hasRole('LECTURER')")
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, Principal principal) {
        Topic topic = topicService.getTopicById(id);
        TopicForm form = new TopicForm();
        form.setId(topic.getId());
        form.setCode(topic.getCode());
        form.setTitle(topic.getTitle());
        form.setDescription(topic.getDescription());
        form.setRequirement(topic.getRequirement());
        form.setDepartmentId(topic.getDepartment().getId());
        form.setRegistrationPeriodId(topic.getRegistrationPeriod().getId());
        form.setAdvisorIds(topic.getAdvisors().stream().map(User::getId).toList());

        model.addAttribute("topicForm", form);
        model.addAttribute("periods", periodService.findAllPeriods());
        model.addAttribute("departments", departmentService.findAllDepartments());
        model.addAttribute("lecturers", userService.findUsersByRole("LECTURER"));
        return "topics/form";
    }

    @PreAuthorize("hasRole('LECTURER')")
    @PostMapping("/edit/{id}")
    public String updateTopic(@PathVariable Long id,
                              @Valid @ModelAttribute("topicForm") TopicForm form,
                              BindingResult bindingResult,
                              Principal principal,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("periods", periodService.findAllPeriods());
            model.addAttribute("departments", departmentService.findAllDepartments());
            model.addAttribute("lecturers", userService.findUsersByRole("LECTURER"));
            return "topics/form";
        }

        try {
            topicService.updateTopic(id, form, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật đề tài thành công!");
            return "redirect:/topics/detail/" + id;
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("periods", periodService.findAllPeriods());
            model.addAttribute("departments", departmentService.findAllDepartments());
            model.addAttribute("lecturers", userService.findUsersByRole("LECTURER"));
            return "topics/form";
        }
    }

    @PreAuthorize("hasRole('LECTURER')")
    @PostMapping("/submit/{id}")
    public String submitTopic(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            topicService.submitTopic(id, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi đề tài để chờ duyệt!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/topics/detail/" + id;
    }

    @PreAuthorize("hasAnyRole('FACULTY_MANAGER', 'ADMIN')")
    @PostMapping("/approve/{id}")
    public String approveTopic(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            topicService.approveTopic(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã duyệt đề tài thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/topics/detail/" + id;
    }

    @PreAuthorize("hasAnyRole('FACULTY_MANAGER', 'ADMIN')")
    @PostMapping("/reject/{id}")
    public String rejectTopic(@PathVariable Long id, @ModelAttribute RejectForm rejectForm, RedirectAttributes redirectAttributes) {
        try {
            topicService.rejectTopic(id, rejectForm.getReason());
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối đề tài!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/topics/detail/" + id;
    }

    @PreAuthorize("hasAnyRole('FACULTY_MANAGER', 'ADMIN')")
    @PostMapping("/publish/{id}")
    public String publishTopic(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            topicService.publishTopic(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã công bố đề tài thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/topics/detail/" + id;
    }
}
