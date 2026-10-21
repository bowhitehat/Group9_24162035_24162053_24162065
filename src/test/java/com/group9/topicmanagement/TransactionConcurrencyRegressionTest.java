package com.group9.topicmanagement;

import com.group9.topicmanagement.dto.EvaluationSubmitDto;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.model.*;
import com.group9.topicmanagement.model.council.*;
import com.group9.topicmanagement.model.enums.*;
import com.group9.topicmanagement.model.evaluation.*;
import com.group9.topicmanagement.model.topic.*;
import com.group9.topicmanagement.repository.*;
import com.group9.topicmanagement.service.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:group9_atomic_concurrency;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.config.import=", "app.mail.enabled=false", "app.demo-password="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionConcurrencyRegressionTest {
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

    @Autowired MockMvc mvc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired RoleRepository roles;
    @Autowired DepartmentRepository departments;
    @Autowired UserRepository users;
    @Autowired RegistrationPeriodRepository periods;
    @Autowired CouncilRepository councils;
    @Autowired CouncilMemberRepository members;
    @Autowired CouncilAssignmentRepository assignments;
    @Autowired TopicRepository topics;
    @Autowired EvaluationCriterionRepository criteria;
    @Autowired ReviewerAssignmentRepository reviewers;
    @Autowired EvaluationRepository evaluations;
    @MockitoSpyBean EvaluationScoreRepository scores;
    @Autowired EvaluationService evaluationService;
    @Autowired CouncilService councilService;
    @Autowired ReviewerAssignmentService reviewerService;

    private Fixture fixture;
    record Fixture(Long periodId, Long topicId, List<Long> users, String evaluator,
                   Long councilId, List<Long> criterionIds, Long reviewerId) {}

    @BeforeEach void seedIsolatedFixture() {
        // Unique, scoped fixtures avoid resetting any other test database or demo data.
        fixture = new TransactionTemplate(transactions).execute(tx -> {
            String prefix = "atomic" + UUID.randomUUID().toString().substring(0, 8);
            var role = roles.findByName(RoleName.LECTURER).orElseThrow();
            var department = departments.findByCodeIgnoreCase("CNPM").orElseThrow();
            List<User> actors = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                User actor = new User();
                actor.setUsername(prefix + "_" + i); actor.setFullName("QA " + i);
                actor.setEmail(prefix + i + "@example.test"); actor.setPasswordHash("unused-test-hash");
                actor.setDepartment(department); actor.getRoles().add(role); actors.add(users.save(actor));
            }
            var period = new RegistrationPeriod();
            period.setName(prefix); period.setType(PeriodType.MON_HOC); period.setStatus(PeriodStatus.IN_PROGRESS);
            period.setLecturerStart(LocalDateTime.now().minusDays(20));
            period.setLecturerEnd(LocalDateTime.now().minusDays(15));
            period.setStudentStart(LocalDateTime.now().minusDays(14));
            period.setStudentEnd(LocalDateTime.now().minusDays(10));
            period = periods.save(period);
            var topic = new Topic(); topic.setCode(prefix); topic.setTitle("Atomic grading QA");
            topic.setDepartment(department); topic.setRegistrationPeriod(period);
            topic.setProposer(actors.get(6)); topic.setStatus(TopicStatus.PUBLISHED); topic = topics.save(topic);
            var council = new Council(); council.setName(prefix); council.setRegistrationPeriod(period);
            council.setStatus(CouncilStatus.DRAFT); council.setReportDate(LocalDateTime.now().plusDays(5));
            council.setLocation("QA"); council = councils.save(council);
            for (int i = 0; i < 4; i++) {
                var member = new CouncilMember(); member.setCouncil(council); member.setMember(actors.get(i));
                member.setRole(i == 0 ? CouncilMemberRole.CHAIR : i == 1 ? CouncilMemberRole.SECRETARY : CouncilMemberRole.MEMBER);
                members.save(member);
            }
            var assignment = new CouncilAssignment(); assignment.setCouncil(council); assignment.setTopic(topic);
            assignments.save(assignment);
            List<Long> criterionIds = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                var criterion = new EvaluationCriterion(); criterion.setName("Required " + i);
                criterion.setRegistrationPeriod(period); criterion.setDisplayOrder(i);
                criterion.setIsActive(true); criterion.setIsMandatory(true); criterionIds.add(criteria.save(criterion).getId());
            }
            var reviewer = new ReviewerAssignment(); reviewer.setTopic(topic); reviewer.setReviewer(actors.get(0));
            reviewer.setStatus(ReviewerAssignmentStatus.ASSIGNED); reviewer.setDeadline(LocalDateTime.now().plusDays(3));
            reviewer = reviewers.save(reviewer);
            return new Fixture(period.getId(), topic.getId(), actors.stream().map(User::getId).toList(),
                    actors.get(0).getUsername(), council.getId(), criterionIds, reviewer.getId());
        });
        authenticate(fixture.evaluator(), "LECTURER");
        clearInvocations(scores);
    }

    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @Test void failedHttpResubmissionPreservesSubmittedBallotAndScores() throws Exception {
        var original = evaluationService.saveEvaluation(fixture.topicId(), fixture.evaluator(), form(true, true));
        var submittedAt = evaluations.findById(original.getId()).orElseThrow().getSubmissionTime();
        mvc.perform(post("/evaluations/topic/" + fixture.topicId()).with(user(fixture.evaluator()).roles("LECTURER"))
                        .with(csrf()).param("type", "REVIEWER").param("submitAction", "true")
                        .param("comments", "Incomplete replacement")
                        .param("scores[" + fixture.criterionIds().get(0) + "]", "9"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("errorMessage"));
        var stored = evaluations.findById(original.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(EvaluationStatus.SUBMITTED);
        assertThat(stored.getComments()).isEqualTo("Original complete ballot");
        assertThat(stored.getSubmissionTime()).isEqualTo(submittedAt);
        assertThat(scores.findByEvaluationId(stored.getId())).hasSize(2)
                .allSatisfy(score -> assertThat(score.getScore()).isEqualByComparingTo("8"));
        assertThat(reviewers.findById(fixture.reviewerId()).orElseThrow().getStatus()).isEqualTo(ReviewerAssignmentStatus.SUBMITTED);
    }

    @Test void failedFirstSubmissionLeavesNoPartialBallotOrReviewerState() {
        assertThatThrownBy(() -> evaluationService.saveEvaluation(fixture.topicId(), fixture.evaluator(), form(true, false)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("bắt buộc");
        assertThat(evaluations.findByTopicId(fixture.topicId())).isEmpty();
        assertThat(reviewers.findById(fixture.reviewerId()).orElseThrow().getStatus()).isEqualTo(ReviewerAssignmentStatus.ASSIGNED);
    }

    @Test void validSubmissionSavesAllScoresAndReviewerTogether() {
        var ballot = evaluationService.saveEvaluation(fixture.topicId(), fixture.evaluator(), form(true, true));
        assertThat(ballot.getStatus()).isEqualTo(EvaluationStatus.SUBMITTED);
        assertThat(scores.findByEvaluationId(ballot.getId())).hasSize(2);
        assertThat(reviewers.findById(fixture.reviewerId()).orElseThrow().getStatus()).isEqualTo(ReviewerAssignmentStatus.SUBMITTED);
    }

    @Test void intentionalPartialDraftRemainsSupported() {
        var ballot = evaluationService.saveEvaluation(fixture.topicId(), fixture.evaluator(), form(false, false));
        assertThat(ballot.getStatus()).isEqualTo(EvaluationStatus.DRAFT);
        assertThat(scores.findByEvaluationId(ballot.getId())).hasSize(1);
    }

    @Test void savingScoresReadsExistingScoresOnceInsteadOfPerCriterion() {
        evaluationService.saveEvaluation(fixture.topicId(), fixture.evaluator(), form(false, true));
        verify(scores, times(1)).findByEvaluationId(anyLong());
    }

    @Test void resultDetailLoadsScoreRowsInOneBatch() {
        evaluationService.saveEvaluation(fixture.topicId(), fixture.evaluator(), form(true, true));
        clearInvocations(scores);
        var detail = evaluationService.listByTopic(fixture.topicId());
        assertThat(detail).hasSize(1);
        assertThat(detail.get(0).getScores()).hasSize(2);
        verify(scores, times(1)).findByEvaluationIdIn(anyCollection());
        verify(scores, never()).findByEvaluationId(anyLong());
    }

    @Test void concurrentAddsCannotExceedFiveMembers() throws Exception {
        raceTwoAdds(CouncilMemberRole.MEMBER);
        assertThat(members.findByCouncilId(fixture.councilId())).hasSize(5);
    }

    @Test void concurrentAddsCannotCreateTwoChairs() throws Exception {
        // Remove existing members from this fixture only, before starting either transaction.
        new TransactionTemplate(transactions).executeWithoutResult(tx -> members.deleteAll(members.findByCouncilId(fixture.councilId())));
        raceTwoAdds(CouncilMemberRole.CHAIR);
        assertThat(members.countByCouncilIdAndRole(fixture.councilId(), CouncilMemberRole.CHAIR)).isEqualTo(1);
    }

    @Test void activeCouncilRejectsLateStructureChanges() {
        authenticate("qa-manager", "FACULTY_MANAGER");
        councilService.activateCouncil(fixture.councilId());
        assertThatThrownBy(() -> councilService.addMember(fixture.councilId(), fixture.users().get(4), CouncilMemberRole.MEMBER))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("kích hoạt");
        assertThat(councils.findById(fixture.councilId()).orElseThrow().getStatus()).isEqualTo(CouncilStatus.ACTIVE);
        assertThat(members.findByCouncilId(fixture.councilId())).hasSize(4);
    }

    @Test void delayedCancellationCannotOverwriteSubmittedReviewer() throws Exception {
        assertDelayedManagerMutationRejectsSubmittedReviewer(false);
    }

    @Test void delayedReviewerChangeCannotOverwriteSubmittedReviewer() throws Exception {
        assertDelayedManagerMutationRejectsSubmittedReviewer(true);
    }

    @Test void councilCancellationSeesBallotCommittedWhileWaitingForTopicLock() throws Exception {
        new TransactionTemplate(transactions).executeWithoutResult(tx ->
                councils.findById(fixture.councilId()).orElseThrow().setStatus(CouncilStatus.ACTIVE));
        CountDownLatch paused = new CountDownLatch(1), release = new CountDownLatch(1);
        String worker = "delayed-council-cancellation";
        var advised = (org.springframework.aop.framework.Advised) topics;
        org.aopalliance.intercept.MethodInterceptor pause = invocation -> {
            if (invocation.getMethod().getName().equals("findLockedById") && Thread.currentThread().getName().equals(worker)) {
                paused.countDown();
                if (!release.await(15, TimeUnit.SECONDS)) throw new AssertionError("Timed out before topic lock");
            }
            return invocation.proceed();
        };
        advised.addAdvice(0, pause);
        var pool = Executors.newSingleThreadExecutor();
        try {
            Future<String> cancellation = pool.submit(() -> {
                Thread.currentThread().setName(worker); authenticate("qa-manager", "FACULTY_MANAGER");
                try { councilService.cancelCouncil(fixture.councilId()); return "unexpected-success"; }
                catch (BusinessRuleException expected) { return expected.getMessage(); }
                finally { SecurityContextHolder.clearContext(); }
            });
            assertThat(paused.await(10, TimeUnit.SECONDS)).isTrue();
            var ballotForm = form(true, true); ballotForm.setType(EvaluationType.COUNCIL_MEMBER);
            evaluationService.saveEvaluation(fixture.topicId(), fixture.evaluator(), ballotForm);
            release.countDown();
            assertThat(cancellation.get(15, TimeUnit.SECONDS)).contains("đã có phiếu chấm");
            assertThat(councils.findById(fixture.councilId()).orElseThrow().getStatus()).isEqualTo(CouncilStatus.ACTIVE);
            assertThat(evaluations.findByTopicId(fixture.topicId())).singleElement()
                    .satisfies(ballot -> assertThat(ballot.getStatus()).isEqualTo(EvaluationStatus.SUBMITTED));
        } finally {
            release.countDown(); pool.shutdownNow(); pool.awaitTermination(5, TimeUnit.SECONDS); advised.removeAdvice(pause);
        }
    }

    private void assertDelayedManagerMutationRejectsSubmittedReviewer(boolean change) throws Exception {
        CountDownLatch paused = new CountDownLatch(1), release = new CountDownLatch(1);
        String worker = "delayed-reviewer-manager";
        org.springframework.aop.framework.Advised advised = (org.springframework.aop.framework.Advised) topics;
        org.aopalliance.intercept.MethodInterceptor pause = invocation -> {
            if (invocation.getMethod().getName().equals("findLockedById") && Thread.currentThread().getName().equals(worker)) {
                paused.countDown();
                if (!release.await(15, TimeUnit.SECONDS)) throw new AssertionError("Timed out before topic lock");
            }
            return invocation.proceed();
        };
        advised.addAdvice(0, pause);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<String> manager = pool.submit(() -> {
                Thread.currentThread().setName(worker); authenticate("qa-manager", "FACULTY_MANAGER");
                try {
                    if (change) reviewerService.changeReviewer(fixture.reviewerId(), fixture.users().get(4), null);
                    else reviewerService.cancelAssignment(fixture.reviewerId());
                    return "unexpected-success";
                } catch (BusinessRuleException expected) { return expected.getMessage(); }
                finally { SecurityContextHolder.clearContext(); }
            });
            assertThat(paused.await(10, TimeUnit.SECONDS)).isTrue();
            evaluationService.saveEvaluation(fixture.topicId(), fixture.evaluator(), form(true, true));
            release.countDown();
            assertThat(manager.get(15, TimeUnit.SECONDS)).contains("đã nộp điểm");
            var stored = reviewers.findById(fixture.reviewerId()).orElseThrow();
            assertThat(stored.getStatus()).isEqualTo(ReviewerAssignmentStatus.SUBMITTED);
            assertThat(stored.getReviewer().getId()).isEqualTo(fixture.users().getFirst());
        } finally {
            release.countDown(); pool.shutdownNow(); pool.awaitTermination(5, TimeUnit.SECONDS); advised.removeAdvice(pause);
        }
    }

    private void raceTwoAdds(CouncilMemberRole role) throws Exception {
        CountDownLatch firstChecked = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        org.springframework.aop.framework.Advised advised = (org.springframework.aop.framework.Advised) members;
        org.aopalliance.intercept.MethodInterceptor holdFirstSave = invocation -> {
            if (invocation.getMethod().getName().equals("save")
                    && Thread.currentThread().getName().equals("first-council-editor")) {
                firstChecked.countDown();
                if (!releaseFirst.await(10, TimeUnit.SECONDS)) throw new AssertionError("Timed out waiting to commit first edit");
            }
            return invocation.proceed();
        };
        advised.addAdvice(0, holdFirstSave);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<String> first = pool.submit(() -> {
                Thread.currentThread().setName("first-council-editor");
                return addAsManager(fixture.users().get(4), role);
            });
            assertThat(firstChecked.await(5, TimeUnit.SECONDS)).isTrue();
            Future<String> second = pool.submit(() -> {
                secondStarted.countDown();
                return addAsManager(fixture.users().get(5), role);
            });
            assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> second.get(300, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            releaseFirst.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS)).isEqualTo("OK");
            assertThat(second.get(10, TimeUnit.SECONDS)).isEqualTo("REJECTED");
        } finally {
            releaseFirst.countDown(); pool.shutdownNow(); pool.awaitTermination(5, TimeUnit.SECONDS);
            advised.removeAdvice(holdFirstSave);
        }
    }

    private String addAsManager(Long userId, CouncilMemberRole role) {
        authenticate("qa-manager", "FACULTY_MANAGER");
        try { councilService.addMember(fixture.councilId(), userId, role); return "OK"; }
        catch (BusinessRuleException expected) { return "REJECTED"; }
        finally { SecurityContextHolder.clearContext(); }
    }

    private EvaluationSubmitDto form(boolean submit, boolean complete) {
        var form = new EvaluationSubmitDto(); form.setType(EvaluationType.REVIEWER);
        form.setSubmitAction(submit); form.setComments("Original complete ballot");
        Map<Long, BigDecimal> values = new LinkedHashMap<>();
        values.put(fixture.criterionIds().get(0), new BigDecimal("8"));
        if (complete) values.put(fixture.criterionIds().get(1), new BigDecimal("8"));
        form.setScores(values); return form;
    }

    private static void authenticate(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                username, "unused", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
