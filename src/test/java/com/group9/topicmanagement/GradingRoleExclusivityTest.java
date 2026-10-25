package com.group9.topicmanagement;

import com.group9.topicmanagement.dto.EvaluationSubmitDto;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.model.*;
import com.group9.topicmanagement.model.council.*;
import com.group9.topicmanagement.model.enums.*;
import com.group9.topicmanagement.model.evaluation.*;
import com.group9.topicmanagement.model.registration.TopicRegistration;
import com.group9.topicmanagement.model.studentgroup.*;
import com.group9.topicmanagement.model.topic.*;
import com.group9.topicmanagement.repository.*;
import com.group9.topicmanagement.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:group9_role_exclusive;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.config.import=", "app.mail.enabled=false", "app.demo-password="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GradingRoleExclusivityTest {
    @DynamicPropertySource static void optionalIsolatedMySql(DynamicPropertyRegistry properties) {
        String url = System.getenv("GROUP9_QA_MYSQL_URL");
        if (url == null || url.isBlank()) return;
        if (!url.matches("jdbc:mysql://(?:localhost|127\\.0\\.0\\.1)(?::[0-9]+)?/group9_qa_[a-z0-9_]+(?:\\?.*)?")) {
            throw new IllegalArgumentException("Use a dedicated local group9_qa_ database only");
        }
        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.datasource.username", () -> System.getenv("GROUP9_QA_MYSQL_USERNAME"));
        properties.add("spring.datasource.password", () -> System.getenv("GROUP9_QA_MYSQL_PASSWORD"));
    }

    @Autowired RoleRepository roles;
    @Autowired DepartmentRepository departments;
    @Autowired UserRepository users;
    @Autowired RegistrationPeriodRepository periods;
    @Autowired StudentGroupRepository groups;
    @Autowired TopicRegistrationRepository registrations;
    @Autowired TopicRepository topics;
    @Autowired CouncilRepository councils;
    @Autowired CouncilAssignmentRepository assignments;
    @Autowired CouncilMemberRepository members;
    @Autowired ReviewerAssignmentRepository reviewers;
    @Autowired EvaluationRepository evaluations;
    @Autowired EvaluationScoreRepository scores;
    @Autowired EvaluationCriterionRepository criteria;
    @Autowired TopicResultRepository results;
    @Autowired CouncilService councilService;
    @Autowired ReviewerAssignmentService reviewerService;
    @Autowired EvaluationService evaluationService;
    @Autowired EvaluationScopeService scope;
    @Autowired TopicResultService resultService;
    @Autowired PlatformTransactionManager transactions;
    @Autowired MockMvc mvc;
    Fixture f;
    record Fixture(Long topic, Long council, Long reviewerAssignment, Long criterion,
                   List<Long> people, List<String> names, String manager) { }
    <T> T tx(Supplier<T> work) { return new TransactionTemplate(transactions).execute(s -> work.get()); }

    @BeforeEach void setup() { f = fixture(); auth(f.manager(), "FACULTY_MANAGER"); }
    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

    Fixture fixture() {
        return tx(() -> {
            String prefix = "exclusive" + UUID.randomUUID().toString().substring(0, 8);
            var department = departments.findByCodeIgnoreCase("CNPM").orElseThrow();
            var lecturer = roles.findByName(RoleName.LECTURER).orElseThrow();
            List<User> actors = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                User u = new User(); u.setUsername(prefix + "_" + i);
                u.setFullName(i == 0 ? "Phản biện <demo>" : "Giảng viên " + i);
                u.setEmail(prefix + i + "@example.test"); u.setPasswordHash("unused-test-hash");
                u.setDepartment(department);
                u.getRoles().add(i == 7 ? roles.findByName(RoleName.FACULTY_MANAGER).orElseThrow() : lecturer);
                actors.add(users.save(u));
            }
            var period = new RegistrationPeriod(); period.setName(prefix); period.setType(PeriodType.KLTN);
            period.setStatus(PeriodStatus.GRADING);
            period.setLecturerStart(LocalDateTime.now().minusDays(20)); period.setLecturerEnd(LocalDateTime.now().minusDays(15));
            period.setStudentStart(LocalDateTime.now().minusDays(14)); period.setStudentEnd(LocalDateTime.now().minusDays(5));
            period.setReportSubmissionDeadline(LocalDateTime.now().minusDays(1));
            period.setReviewDeadline(LocalDateTime.now().plusDays(3)); period.setCouncilDate(LocalDateTime.now().plusDays(4).toLocalDate());
            period = periods.save(period);
            Topic topic = new Topic(); topic.setCode(prefix); topic.setTitle("Vai trò không trùng");
            topic.setDepartment(department); topic.setRegistrationPeriod(period); topic.setProposer(actors.get(4));
            topic.getAdvisors().add(actors.get(4)); topic.setStatus(TopicStatus.PUBLISHED); topic = topics.save(topic);
            StudentGroup group = new StudentGroup(); group.setRegistrationPeriod(period); group.setLeader(actors.get(5));
            group = groups.save(group);
            TopicRegistration reg = new TopicRegistration(); reg.setTopic(topic); reg.setStudentGroup(group);
            reg.setRegistrationPeriod(period); reg.setStatus(RegistrationStatus.APPROVED); registrations.save(reg);
            Council council = new Council(); council.setName(prefix); council.setRegistrationPeriod(period);
            council.setReportDate(LocalDateTime.now().plusDays(4)); council.setLocation("QA"); council.setStatus(CouncilStatus.DRAFT);
            council = councils.save(council);
            for (int i = 1; i <= 3; i++) {
                CouncilMember m = new CouncilMember(); m.setCouncil(council); m.setMember(actors.get(i));
                m.setRole(i == 1 ? CouncilMemberRole.CHAIR : i == 2 ? CouncilMemberRole.SECRETARY : CouncilMemberRole.MEMBER);
                members.save(m);
            }
            ReviewerAssignment reviewer = new ReviewerAssignment(); reviewer.setTopic(topic); reviewer.setReviewer(actors.get(0));
            reviewer.setAssigner(actors.get(7)); reviewer.setStatus(ReviewerAssignmentStatus.ASSIGNED);
            reviewer.setDeadline(LocalDateTime.now().plusDays(2)); reviewer = reviewers.save(reviewer);
            EvaluationCriterion criterion = new EvaluationCriterion(); criterion.setName("Bắt buộc");
            criterion.setRegistrationPeriod(period); criterion.setDisplayOrder(1); criterion.setIsActive(true); criterion.setIsMandatory(true);
            criterion = criteria.save(criterion);
            return new Fixture(topic.getId(), council.getId(), reviewer.getId(), criterion.getId(),
                    actors.stream().map(User::getId).toList(), actors.stream().map(User::getUsername).toList(), actors.get(7).getUsername());
        });
    }

    void assign() { councilService.assignTopic(f.council(), f.topic()); }
    void rawAssignmentAndConflict(boolean active) {
        tx(() -> {
            Council c = councils.findById(f.council()).orElseThrow();
            if (active) c.setStatus(CouncilStatus.ACTIVE);
            CouncilAssignment a = new CouncilAssignment(); a.setCouncil(c); a.setTopic(topics.findById(f.topic()).orElseThrow()); assignments.save(a);
            CouncilMember m = new CouncilMember(); m.setCouncil(c); m.setMember(users.findById(f.people().get(0)).orElseThrow());
            m.setRole(CouncilMemberRole.MEMBER); members.save(m); return null;
        });
    }
    EvaluationSubmitDto form(EvaluationType type) {
        EvaluationSubmitDto dto = new EvaluationSubmitDto(); dto.setType(type); dto.setSubmitAction(true);
        dto.setScores(Map.of(f.criterion(), new BigDecimal("8"))); return dto;
    }

    @Test void memberAddedAfterTopicCannotBeItsReviewer() {
        assign();
        assertThatThrownBy(() -> councilService.addMember(f.council(), f.people().get(0), CouncilMemberRole.MEMBER))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("phản biện", "cùng đề tài");
        assertThat(members.findByCouncilId(f.council())).hasSize(3);
    }
    @Test void topicAddedAfterMemberCannotHaveSameReviewer() {
        councilService.addMember(f.council(), f.people().get(0), CouncilMemberRole.MEMBER);
        assertThatThrownBy(this::assign).isInstanceOf(BusinessRuleException.class).hasMessageContaining("phản biện");
        assertThat(assignments.findByCouncilId(f.council())).isEmpty();
    }
    @Test void reviewerAssignedAfterCouncilMembershipIsRejected() {
        assign();
        assertThatThrownBy(() -> reviewerService.assignReviewer(f.topic(), f.people().get(1), f.manager(), LocalDateTime.now().plusDays(1)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("thành viên hội đồng");
        assertThat(reviewers.findByTopicId(f.topic())).hasSize(1);
    }
    @Test void changingReviewerToCouncilMemberIsRejected() {
        assign();
        assertThatThrownBy(() -> reviewerService.changeReviewer(f.reviewerAssignment(), f.people().get(1), null))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("thành viên hội đồng");
        assertThat(reviewers.findById(f.reviewerAssignment()).orElseThrow().getReviewer().getId()).isEqualTo(f.people().get(0));
    }
    @Test void cancelledReviewerDoesNotBlockCouncilAssignment() {
        reviewerService.cancelAssignment(f.reviewerAssignment());
        councilService.addMember(f.council(), f.people().get(0), CouncilMemberRole.MEMBER); assign();
        assertThat(scope.roleConflictReason(f.topic())).isNull();
    }
    @Test void cancelledCouncilDoesNotBlockNewReviewer() {
        assign(); tx(() -> { councils.findById(f.council()).orElseThrow().setStatus(CouncilStatus.CANCELLED); return null; });
        reviewerService.assignReviewer(f.topic(), f.people().get(1), f.manager(), LocalDateTime.now().plusDays(1));
        assertThat(reviewers.findByTopicId(f.topic())).hasSize(2);
    }
    @Test void memberOfAnotherTopicsCouncilMayBeReviewer() {
        assign(); Fixture other = fixture();
        reviewerService.assignReviewer(other.topic(), f.people().get(1), f.manager(), LocalDateTime.now().plusDays(1));
        assertThat(scope.roleConflictReason(other.topic())).isNull();
    }
    @Test void legacyConflictCannotActivateCouncil() {
        rawAssignmentAndConflict(false);
        assertThatThrownBy(() -> councilService.activateCouncil(f.council())).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("đồng thời", "giữ nguyên");
        assertThat(councils.findById(f.council()).orElseThrow().getStatus()).isEqualTo(CouncilStatus.DRAFT);
        assertThat(councilService.canActivate(councils.findById(f.council()).orElseThrow(), true)).isFalse();
    }
    @ParameterizedTest @ValueSource(booleans = {true, false})
    void legacyConflictCannotSaveEitherType(boolean reviewer) {
        rawAssignmentAndConflict(true); auth(f.names().get(0), "LECTURER");
        assertThatThrownBy(() -> evaluationService.saveEvaluation(f.topic(), f.names().get(0),
                form(reviewer ? EvaluationType.REVIEWER : EvaluationType.COUNCIL_MEMBER)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("trùng vai trò");
        assertThat(evaluations.findByTopicId(f.topic())).isEmpty();
    }
    @Test void legacyConflictBlocksResultsAndCompletionWithoutDeletingBallots() {
        rawAssignmentAndConflict(true);
        tx(() -> {
            Evaluation e = new Evaluation(); e.setTopic(topics.findById(f.topic()).orElseThrow()); e.setEvaluator(users.findById(f.people().get(0)).orElseThrow());
            e.setEvaluationType(EvaluationType.REVIEWER); e.setReviewerAssignment(reviewers.findById(f.reviewerAssignment()).orElseThrow()); e.setStatus(EvaluationStatus.SUBMITTED);
            evaluations.save(e);
            TopicResult r = new TopicResult(); r.setTopic(e.getTopic()); r.setCouncilAssignment(assignments.findByCouncilId(f.council()).getFirst());
            r.setStatus(TopicResultStatus.CONFIRMED); r.setFinalScore(new BigDecimal("8")); results.save(r); return null;
        });
        assertThatThrownBy(() -> resultService.calculateResult(f.topic())).isInstanceOf(BusinessRuleException.class).hasMessageContaining("trùng vai trò");
        auth(f.names().get(1), "LECTURER");
        assertThatThrownBy(() -> resultService.confirmResult(f.topic(), f.names().get(1))).isInstanceOf(BusinessRuleException.class).hasMessageContaining("trùng vai trò");
        auth(f.manager(), "FACULTY_MANAGER");
        assertThatThrownBy(() -> resultService.publishResult(f.topic(), f.manager())).isInstanceOf(BusinessRuleException.class).hasMessageContaining("trùng vai trò");
        assertThatThrownBy(() -> councilService.completeCouncil(f.council())).isInstanceOf(BusinessRuleException.class).hasMessageContaining("trùng vai trò");
        assertThat(evaluations.findByTopicId(f.topic())).hasSize(1);
        assertThat(results.findByTopicId(f.topic()).orElseThrow().getStatus()).isEqualTo(TopicResultStatus.CONFIRMED);
    }
    @Test void legacyConflictIsVisibleAndEscapedInResultAndCouncilPages() throws Exception {
        rawAssignmentAndConflict(true);
        mvc.perform(get("/evaluations/topic/" + f.topic() + "/results").with(user(f.manager()).roles("FACULTY_MANAGER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("trùng vai trò")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;demo&gt;")));
        mvc.perform(get("/councils/" + f.council()).with(user(f.manager()).roles("FACULTY_MANAGER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("trùng vai trò")));
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void concurrentMemberAndReviewerCannotCreateConflict(boolean reviewerFirst) throws Exception {
        assign(); race(reviewerFirst, false);
    }
    @ParameterizedTest @ValueSource(booleans = {true, false})
    void concurrentTopicAndReviewerCannotCreateConflict(boolean reviewerFirst) throws Exception {
        councilService.addMember(f.council(), f.people().get(6), CouncilMemberRole.MEMBER);
        race(reviewerFirst, true);
    }
    void race(boolean reviewerFirst, boolean assignTopic) throws Exception {
        Object repository = reviewerFirst ? reviewers : assignTopic ? assignments : members;
        var advised = (org.springframework.aop.framework.Advised) repository;
        CountDownLatch paused = new CountDownLatch(1), release = new CountDownLatch(1), secondStarted = new CountDownLatch(1);
        org.aopalliance.intercept.MethodInterceptor hold = invocation -> {
            if (Thread.currentThread().getName().equals("first-role-editor") && invocation.getMethod().getName().equals("save")) {
                paused.countDown(); if (!release.await(15, TimeUnit.SECONDS)) throw new AssertionError("First save timed out");
            }
            return invocation.proceed();
        };
        advised.addAdvice(0, hold);
        var pool = Executors.newFixedThreadPool(2);
        Supplier<String> review = () -> asManager(() -> reviewerService.assignReviewer(f.topic(), f.people().get(6), f.manager(), LocalDateTime.now().plusDays(1)));
        Supplier<String> council = () -> asManager(() -> {
            if (assignTopic) councilService.assignTopic(f.council(), f.topic());
            else councilService.addMember(f.council(), f.people().get(6), CouncilMemberRole.MEMBER);
        });
        try {
            Future<String> first = pool.submit(() -> { Thread.currentThread().setName("first-role-editor"); return (reviewerFirst ? review : council).get(); });
            assertThat(paused.await(10, TimeUnit.SECONDS)).isTrue();
            Future<String> second = pool.submit(() -> { secondStarted.countDown(); return (reviewerFirst ? council : review).get(); });
            assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> second.get(250, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            release.countDown();
            assertThat(first.get(15, TimeUnit.SECONDS)).isEqualTo("OK");
            assertThat(second.get(15, TimeUnit.SECONDS)).isEqualTo("REJECTED");
            assertThat(scope.roleConflictReason(f.topic())).isNull();
        } finally {
            release.countDown(); pool.shutdownNow(); pool.awaitTermination(5, TimeUnit.SECONDS); advised.removeAdvice(hold);
        }
    }
    String asManager(Runnable work) {
        auth(f.manager(), "FACULTY_MANAGER");
        try { work.run(); return "OK"; }
        catch (BusinessRuleException expected) { return "REJECTED"; }
        finally { SecurityContextHolder.clearContext(); }
    }
    static void auth(String name, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(name, "unused",
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
