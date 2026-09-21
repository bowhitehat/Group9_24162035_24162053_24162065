package com.group9.topicmanagement.web;

import com.group9.topicmanagement.domain.enums.EvaluationType;
import com.group9.topicmanagement.domain.evaluation.Evaluation;
import com.group9.topicmanagement.domain.evaluation.EvaluationCriterion;
import com.group9.topicmanagement.domain.evaluation.EvaluationScore;
import com.group9.topicmanagement.domain.evaluation.TopicResult;
import com.group9.topicmanagement.domain.topic.Topic;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.repository.EvaluationCriterionRepository;
import com.group9.topicmanagement.repository.EvaluationScoreRepository;
import com.group9.topicmanagement.service.EvaluationService;
import com.group9.topicmanagement.service.TopicResultService;
import com.group9.topicmanagement.service.TopicService;
import com.group9.topicmanagement.service.UserService;
import org.springframework.security.access.AccessDeniedException;
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
    private final EvaluationCriterionRepository criterionRepository;
    private final TopicService topicService;
    private final UserService userService;
    private final EvaluationScoreRepository scoreRepository;

    public EvaluationController(EvaluationService evaluationService,
                                TopicResultService topicResultService,
                                EvaluationCriterionRepository criterionRepository,
                                TopicService topicService,
                                UserService userService,
                                EvaluationScoreRepository scoreRepository) {
        this.evaluationService = evaluationService;
        this.topicResultService = topicResultService;
        this.criterionRepository = criterionRepository;
        this.topicService = topicService;
        this.userService = userService;
        this.scoreRepository = scoreRepository;
    }

    @GetMapping("/topic/{topicId}")
    @PreAuthorize("hasAnyRole('LECTURER', 'FACULTY_MANAGER')")
    public String showEvaluationForm(@PathVariable Long topicId,
                                     @RequestParam String type,
                                     Authentication auth,
                                     Model model) {
        Topic topic = topicService.getTopicById(topicId);
        EvaluationType evaluationType = EvaluationType.valueOf(type);
        Long evaluatorId = userService.getByUsername(auth.getName()).getId();
        Evaluation evaluation = evaluationService.getOrEmpty(topicId, evaluatorId, evaluationType);
        List<EvaluationScore> scores = evaluation.getId() == null
                ? new ArrayList<>()
                : scoreRepository.findByEvaluationId(evaluation.getId());
        model.addAttribute("topic", topic);
        model.addAttribute("criteria", criterionRepository
                .findByRegistrationPeriodIdAndIsActiveTrueOrderByDisplayOrderAsc(topic.getRegistrationPeriod().getId()));
        model.addAttribute("evaluation", evaluation.getId() == null ? new Evaluation() : evaluation);
        model.addAttribute("existingScores", scores);
        model.addAttribute("evalType", evaluationType.name());
        return "evaluations/form";
    }

    @PostMapping("/topic/{topicId}")
    @PreAuthorize("hasRole('LECTURER')")
    public String saveEvaluation(@PathVariable Long topicId,
                                 @RequestParam String type,
                                 @RequestParam String comments,
                                 @RequestParam Map<String, String> allParams,
                                 @RequestParam(required = false) boolean submitAction,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        try {
            EvaluationType evaluationType = EvaluationType.valueOf(type);
            List<EvaluationScore> scores = new ArrayList<>();
            for (Map.Entry<String, String> entry : allParams.entrySet()) {
                if (entry.getKey().startsWith("score_") && entry.getValue() != null && !entry.getValue().isBlank()) {
                    Long criterionId = Long.parseLong(entry.getKey().substring(6));
                    EvaluationScore score = new EvaluationScore();
                    EvaluationCriterion crit = new EvaluationCriterion();
                    crit.setId(criterionId);
                    score.setCriterion(crit);
                    score.setScore(new BigDecimal(entry.getValue()));
                    scores.add(score);
                }
            }
            Evaluation eval = evaluationService.saveDraft(topicId, auth.getName(), evaluationType, scores, comments);
            if (submitAction) {
                evaluationService.submitEvaluation(eval.getId());
                redirectAttributes.addFlashAttribute("successMessage", "Nộp điểm thành công!");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Lưu nháp thành công!");
            }
        } catch (BusinessRuleException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/evaluations/topic/" + topicId + "?type=" + type;
    }

    @GetMapping("/results")
    @PreAuthorize("hasAnyRole('FACULTY_MANAGER', 'LECTURER')")
    public String listResults(Model model) {
        model.addAttribute("results", topicResultService.listResults());
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
    @PreAuthorize("hasAnyRole('FACULTY_MANAGER', 'LECTURER')")
    public String calculateResult(@PathVariable Long topicId, RedirectAttributes redirectAttributes) {
        try {
            topicResultService.calculateResult(topicId);
            redirectAttributes.addFlashAttribute("successMessage", "Tính điểm tổng kết thành công");
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/evaluations/results";
    }

    @PostMapping("/topic/{topicId}/confirm")
    @PreAuthorize("hasRole('LECTURER')")
    public String confirmResult(@PathVariable Long topicId, Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            topicResultService.confirmResult(topicId, auth.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Xác nhận kết quả thành công");
        } catch (BusinessRuleException | AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
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
        try {
            evaluationService.lockEvaluation(evaluationId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã khóa phiếu chấm");
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/evaluations/results";
    }
}
