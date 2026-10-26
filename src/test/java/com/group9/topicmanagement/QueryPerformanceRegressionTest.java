package com.group9.topicmanagement;

import com.group9.topicmanagement.model.*;
import com.group9.topicmanagement.model.council.*;
import com.group9.topicmanagement.model.enums.*;
import com.group9.topicmanagement.model.evaluation.ReviewerAssignment;
import com.group9.topicmanagement.model.registration.TopicRegistration;
import com.group9.topicmanagement.model.studentgroup.*;
import com.group9.topicmanagement.model.topic.*;
import com.group9.topicmanagement.repository.*;
import com.group9.topicmanagement.service.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.config.import=", "app.mail.enabled=false", "app.demo-password=",
        "spring.datasource.url=jdbc:h2:mem:group9_query_regression;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.properties.hibernate.query.fail_on_pagination_over_collection_fetch=true",
        "logging.level.org.hibernate.stat=OFF", "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF"
})
@ActiveProfiles("test")
@Transactional
class QueryPerformanceRegressionTest {
    @Autowired EntityManager em;
    @Autowired EntityManagerFactory emf;
    @Autowired RoleRepository roles;
    @Autowired DepartmentRepository departments;
    @Autowired TopicRepository topics;
    @Autowired TopicService topicService;
    @Autowired TopicRegistrationService registrations;
    @Autowired ReviewerAssignmentService reviewerService;
    @Autowired UserService userService;
    @Autowired CouncilService councilService;
    private RegistrationPeriod period;
    private Department department;
    private User proposer, lecturer;
    private final List<Long> topicIds = new ArrayList<>(), registrationIds = new ArrayList<>();
    private final List<String> studentNames = new ArrayList<>();
    private Statistics statistics;
    private String prefix;

    @BeforeEach void fixtures() {
        prefix = UUID.randomUUID().toString().substring(0, 8);
        department = departments.findByCodeIgnoreCase("CNPM").orElseThrow();
        proposer = actor("proposer", RoleName.LECTURER);
        lecturer = actor("lecturer", RoleName.LECTURER);
        period = new RegistrationPeriod(); period.setName(prefix); period.setType(PeriodType.MON_HOC);
        period.setStatus(PeriodStatus.IN_PROGRESS);
        period.setLecturerStart(LocalDateTime.now().minusDays(10)); period.setLecturerEnd(LocalDateTime.now().minusDays(8));
        period.setStudentStart(LocalDateTime.now().minusDays(7)); period.setStudentEnd(LocalDateTime.now().minusDays(5));
        em.persist(period);
        for (int i = 0; i < 30; i++) {
            var topic = topic("T" + i, TopicStatus.PUBLISHED);
            var student = actor("student" + i, RoleName.STUDENT); studentNames.add(student.getUsername());
            var group = new StudentGroup(); group.setLeader(student); group.setRegistrationPeriod(period);
            var member = new GroupMember(); member.setGroup(group); member.setMember(student); member.setLeader(true);
            group.getMembers().add(member); em.persist(group);
            var registration = new TopicRegistration(); registration.setTopic(topic); registration.setStudentGroup(group);
            registration.setRegistrationPeriod(period); registration.setStatus(RegistrationStatus.PENDING); em.persist(registration);
            topicIds.add(topic.getId()); registrationIds.add(registration.getId());
        }
        statistics = emf.unwrap(SessionFactory.class).getStatistics(); resetReads();
    }

    @Test void topicPaginationLoadsOnlyRequestedPageAndHydratesAdvisors() {
        var page = topicService.findTopicsWithFilters(period.getId(), null, null, null, PageRequest.of(0, 1));
        assertThat(page.getTotalElements()).isEqualTo(30);
        assertThat(page.getContent()).extracting(Topic::getId).containsExactly(topicIds.get(29));
        assertThat(statistics.getEntityStatistics(Topic.class.getName()).getLoadCount()).isEqualTo(1);
        assertThat(Hibernate.isInitialized(page.getContent().getFirst().getAdvisors())).isTrue();
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }

    @Test void registrationPaginationLoadsOnlyPageAndHydratesMembers() {
        var page = registrations.findRegistrationsWithFilters(period.getId(), department.getId(), null, null, PageRequest.of(1, 1));
        assertThat(page.getTotalElements()).isEqualTo(30);
        assertThat(page.getContent()).extracting(TopicRegistration::getId).containsExactly(registrationIds.get(28));
        assertThat(statistics.getEntityStatistics(TopicRegistration.class.getName()).getLoadCount()).isEqualTo(1);
        assertThat(Hibernate.isInitialized(page.getContent().getFirst().getStudentGroup().getMembers())).isTrue();
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }

    @Test void studentRegistrationScopeIsAppliedBeforePaginationAndCount() {
        var page = registrations.findRegistrationsForStudent(studentNames.get(7).toUpperCase(Locale.ROOT),
                period.getId(), RegistrationStatus.PENDING, null, PageRequest.of(0, 1));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(TopicRegistration::getId).containsExactly(registrationIds.get(7));
        assertThat(statistics.getEntityStatistics(TopicRegistration.class.getName()).getLoadCount()).isEqualTo(1);
    }

    @Test void lecturerScopeIncludesProposerAdvisorReviewerAndCouncilButNotCancelledOrUnrelated() {
        User target = em.getReference(User.class, lecturer.getId());
        var own = topic("own", TopicStatus.DRAFT); own.setProposer(target);
        var advised = topic("advised", TopicStatus.DRAFT); advised.getAdvisors().add(target);
        var reviewed = topic("reviewed", TopicStatus.DRAFT); reviewer(reviewed, target, ReviewerAssignmentStatus.ASSIGNED, false);
        var councilTopic = topic("council", TopicStatus.DRAFT); council(councilTopic, target, CouncilStatus.DRAFT);
        reviewer(topic("cancelPB", TopicStatus.DRAFT), target, ReviewerAssignmentStatus.CANCELLED, false);
        council(topic("cancelHD", TopicStatus.DRAFT), target, CouncilStatus.CANCELLED);
        var ids = List.of(own.getId(), advised.getId(), reviewed.getId(), councilTopic.getId()); resetReads();
        var page = topicService.findTopicsForLecturer(target.getId(), period.getId(), null, null, null, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getContent()).extracting(Topic::getId).containsExactlyElementsOf(ids.reversed());
        assertThat(statistics.getEntityStatistics(Topic.class.getName()).getLoadCount()).isEqualTo(4);
    }

    @Test void studentTopicsExcludeDraftAndKeywordWildcardsRemainLiteral() {
        topic("hidden", TopicStatus.DRAFT);
        var literal = topic("percent", TopicStatus.PUBLISHED); literal.setTitle("Exact 100%_! match"); resetReads();
        var page = topicService.findAvailableTopicsForStudents(period.getId(), null, "100%_!", PageRequest.of(0, 1));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(Topic::getId).containsExactly(literal.getId());
        assertThat(topicService.findAvailableTopicsForStudents(period.getId(), null, null, PageRequest.of(0, 1)).getTotalElements()).isEqualTo(31);
    }

    @Test void reviewerFiltersUseEffectiveOverdueStatusAndRetainCombinedPendingFilter() {
        var overdue = reviewer(em.getReference(Topic.class, topicIds.get(0)), em.getReference(User.class, lecturer.getId()), ReviewerAssignmentStatus.ASSIGNED, true);
        reviewer(em.getReference(Topic.class, topicIds.get(1)), em.getReference(User.class, lecturer.getId()), ReviewerAssignmentStatus.SUBMITTED, false);
        resetReads();
        assertThat(reviewerService.listAssignments(period.getId(), null, lecturer.getId(), ReviewerAssignmentStatus.OVERDUE, false, true))
                .extracting(ReviewerAssignment::getId).containsExactly(overdue.getId());
        assertThat(reviewerService.listAssignments(period.getId(), null, lecturer.getId(), ReviewerAssignmentStatus.ASSIGNED, false, false)).isEmpty();
        assertThat(reviewerService.listAssignments(period.getId(), null, lecturer.getId(), ReviewerAssignmentStatus.SUBMITTED, false, true)).isEmpty();
    }

    @Test void withoutReviewerUsesDatabaseExistenceAndIgnoresCancelledAssignments() {
        var first = em.find(TopicRegistration.class, registrationIds.get(0)); first.setStatus(RegistrationStatus.APPROVED);
        var second = em.find(TopicRegistration.class, registrationIds.get(1)); second.setStatus(RegistrationStatus.APPROVED);
        reviewer(first.getTopic(), em.getReference(User.class, lecturer.getId()), ReviewerAssignmentStatus.CANCELLED, false);
        reviewer(second.getTopic(), em.getReference(User.class, lecturer.getId()), ReviewerAssignmentStatus.ASSIGNED, false);
        resetReads();
        assertThat(reviewerService.topicsWithoutReviewer()).extracting(TopicRegistration::getId).containsExactly(first.getId());
        assertThat(statistics.getEntityStatistics(TopicRegistration.class.getName()).getLoadCount()).isEqualTo(1);
    }

    @Test void roleQueryDoesNotMaterializeStudentsAndRetainsCompleteRoles() {
        assertThat(userService.findUsersByRole("LECTURER")).hasSize(2).allSatisfy(user -> {
            assertThat(Hibernate.isInitialized(user.getRoles())).isTrue();
            assertThat(user.getRoles()).extracting(Role::getName).contains(RoleName.LECTURER);
        });
        assertThat(statistics.getEntityStatistics(User.class.getName()).getLoadCount()).isEqualTo(2);
    }

    @Test void councilDetailReusesReadsAndKeepsActivationAndCancellationRules() {
        var c = draftCouncil();
        var assignment = new CouncilAssignment(); assignment.setCouncil(c);
        assignment.setTopic(em.getReference(Topic.class, topicIds.getFirst())); em.persist(assignment);
        Long id = c.getId(); resetReads();
        var detail = councilService.detailForViewer(id, "manager", true);
        assertThat(detail.members()).hasSize(3);
        assertThat(detail.assignments()).hasSize(1);
        assertThat(detail.canActivate()).isTrue();
        assertThat(detail.canCancel()).isTrue();
        assertThat(detail.canGrade()).isFalse();
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(5);
        assertThat(statistics.getEntityStatistics(TopicRegistration.class.getName()).getLoadCount()).isZero();

        var ballot = new com.group9.topicmanagement.model.evaluation.Evaluation();
        ballot.setTopic(em.getReference(Topic.class, topicIds.getFirst()));
        ballot.setEvaluator(detail.members().getFirst().getMember());
        ballot.setCouncilMember(detail.members().getFirst()); ballot.setEvaluationType(EvaluationType.COUNCIL_MEMBER);
        em.persist(ballot); resetReads();
        assertThat(councilService.detailForViewer(id, "manager", true).canCancel()).isFalse();
    }

    @Test void councilDetailStillRejectsUnrelatedLecturerAndLimitsMemberActions() {
        Long id = draftCouncil().getId(); resetReads();
        assertThatThrownBy(() -> councilService.detailForViewer(id, proposer.getUsername(), false))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        var detail = councilService.detailForViewer(id, lecturer.getUsername(), false);
        assertThat(detail.canEditStructure()).isFalse();
        assertThat(detail.canActivate()).isFalse();
        assertThat(detail.canComplete()).isFalse();
        assertThat(detail.canCancel()).isFalse();
    }

    @Test void councilTopicPickerFiltersPeriodAndStatusBeforeLoadingEntities() {
        var first = em.find(TopicRegistration.class, registrationIds.getFirst()); first.setStatus(RegistrationStatus.APPROVED);
        var other = new RegistrationPeriod(); other.setName(prefix + "other"); other.setType(PeriodType.MON_HOC);
        other.setLecturerStart(LocalDateTime.now().minusDays(3)); other.setLecturerEnd(LocalDateTime.now().minusDays(2));
        other.setStudentStart(LocalDateTime.now().minusDays(1)); other.setStudentEnd(LocalDateTime.now().plusDays(1));
        em.persist(other);
        var unrelated = em.find(TopicRegistration.class, registrationIds.get(1));
        unrelated.setStatus(RegistrationStatus.APPROVED); unrelated.setRegistrationPeriod(other);
        Long expected = first.getId(); resetReads();
        var options = registrations.listApprovedRegistrationsForPeriod(period.getId());
        assertThat(options).extracting(TopicRegistration::getId).containsExactly(expected);
        assertThat(Hibernate.isInitialized(options.getFirst().getTopic())).isTrue();
        assertThat(statistics.getEntityStatistics(TopicRegistration.class.getName()).getLoadCount()).isEqualTo(1);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    private Council draftCouncil() {
        var c = new Council(); c.setName(prefix); c.setRegistrationPeriod(period);
        c.setLocation("QA"); c.setReportDate(LocalDateTime.now().plusDays(1)); c.setStatus(CouncilStatus.DRAFT); em.persist(c);
        var people = List.of(em.getReference(User.class, lecturer.getId()), actor("secretary", RoleName.LECTURER),
                actor("member", RoleName.LECTURER));
        var positions = List.of(CouncilMemberRole.CHAIR, CouncilMemberRole.SECRETARY, CouncilMemberRole.MEMBER);
        for (int i = 0; i < people.size(); i++) {
            var m = new CouncilMember(); m.setCouncil(c); m.setMember(people.get(i)); m.setRole(positions.get(i)); em.persist(m);
        }
        return c;
    }

    private User actor(String suffix, RoleName role) {
        var user = new User(); user.setUsername(prefix + suffix); user.setFullName(suffix);
        user.setEmail(prefix + suffix + "@example.test"); user.setPasswordHash("unused-test-hash"); user.setDepartment(department);
        user.getRoles().add(roles.findByName(role).orElseThrow()); em.persist(user); return user;
    }
    private Topic topic(String suffix, TopicStatus status) {
        var topic = new Topic(); topic.setCode(prefix + suffix); topic.setTitle(suffix);
        topic.setProposer(em.getReference(User.class, proposer.getId())); topic.getAdvisors().add(em.getReference(User.class, proposer.getId()));
        topic.setDepartment(department); topic.setRegistrationPeriod(period); topic.setStatus(status); em.persist(topic); return topic;
    }
    private ReviewerAssignment reviewer(Topic topic, User actor, ReviewerAssignmentStatus status, boolean past) {
        var assignment = new ReviewerAssignment(); assignment.setTopic(topic); assignment.setReviewer(actor); assignment.setStatus(status);
        assignment.setDeadline(LocalDateTime.now().plusDays(past ? -1 : 1)); em.persist(assignment); return assignment;
    }
    private void council(Topic topic, User actor, CouncilStatus status) {
        var council = new Council(); council.setName(prefix); council.setRegistrationPeriod(period); council.setStatus(status);
        council.setLocation("QA"); council.setReportDate(LocalDateTime.now().plusDays(1)); em.persist(council);
        var assignment = new CouncilAssignment(); assignment.setTopic(topic); assignment.setCouncil(council); em.persist(assignment);
        var member = new CouncilMember(); member.setCouncil(council); member.setMember(actor); member.setRole(CouncilMemberRole.MEMBER); em.persist(member);
    }
    private void resetReads() { em.flush(); em.clear(); statistics.clear(); }
}
