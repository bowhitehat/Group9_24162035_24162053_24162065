package com.group9.topicmanagement.service;

import com.group9.topicmanagement.model.council.CouncilAssignment;
import com.group9.topicmanagement.model.enums.CouncilStatus;
import com.group9.topicmanagement.model.enums.EvaluationStatus;
import com.group9.topicmanagement.model.enums.EvaluationType;
import com.group9.topicmanagement.model.enums.ReviewerAssignmentStatus;
import com.group9.topicmanagement.model.evaluation.Evaluation;
import com.group9.topicmanagement.model.evaluation.EvaluationCriterion;
import com.group9.topicmanagement.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** One consistent ballot scope for calculation, confirmation and council completion. */
@Service
@Transactional(readOnly = true)
public class EvaluationScopeService {
    private final CouncilAssignmentRepository assignments;
    private final CouncilMemberRepository members;
    private final ReviewerAssignmentRepository reviewers;
    private final EvaluationRepository evaluations;
    private final EvaluationCriterionRepository criteria;
    private final EvaluationScoreRepository scores;

    public EvaluationScopeService(CouncilAssignmentRepository assignments, CouncilMemberRepository members,
                                  ReviewerAssignmentRepository reviewers, EvaluationRepository evaluations,
                                  EvaluationCriterionRepository criteria, EvaluationScoreRepository scores) {
        this.assignments = assignments;
        this.members = members;
        this.reviewers = reviewers;
        this.evaluations = evaluations;
        this.criteria = criteria;
        this.scores = scores;
    }

    public Optional<CouncilAssignment> currentAssignment(Long topicId) {
        return assignments.findByTopicIdAndCouncil_StatusIn(topicId,
                List.of(CouncilStatus.ACTIVE, CouncilStatus.COMPLETED)).stream().findFirst();
    }

    public List<Evaluation> currentEvaluations(Long topicId) {
        var currentMembers = currentAssignment(topicId)
                .map(a -> members.findByCouncilId(a.getCouncil().getId())).orElse(List.of());
        var currentReviewers = reviewers.findByTopicId(topicId).stream()
                .filter(a -> a.getStatus() != null && a.getStatus() != ReviewerAssignmentStatus.CANCELLED).toList();
        return evaluations.findByTopicId(topicId).stream().filter(e -> {
            if (e.getEvaluationType() == EvaluationType.COUNCIL_MEMBER) {
                return e.getCouncilMember() != null && currentMembers.stream().anyMatch(m ->
                        m.getId().equals(e.getCouncilMember().getId())
                                && m.getMember().getId().equals(e.getEvaluator().getId()));
            }
            return e.getEvaluationType() == EvaluationType.REVIEWER && e.getReviewerAssignment() != null
                    && currentReviewers.stream().anyMatch(a -> a.getId().equals(e.getReviewerAssignment().getId())
                            && a.getReviewer().getId().equals(e.getEvaluator().getId()));
        }).toList();
    }

    public boolean allSubmitted(Long topicId) {
        var assignment = currentAssignment(topicId);
        if (assignment.isEmpty() || !councilSubmitted(assignment.get())) return false;
        var ballots = currentEvaluations(topicId);
        return reviewers.findByTopicId(topicId).stream()
                .filter(a -> a.getStatus() != null && a.getStatus() != ReviewerAssignmentStatus.CANCELLED)
                .allMatch(a -> ballots.stream().anyMatch(e -> e.getReviewerAssignment() != null
                        && e.getReviewerAssignment().getId().equals(a.getId()) && submitted(e)));
    }

    public boolean councilSubmitted(CouncilAssignment assignment) {
        var councilMembers = members.findByCouncilId(assignment.getCouncil().getId());
        if (councilMembers.size() < 3 || councilMembers.size() > 5) return false;
        var mandatory = criteria.findByRegistrationPeriodIdAndIsActiveTrueOrderByDisplayOrderAsc(
                assignment.getTopic().getRegistrationPeriod().getId()).stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsMandatory())).map(EvaluationCriterion::getId).toList();
        if (mandatory.isEmpty()) return false;
        var ballots = evaluations.findByTopicIdAndEvaluationType(
                assignment.getTopic().getId(), EvaluationType.COUNCIL_MEMBER);
        Map<Long, Set<Long>> scoredCriteria = ballots.isEmpty() ? Map.of() : scores.findByEvaluationIdIn(
                        ballots.stream().map(Evaluation::getId).toList()).stream()
                .filter(s -> s.getScore() != null)
                .collect(Collectors.groupingBy(s -> s.getEvaluation().getId(),
                        Collectors.mapping(s -> s.getCriterion().getId(), Collectors.toSet())));
        return councilMembers.stream().allMatch(m -> ballots.stream().anyMatch(e ->
                e.getCouncilMember() != null && m.getId().equals(e.getCouncilMember().getId())
                        && m.getMember().getId().equals(e.getEvaluator().getId()) && submitted(e)
                        && scoredCriteria.getOrDefault(e.getId(), Set.of()).containsAll(mandatory)));
    }

    private boolean submitted(Evaluation e) {
        return e.getStatus() == EvaluationStatus.SUBMITTED || e.getStatus() == EvaluationStatus.LOCKED;
    }
}
