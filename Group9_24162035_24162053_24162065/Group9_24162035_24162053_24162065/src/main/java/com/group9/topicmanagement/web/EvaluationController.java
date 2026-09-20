package com.group9.topicmanagement.web;

import com.group9.topicmanagement.domain.evaluation.Evaluation;
import com.group9.topicmanagement.domain.evaluation.EvaluationCriterion;
import com.group9.topicmanagement.domain.evaluation.EvaluationScore;
import com.group9.topicmanagement.domain.topic.Topic;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.repository.*;
import com.group9.topicmanagement.service.EvaluationService;
import com.group9.topicmanagement.service.TopicResultService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
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
    private final EvaluationRepository evaluationRepository;
    private final EvaluationCriterionRepository criterionRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final EvaluationScoreRepository scoreRepository;
    private final TopicResultRepository topicResultRepository;

    public EvaluationController(EvaluationService evaluationService, TopicResultService topicResultService,
                                EvaluationRepository evaluationRepository, EvaluationCriterionRepository criterionRepository,
                                TopicRepository topicRepository, UserRepository userRepository,
                                EvaluationScoreRepository scoreRepository, TopicResultRepository topicResultRepository) {
        this.evaluationService = evaluationService;
        this.topicResultService = topicResultService;
        this.evaluationRepository = evaluationRepository;
        this.criterionRepository = criterionRepository;
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
        this.scoreRepository = scoreRepository;
        this.topicResultRepository = topicResultRepository;
    }

    @GetMapping("/topic/{topicId}")
    @PreAuthorize("hasAnyRole('LECTURER', 'FACULTY_MANAGER')")
    public String showEvaluationForm(@PathVariable Long topicId, @RequestParam String type, Authentication auth, Model model) {
        Topic topic = topicRepository.findById(topicId).orElseThrow();
        Long periodId = topic.getRegistrationPeriod().getId();
        List<EvaluationCriterion> criteria = criterionRepository.findByRegistrationPeriodIdAndIsActiveTrueOrderByDisplayOrderAsc(periodId);
        
        Long evaluatorId = userRepository.findByUsernameIgnoreCase(auth.getName()).orElseThrow().getId();
        Evaluation evaluation = evaluationRepository.findByTopicIdAndEvaluatorIdAndEvaluationType(topicId, evaluatorId, type).orElse(new Evaluation());
        
        List<EvaluationScore> scores = new ArrayList<>();
        if (evaluation.getId() != null) {
            scores = scoreRepository.findByEvaluationId(evaluation.getId());
        }
        
        model.addAttribute("topic", topic);
        model.addAttribute("criteria", criteria);
        model.addAttribute("evaluation", evaluation);
        model.addAttribute("existingScores", scores);
        model.addAttribute("evalType", type);
        
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
            List<EvaluationScore> scores = new ArrayList<>();
            for (Map.Entry<String, String> entry : allParams.entrySet()) {
                if (entry.getKey().startsWith("score_") && !entry.getValue().isBlank()) {
                    Long criterionId = Long.parseLong(entry.getKey().substring(6));
                    BigDecimal val = new BigDecimal(entry.getValue());
                    EvaluationScore score = new EvaluationScore();
                    EvaluationCriterion crit = new EvaluationCriterion();
                    crit.setId(criterionId);
                    score.setCriterion(crit);
                    score.setScore(val);
                    scores.add(score);
                }
            }
            
            Evaluation eval = evaluationService.saveDraft(topicId, auth.getName(), type, scores, comments);
            
            if (submitAction) {
                evaluationService.submitEvaluation(eval.getId());
                redirectAttributes.addFlashAttribute("successMessage", "Nộp điểm thành công!");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Lưu nháp thành công!");
            }
        } catch (BusinessRuleException | NumberFormatException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:/evaluations/topic/" + topicId + "?type=" + type;
    }

    @GetMapping("/results")
    @PreAuthorize("hasAnyRole('FACULTY_MANAGER', 'LECTURER')")
    public String listResults(Model model) {
        model.addAttribute("results", topicResultRepository.findAll());
        return "evaluations/results";
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
    @PreAuthorize("hasRole('LECTURER')") // Council Chair
    public String confirmResult(@PathVariable Long topicId, Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            topicResultService.confirmResult(topicId, auth.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Xác nhận kết quả thành công");
        } catch (BusinessRuleException e) {
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
}
