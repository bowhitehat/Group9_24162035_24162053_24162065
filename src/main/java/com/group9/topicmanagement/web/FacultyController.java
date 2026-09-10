package com.group9.topicmanagement.web;

import com.group9.topicmanagement.domain.enums.PeriodStatus;
import com.group9.topicmanagement.domain.enums.PeriodType;
import com.group9.topicmanagement.service.RegistrationPeriodService;
import com.group9.topicmanagement.web.form.PeriodForm;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/faculty/periods") @PreAuthorize("hasAnyRole('FACULTY_MANAGER','ADMIN')")
public class FacultyController {
    private final RegistrationPeriodService periods;
    public FacultyController(RegistrationPeriodService periods) { this.periods=periods; }
    @GetMapping String list(@RequestParam(defaultValue="") String q, @RequestParam(required=false) PeriodType type, @RequestParam(required=false) PeriodStatus status, Model model) { model.addAttribute("periods",periods.search(q,type,status)); model.addAttribute("types",PeriodType.values()); model.addAttribute("statuses",PeriodStatus.values()); model.addAttribute("periodForm",new PeriodForm()); model.addAttribute("q",q); return "faculty/periods"; }
    @PostMapping String create(@Valid @ModelAttribute PeriodForm periodForm, BindingResult errors, Model model, RedirectAttributes redirect) { if(errors.hasErrors()){model.addAttribute("periods",periods.search("",null,null));model.addAttribute("types",PeriodType.values());model.addAttribute("statuses",PeriodStatus.values());return "faculty/periods";} periods.save(periodForm.toEntity()); redirect.addFlashAttribute("success","Đã tạo đợt đăng ký"); return "redirect:/faculty/periods"; }
    @GetMapping("/{id}") String detail(@PathVariable Long id, Model model) { model.addAttribute("period",periods.get(id)); model.addAttribute("statuses",PeriodStatus.values()); return "faculty/period-detail"; }
    @GetMapping("/{id}/edit") String edit(@PathVariable Long id, Model model) { model.addAttribute("period",periods.get(id)); model.addAttribute("periodForm",PeriodForm.from(periods.get(id))); model.addAttribute("types",PeriodType.values()); return "faculty/period-edit"; }
    @PostMapping("/{id}/edit") String update(@PathVariable Long id, @Valid @ModelAttribute PeriodForm periodForm, BindingResult errors, Model model, RedirectAttributes redirect) { if(errors.hasErrors()){model.addAttribute("period",periods.get(id));model.addAttribute("types",PeriodType.values());return "faculty/period-edit";} periods.update(id,periodForm.toEntity()); redirect.addFlashAttribute("success","Đã cập nhật đợt"); return "redirect:/faculty/periods/"+id; }
    @PostMapping("/{id}/status") String transition(@PathVariable Long id, @RequestParam PeriodStatus status, RedirectAttributes redirect) { periods.transition(id,status); redirect.addFlashAttribute("success","Đã chuyển trạng thái"); return "redirect:/faculty/periods/"+id; }
}
