package com.group9.topicmanagement;

import com.group9.topicmanagement.model.*;
import com.group9.topicmanagement.model.council.*;
import com.group9.topicmanagement.model.enums.*;
import com.group9.topicmanagement.model.evaluation.*;
import com.group9.topicmanagement.model.registration.TopicRegistration;
import com.group9.topicmanagement.model.studentgroup.StudentGroup;
import com.group9.topicmanagement.model.topic.*;
import com.group9.topicmanagement.repository.*;
import com.group9.topicmanagement.service.TopicResultService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:group9_result_query;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.config.import=", "app.mail.enabled=false", "app.demo-password=",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.stat=OFF",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF"
})
@ActiveProfiles("test")
@Transactional
class ResultQueryRegressionTest {
    @Autowired EntityManager em;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired DepartmentRepository departments;
    @Autowired RegistrationPeriodRepository periods;
    @Autowired TopicRepository topics;
    @Autowired CouncilRepository councils;
    @Autowired CouncilMemberRepository members;
    @Autowired CouncilAssignmentRepository assignments;
    @Autowired TopicResultRepository results;
    @Autowired StudentGroupRepository groups;
    @Autowired TopicRegistrationRepository registrations;
    @Autowired EvaluationCriterionRepository criteria;
    @Autowired ReviewerAssignmentRepository reviewers;
    @Autowired EvaluationRepository evaluations;
    @Autowired EvaluationScoreRepository scores;
    @Autowired EvaluationStatisticsRepository statistics;
    @Autowired TopicResultService service;

    private String prefix;
    private int serial;
    private User lecturer;
    private User outsider;
    private Department department;
    private RegistrationPeriod period;
    private List<EvaluationCriterion> mandatory;

    @BeforeEach void fixture() {
        prefix = "resultqa" + UUID.randomUUID().toString().substring(0, 8);
        department = departments.findByCodeIgnoreCase("CNPM").orElseThrow();
        lecturer = actor("lecturer"); outsider = actor("outsider");
        period = new RegistrationPeriod(); period.setName(prefix); period.setType(PeriodType.MON_HOC);
        period.setStatus(PeriodStatus.IN_PROGRESS);
        period.setLecturerStart(LocalDateTime.now().minusDays(20));
        period.setLecturerEnd(LocalDateTime.now().minusDays(15));
        period.setStudentStart(LocalDateTime.now().minusDays(14));
        period.setStudentEnd(LocalDateTime.now().minusDays(10));
        period = periods.save(period);
        mandatory = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            var criterion = new EvaluationCriterion(); criterion.setRegistrationPeriod(period);
            criterion.setName("Required " + i); criterion.setIsActive(true); criterion.setIsMandatory(true);
            criterion.setDisplayOrder(i); mandatory.add(criteria.save(criterion));
        }
    }

    @Test void resultListKeepsCurrentAssignmentPriorityHistoryOrderAndMatchingResult() {
        Topic first = topic(); Topic second = topic();
        var active = assign(second, CouncilStatus.ACTIVE, lecturer);
        assign(second, CouncilStatus.COMPLETED, outsider); // Newer completed must not replace ACTIVE.
        var oldCompleted = assign(first, CouncilStatus.COMPLETED, lecturer);
        var latestCompleted = assign(first, CouncilStatus.COMPLETED, lecturer);
        Topic draft = topic(); var draftAssignment = assign(draft, CouncilStatus.DRAFT, lecturer);
        Topic cancelled = topic(); assign(cancelled, CouncilStatus.DRAFT, lecturer);
        assign(cancelled, CouncilStatus.CANCELLED, lecturer); // Latest fallback is cancelled: hide topic.
        result(second, active, "8.5"); result(first, oldCompleted, "9");
        flushAndClear();

        var list = service.listResults(auth(true));
        assertThat(list).extracting(r -> r.getTopic().getId())
                .containsExactly(second.getId(), first.getId(), draft.getId());
        assertThat(list).extracting(r -> r.getCouncilAssignment().getId())
                .containsExactly(active.getId(), latestCompleted.getId(), draftAssignment.getId());
        assertThat(list.get(0).getFinalScore()).isEqualByComparingTo("8.5");
        assertThat(list.get(1).getFinalScore()).isNull(); // Never show a historical council's score.
    }

    @Test void oldCouncilMembershipDoesNotExposeNewCouncilAndQueryCountIsBounded() {
        Topic visible = topic(); var current = assign(visible, CouncilStatus.ACTIVE, lecturer);
        result(visible, current, "8");
        for (int i = 0; i < 15; i++) {
            Topic hidden = topic(); assign(hidden, CouncilStatus.COMPLETED, lecturer);
            assign(hidden, CouncilStatus.ACTIVE, outsider);
        }
        flushAndClear();
        var hibernate = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        hibernate.clear();

        assertThat(service.listResults(auth(false))).extracting(r -> r.getTopic().getId())
                .containsExactly(visible.getId());
        assertThat(hibernate.getPrepareStatementCount()).isLessThanOrEqualTo(3);
        assertThat(hibernate.getEntityLoadCount()).isLessThan(15);
    }

    @Test void dashboardCountMatchesMissingScoreRulesForReviewerBallots() {
        Topic noBallots = approvedTopic();
        Topic complete = approvedTopic(); reviewerBallot(complete, ReviewerAssignmentStatus.SUBMITTED, true);
        Topic incomplete = approvedTopic(); reviewerBallot(incomplete, ReviewerAssignmentStatus.SUBMITTED, false);
        Topic cancelled = approvedTopic(); reviewerBallot(cancelled, ReviewerAssignmentStatus.CANCELLED, true);
        Topic draftOnly = approvedTopic();
        var draft = reviewerBallot(draftOnly, ReviewerAssignmentStatus.IN_PROGRESS, true);
        draft.setStatus(EvaluationStatus.DRAFT); evaluations.save(draft);
        flushAndClear();

        assertThat(statistics.countApprovedWithMissingMandatoryScores()).isEqualTo(4);
        assertThat(List.of(noBallots, complete, incomplete, cancelled, draftOnly).stream()
                .filter(t -> service.hasMissingMandatoryScores(t.getId())).count()).isEqualTo(4);
    }

    @Test void dashboardIgnoresOldCouncilScoresAndUsesCurrentCouncilMemberIdentity() {
        Topic topic = approvedTopic();
        var old = assign(topic, CouncilStatus.COMPLETED, lecturer);
        councilBallot(topic, old, lecturer, true);
        var current = assign(topic, CouncilStatus.ACTIVE, outsider);
        flushAndClear();
        assertThat(statistics.countApprovedWithMissingMandatoryScores()).isEqualTo(1);
        assertThat(service.hasMissingMandatoryScores(topic.getId())).isTrue();
        councilBallot(em.getReference(Topic.class, topic.getId()), current, outsider, true);
        em.flush();
        assertThat(statistics.countApprovedWithMissingMandatoryScores()).isZero();
        assertThat(service.hasMissingMandatoryScores(topic.getId())).isFalse();
    }

    private User actor(String name) {
        var actor = new User(); actor.setUsername(prefix + name + serial++);
        actor.setFullName("QA " + name); actor.setEmail(actor.getUsername() + "@example.test");
        actor.setPasswordHash("unused-test-hash"); actor.setDepartment(department);
        actor.getRoles().add(roles.findByName(RoleName.LECTURER).orElseThrow());
        return users.save(actor);
    }

    private Topic topic() {
        var topic = new Topic(); topic.setCode(prefix + serial++); topic.setTitle("Result query QA");
        topic.setDepartment(department); topic.setRegistrationPeriod(period); topic.setProposer(lecturer);
        topic.setStatus(TopicStatus.PUBLISHED); return topics.save(topic);
    }

    private Topic approvedTopic() {
        var topic = topic(); var group = new StudentGroup(); group.setLeader(actor("leader"));
        group.setRegistrationPeriod(period); group = groups.save(group);
        var registration = new TopicRegistration(); registration.setStudentGroup(group);
        registration.setTopic(topic); registration.setRegistrationPeriod(period);
        registration.setStatus(RegistrationStatus.APPROVED); registrations.save(registration);
        return topic;
    }

    private CouncilAssignment assign(Topic topic, CouncilStatus status, User member) {
        var council = new Council(); council.setName(prefix + serial++); council.setRegistrationPeriod(period);
        council.setStatus(status); council.setReportDate(LocalDateTime.now().plusDays(5));
        council.setLocation("QA"); council = councils.save(council);
        var membership = new CouncilMember(); membership.setCouncil(council); membership.setMember(member);
        membership.setRole(CouncilMemberRole.CHAIR); members.save(membership);
        var assignment = new CouncilAssignment(); assignment.setTopic(topic); assignment.setCouncil(council);
        return assignments.save(assignment);
    }

    private void result(Topic topic, CouncilAssignment assignment, String score) {
        var result = new TopicResult(); result.setTopic(topic); result.setCouncilAssignment(assignment);
        result.setFinalScore(new BigDecimal(score)); results.save(result);
    }

    private Evaluation reviewerBallot(Topic topic, ReviewerAssignmentStatus status, boolean complete) {
        var reviewer = new ReviewerAssignment(); reviewer.setTopic(topic); reviewer.setReviewer(lecturer);
        reviewer.setStatus(status); reviewer.setDeadline(LocalDateTime.now().plusDays(2));
        reviewer = reviewers.save(reviewer);
        var evaluation = new Evaluation(); evaluation.setTopic(topic); evaluation.setEvaluator(lecturer);
        evaluation.setEvaluationType(EvaluationType.REVIEWER); evaluation.setReviewerAssignment(reviewer);
        evaluation.setStatus(EvaluationStatus.SUBMITTED); evaluation = evaluations.save(evaluation);
        score(evaluation, complete); return evaluation;
    }

    private void councilBallot(Topic topic, CouncilAssignment assignment, User evaluator, boolean complete) {
        var membership = members.findByCouncilId(assignment.getCouncil().getId()).stream()
                .filter(m -> m.getMember().getId().equals(evaluator.getId())).findFirst().orElseThrow();
        var evaluation = new Evaluation(); evaluation.setTopic(topic); evaluation.setEvaluator(evaluator);
        evaluation.setEvaluationType(EvaluationType.COUNCIL_MEMBER); evaluation.setCouncilMember(membership);
        evaluation.setStatus(EvaluationStatus.LOCKED); evaluation = evaluations.save(evaluation);
        score(evaluation, complete);
    }

    private void score(Evaluation evaluation, boolean complete) {
        for (var criterion : mandatory.subList(0, complete ? mandatory.size() : 1)) {
            var score = new EvaluationScore(); score.setEvaluation(evaluation); score.setCriterion(criterion);
            score.setScore(new BigDecimal("8")); scores.save(score);
        }
    }

    private UsernamePasswordAuthenticationToken auth(boolean manager) {
        return new UsernamePasswordAuthenticationToken(lecturer.getUsername(), "unused", List.of(
                new SimpleGrantedAuthority(manager ? "ROLE_FACULTY_MANAGER" : "ROLE_LECTURER")));
    }

    private void flushAndClear() { em.flush(); em.clear(); }
}
