package com.group9.topicmanagement;

import com.group9.topicmanagement.dto.EvaluationSubmitDto;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.model.*;
import com.group9.topicmanagement.model.enums.*;
import com.group9.topicmanagement.model.evaluation.*;
import com.group9.topicmanagement.model.topic.*;
import com.group9.topicmanagement.repository.*;
import com.group9.topicmanagement.service.EvaluationService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:group9_ballot_state;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.config.import=", "app.mail.enabled=false", "app.demo-password="
})
@ActiveProfiles("test")
class ConcurrentBallotStateRegressionTest {
    @DynamicPropertySource
    static void optionalIsolatedMySql(DynamicPropertyRegistry properties) {
        String url = System.getenv("GROUP9_QA_MYSQL_URL");
        if (url == null || url.isBlank()) return;
        if (!url.matches("jdbc:mysql://(?:localhost|127\\.0\\.0\\.1)(?::[0-9]+)?/group9_qa_[a-z0-9_]+(?:\\?.*)?")) {
            throw new IllegalArgumentException("MySQL regression tests require a dedicated local group9_qa_ database");
        }
        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.datasource.username", () -> System.getenv("GROUP9_QA_MYSQL_USERNAME"));
        properties.add("spring.datasource.password", () -> System.getenv("GROUP9_QA_MYSQL_PASSWORD"));
    }

    @Autowired PlatformTransactionManager transactions;
    @Autowired RoleRepository roles;
    @Autowired DepartmentRepository departments;
    @Autowired UserRepository users;
    @Autowired RegistrationPeriodRepository periods;
    @Autowired TopicRepository topics;
    @Autowired EvaluationCriterionRepository criteria;
    @Autowired ReviewerAssignmentRepository reviewers;
    @Autowired EvaluationRepository evaluations;
    @Autowired EvaluationScoreRepository scores;
    @Autowired EvaluationService service;
    private Fixture fixture;

    record Fixture(Long topicId, String evaluator, List<Long> criterionIds) {}

    @BeforeEach void seedFixture() {
        fixture = new TransactionTemplate(transactions).execute(tx -> {
            String prefix = "ballot" + UUID.randomUUID().toString().substring(0, 8);
            var actor = new User(); actor.setUsername(prefix); actor.setFullName("QA ballot editor");
            actor.setEmail(prefix + "@example.test"); actor.setPasswordHash("unused-test-hash");
            actor.setDepartment(departments.findByCodeIgnoreCase("CNPM").orElseThrow());
            actor.getRoles().add(roles.findByName(RoleName.LECTURER).orElseThrow()); actor = users.save(actor);
            var period = new RegistrationPeriod(); period.setName(prefix); period.setType(PeriodType.MON_HOC);
            period.setStatus(PeriodStatus.IN_PROGRESS); period.setLecturerStart(LocalDateTime.now().minusDays(10));
            period.setLecturerEnd(LocalDateTime.now().minusDays(8)); period.setStudentStart(LocalDateTime.now().minusDays(7));
            period.setStudentEnd(LocalDateTime.now().minusDays(5)); period = periods.save(period);
            var topic = new Topic(); topic.setCode(prefix); topic.setTitle("Current ballot state");
            topic.setDepartment(actor.getDepartment()); topic.setRegistrationPeriod(period);
            topic.setProposer(actor); topic.setStatus(TopicStatus.PUBLISHED); topic = topics.save(topic);
            var reviewer = new ReviewerAssignment(); reviewer.setTopic(topic); reviewer.setReviewer(actor);
            reviewer.setStatus(ReviewerAssignmentStatus.ASSIGNED); reviewer.setDeadline(LocalDateTime.now().plusDays(2));
            reviewers.save(reviewer);
            List<Long> ids = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                var criterion = new EvaluationCriterion(); criterion.setName("Required " + i);
                criterion.setRegistrationPeriod(period); criterion.setDisplayOrder(i);
                criterion.setIsActive(true); criterion.setIsMandatory(true); ids.add(criteria.save(criterion).getId());
            }
            return new Fixture(topic.getId(), actor.getUsername(), ids);
        });
        authenticate(fixture.evaluator(), "LECTURER");
    }

    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @Test void managerCannotLockBallotThatBecameDraftWhileWaiting() throws Exception {
        var original = service.saveEvaluation(fixture.topicId(), fixture.evaluator(), form(true, true, "Submitted original"));
        assertConcurrentDraftIsObserved(original.getId(), true, "Chỉ khóa phiếu đã nộp");
    }

    @Test void submissionCannotUseScoresFromBeforeConcurrentDraftEdit() throws Exception {
        var original = service.saveEvaluation(fixture.topicId(), fixture.evaluator(), form(false, true, "Complete original"));
        assertConcurrentDraftIsObserved(original.getId(), false, "bắt buộc");
    }

    private void assertConcurrentDraftIsObserved(Long ballotId, boolean locking, String expectedError) throws Exception {
        CountDownLatch waitingForTopic = new CountDownLatch(1);
        CountDownLatch releaseWaiting = new CountDownLatch(1);
        String workerName = "waiting-ballot-" + UUID.randomUUID();
        MethodInterceptor pauseBeforeTopicLock = invocation -> {
            if (invocation.getMethod().getName().equals("findLockedById")
                    && Thread.currentThread().getName().equals(workerName)) {
                waitingForTopic.countDown();
                if (!releaseWaiting.await(15, TimeUnit.SECONDS)) throw new AssertionError("Timed out before topic lock");
            }
            return invocation.proceed();
        };
        Advised advised = (Advised) topics; advised.addAdvice(0, pauseBeforeTopicLock);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<String> waiting = pool.submit(() -> {
                Thread.currentThread().setName(workerName);
                authenticate(locking ? "qa-manager" : fixture.evaluator(), locking ? "FACULTY_MANAGER" : "LECTURER");
                try {
                    if (locking) service.lockEvaluation(ballotId); else service.submitEvaluation(ballotId);
                    return "unexpected-success";
                } catch (BusinessRuleException expected) { return expected.getMessage(); }
                finally { SecurityContextHolder.clearContext(); }
            });
            assertThat(waitingForTopic.await(10, TimeUnit.SECONDS)).isTrue();
            service.saveEvaluation(fixture.topicId(), fixture.evaluator(), form(false, false, "New incomplete draft"));
            releaseWaiting.countDown();
            assertThat(waiting.get(15, TimeUnit.SECONDS)).contains(expectedError);
            var stored = evaluations.findById(ballotId).orElseThrow();
            assertThat(stored.getStatus()).isEqualTo(EvaluationStatus.DRAFT);
            assertThat(stored.getComments()).isEqualTo("New incomplete draft");
            assertThat(stored.getSubmissionTime()).isNull();
            assertThat(scores.findByEvaluationId(ballotId)).hasSize(1)
                    .allSatisfy(score -> assertThat(score.getScore()).isEqualByComparingTo("6"));
        } finally {
            releaseWaiting.countDown(); pool.shutdownNow(); pool.awaitTermination(5, TimeUnit.SECONDS);
            advised.removeAdvice(pauseBeforeTopicLock);
        }
    }

    private EvaluationSubmitDto form(boolean submit, boolean complete, String comment) {
        var form = new EvaluationSubmitDto(); form.setType(EvaluationType.REVIEWER);
        form.setSubmitAction(submit); form.setComments(comment);
        Map<Long, BigDecimal> values = new LinkedHashMap<>();
        values.put(fixture.criterionIds().get(0), new BigDecimal(complete ? "8" : "6"));
        if (complete) values.put(fixture.criterionIds().get(1), new BigDecimal("8"));
        form.setScores(values); return form;
    }

    private static void authenticate(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                username, "unused", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
