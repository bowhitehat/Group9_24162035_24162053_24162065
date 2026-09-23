package com.group9.topicmanagement.controller;

import com.group9.topicmanagement.domain.enums.EvaluationType;
import com.group9.topicmanagement.domain.evaluation.Evaluation;
import com.group9.topicmanagement.domain.evaluation.EvaluationCriterion;
import com.group9.topicmanagement.domain.evaluation.EvaluationScore;
import com.group9.topicmanagement.domain.evaluation.TopicResult;
import com.group9.topicmanagement.domain.topic.Topic;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.service.EvaluationService;
import com.group9.topicmanagement.service.TopicResultService;
import com.group9.topicmanagement.service.TopicService;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/evaluations")
public class EvaluationController {
    private final EvaluationService evaluationService;
    private final TopicResultService topicResultService;
    private final TopicService topicService;

    public EvaluationController(EvaluationService evaluationService,
                                TopicResultService topicResultService,
                                TopicService topicService) {
        this.evaluationService = evaluationService;
        this.topicResultService = topicResultService;
        this.topicService = topicService;
    }

    @GetMapping("/topic/{topicId}")
    @PreAuthorize("hasAnyRole('LECTURER', 'FACULTY_MANAGER')")
    public String showEvaluationForm(@PathVariable Long topicId,
                                     @RequestParam String type,
                                     Authentication auth,
                                     Model model) {
        EvaluationType evaluationType = EvaluationType.valueOf(type);
        Evaluation evaluation = evaluationService.getForForm(topicId, auth.getName(), evaluationType);
        Topic topic = topicService.getTopicById(topicId);
        List<EvaluationScore> scores = evaluation.getId() == null
                ? new ArrayList<>()
                : evaluationService.getScores(evaluation.getId());
        model.addAttribute("topic", topic);
        model.addAttribute("criteria", evaluationService.getActiveCriteria(topic.getRegistrationPeriod().getId()));
        model.addAttribute("evaluation", evaluation.getId() == null ? new Evaluation() : evaluation);
        model.addAttribute("existingScores", scores);
        model.addAttribute("evalType", evaluationType.name());
        
        com.group9.topicmanagement.controller.form.EvaluationSubmitDto form = new com.group9.topicmanagement.controller.form.EvaluationSubmitDto();
        form.setType(evaluationType);
        form.setComments(evaluation.getComments());
        Map<Long, BigDecimal> scoreMap = new java.util.HashMap<>();
        for (EvaluationScore s : scores) {
            scoreMap.put(s.getCriterion().getId(), s.getScore());
        }
        form.setScores(scoreMap);
        model.addAttribute("formDto", form);
        
        model.addAttribute("canEdit", evaluationService.canEdit(topicId, auth.getName(), evaluationType, evaluation));
        return "evaluations/form";
    }

    @PostMapping("/topic/{topicId}")
    @PreAuthorize("hasRole('LECTURER')")
    public String saveEvaluation(@PathVariable Long topicId,
                                 @jakarta.validation.Valid @org.springframework.web.bind.annotation.ModelAttribute("formDto") com.group9.topicmanagement.controller.form.EvaluationSubmitDto formDto,
                                 org.springframework.validation.BindingResult bindingResult,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Thông tin nhập không hợp lệ");
            String type = formDto.getType() == null ? EvaluationType.REVIEWER.name() : formDto.getType().name();
            return "redirect:/evaluations/topic/" + topicId + "?type=" + type;
        }
        try {
            List<EvaluationScore> scores = new ArrayList<>();
            if (formDto.getScores() != null) {
                for (Map.Entry<Long, BigDecimal> entry : formDto.getScores().entrySet()) {
                    if (entry.getValue() != null) {
                        EvaluationScore score = new EvaluationScore();
                        EvaluationCriterion crit = new EvaluationCriterion();
                        crit.setId(entry.getKey());
                        score.setCriterion(crit);
                        score.setScore(entry.getValue());
                        scores.add(score);
                    }
                }
            }
            Evaluation eval = evaluationService.saveDraft(topicId, auth.getName(), formDto.getType(), scores, formDto.getComments());
            if (formDto.isSubmitAction()) {
                evaluationService.submitEvaluation(eval.getId());
                redirectAttributes.addFlashAttribute("successMessage", "Nộp điểm thành công!");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Lưu nháp thành công!");
            }
        } catch (BusinessRuleException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/evaluations/topic/" + topicId + "?type=" + formDto.getType().name();
    }

    @GetMapping("/results")
    @PreAuthorize("hasAnyRole('FACULTY_MANAGER', 'LECTURER')")
    public String listResults(Model model, Authentication auth) {
        List<TopicResult> results = topicResultService.listResults(auth);
        model.addAttribute("results", results);
        model.addAttribute("calculableTopicIds", results.stream()
                .filter(result -> topicResultService.canCalculate(result.getTopic().getId(), auth.getName()))
                .map(result -> result.getTopic().getId())
                .collect(java.util.stream.Collectors.toSet()));
        return "evaluations/results";
    }

    @GetMapping("/my-result")
    @PreAuthorize("hasRole('STUDENT')")
    public String myResult(@RequestParam Long topicId, Authentication auth, Model model) {
        TopicResult result = topicResultService.getPublishedResultForStudent(topicId, auth.getName());
        model.addAttribute("result", result);
        model.addAttribute("evaluations", evaluationService.listByTopic(topicId));
        return "evaluations/student-result";
    }

    @PostMapping("/topic/{topicId}/calculate")
    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public String calculateResult(@PathVariable Long topicId, RedirectAttributes redirectAttributes) {
        try {
            topicResultService.calculateResult(topicId);
            redirectAttributes.addFlashAttribute("successMessage", "Tính điểm tổng kết thành công");
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/evaluations/topic/" + topicId + "/results";
    }

    @PostMapping("/topic/{topicId}/confirm")
    @PreAuthorize("hasRole('LECTURER')")
    public String confirmResult(@PathVariable Long topicId, Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            topicResultService.confirmResult(topicId, auth.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Xác nhận kết quả thành công");
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        // Let AccessDeniedException bubble up so Spring Security handles it (HTTP 403)
        return "redirect:/evaluations/results";
    }

    @PostMapping("/topic/{topicId}/publish")
    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public String publishResult(@PathVariable Long topicId, Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            topicResultService.publishResult(topicId, auth.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Công bố kết quả thành công");
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/evaluations/results";
    }

    @PostMapping("/{evaluationId}/lock")
    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public String lockEvaluation(@PathVariable Long evaluationId, RedirectAttributes redirectAttributes) {
        Long topicId = null;
        try {
            topicId = evaluationService.lockEvaluation(evaluationId).getTopic().getId();
            redirectAttributes.addFlashAttribute("successMessage", "Đã khóa phiếu chấm");
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return topicId == null ? "redirect:/evaluations/results"
                : "redirect:/evaluations/topic/" + topicId + "/results";
    }

    @GetMapping("/topic/{topicId}/results")
    @PreAuthorize("hasAnyRole('FACULTY_MANAGER', 'LECTURER')")
    public String viewResultDetail(@PathVariable Long topicId, Authentication auth, Model model) {
        TopicResult result = topicResultService.getResultForDetail(topicId, auth);
        List<Evaluation> evaluations = evaluationService.listByTopic(topicId);
        model.addAttribute("result", result);
        model.addAttribute("evaluations", evaluations);
        
        model.addAttribute("canConfirm", topicResultService.canConfirm(topicId, auth.getName()));
        model.addAttribute("canPublish", topicResultService.canPublish(topicId, auth.getName()));
        model.addAttribute("canCalculate", topicResultService.canCalculate(topicId, auth.getName()));
        boolean isManager = auth.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_FACULTY_MANAGER"));
        model.addAttribute("lockableEvaluationIds", isManager ? evaluations.stream()
                .filter(e -> e.getStatus() == com.group9.topicmanagement.domain.enums.EvaluationStatus.SUBMITTED)
                .map(Evaluation::getId)
                .collect(java.util.stream.Collectors.toSet()) : java.util.Set.of());
        
        return "evaluations/result-detail";
    }
}
