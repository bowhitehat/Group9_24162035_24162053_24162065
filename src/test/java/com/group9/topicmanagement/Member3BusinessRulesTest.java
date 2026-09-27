package com.group9.topicmanagement;

import com.group9.topicmanagement.domain.Department;
import com.group9.topicmanagement.domain.RegistrationPeriod;
import com.group9.topicmanagement.domain.Role;
import com.group9.topicmanagement.domain.User;
import com.group9.topicmanagement.domain.council.Council;
import com.group9.topicmanagement.domain.enums.AnnouncementStatus;
import com.group9.topicmanagement.domain.enums.CouncilMemberRole;
import com.group9.topicmanagement.domain.enums.EvaluationStatus;
import com.group9.topicmanagement.domain.enums.EvaluationType;
import com.group9.topicmanagement.domain.enums.PeriodStatus;
import com.group9.topicmanagement.domain.enums.PeriodType;
import com.group9.topicmanagement.domain.enums.RoleName;
import com.group9.topicmanagement.domain.enums.ReviewerAssignmentStatus;
import com.group9.topicmanagement.domain.enums.TopicResultStatus;
import com.group9.topicmanagement.domain.evaluation.Evaluation;
import com.group9.topicmanagement.domain.evaluation.EvaluationCriterion;
import com.group9.topicmanagement.domain.evaluation.EvaluationScore;
import com.group9.topicmanagement.domain.evaluation.TopicResult;
import com.group9.topicmanagement.domain.studentgroup.StudentGroup;
import com.group9.topicmanagement.domain.topic.Topic;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.repository.*;
import com.group9.topicmanagement.service.*;
import com.group9.topicmanagement.controller.form.TopicForm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Member3BusinessRulesTest {

    @Autowired MockMvc mvc;
    @Autowired UserService userService;
    @Autowired TopicService topicService;
    @Autowired StudentGroupService groupService;
    @Autowired TopicRegistrationService registrationService;
    @Autowired CouncilService councilService;
    @Autowired ReviewerAssignmentService reviewerAssignmentService;
    @Autowired EvaluationService evaluationService;
    @Autowired TopicResultService topicResultService;
    @Autowired AnnouncementService announcementService;

    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired DepartmentRepository departments;
    @Autowired RegistrationPeriodRepository periods;
    @Autowired TopicRepository topics;
    @Autowired StudentGroupRepository groups;
    @Autowired TopicRegistrationRepository registrations;
    @Autowired ReportSubmissionRepository reports;
    @Autowired EvaluationScoreRepository scores;
    @Autowired EvaluationRepository evaluations;
    @Autowired TopicResultRepository results;
    @Autowired ReviewerAssignmentRepository reviewerAssignments;
    @Autowired CouncilAssignmentRepository councilAssignments;
    @Autowired CouncilMemberRepository councilMembers;
    @Autowired CouncilRepository councils;
    @Autowired AnnouncementRepository announcements;
    @Autowired EvaluationCriterionRepository criteria;

    private User lecturer1;
    private User lecturer2;
    private User lecturer3;
    private User lecturer4;
    private User student1;
    private User student2;
    private User facultyManager;
    private Department dept;
    private RegistrationPeriod period;

    @BeforeEach
    void setUp() {
        scores.deleteAll();
        evaluations.deleteAll();
        results.deleteAll();
        reviewerAssignments.deleteAll();
        councilAssignments.deleteAll();
        councilMembers.deleteAll();
        councils.deleteAll();
        announcements.deleteAll();
        criteria.deleteAll();
        reports.deleteAll();
        registrations.deleteAll();
        groups.deleteAll();
        topics.deleteAll();
        periods.deleteAll();
        users.deleteAll();
        roles.deleteAll();
        departments.deleteAll();

        for (RoleName name : RoleName.values()) {
            Role role = new Role();
            role.setName(name);
            roles.save(role);
        }

        dept = new Department();
        dept.setCode("CNPM");
        dept.setName("Công nghệ phần mềm");
        departments.save(dept);

        lecturer1 = userService.create("lecturer1", "GV A", "lec1@test.local", null, dept.getId(), Set.of(RoleName.LECTURER), "Password@123");
        lecturer2 = userService.create("lecturer2", "GV B", "lec2@test.local", null, dept.getId(), Set.of(RoleName.LECTURER), "Password@123");
        lecturer3 = userService.create("lecturer3", "GV C", "lec3@test.local", null, dept.getId(), Set.of(RoleName.LECTURER), "Password@123");
        lecturer4 = userService.create("lecturer4", "GV D", "lec4@test.local", null, dept.getId(), Set.of(RoleName.LECTURER), "Password@123");
        student1 = userService.create("student1", "SV 1", "sv1@test.local", null, dept.getId(), Set.of(RoleName.STUDENT), "Password@123");
        student2 = userService.create("student2", "SV 2", "sv2@test.local", null, dept.getId(), Set.of(RoleName.STUDENT), "Password@123");
        facultyManager = userService.create("faculty1", "Trưởng Khoa", "faculty@test.local", null, dept.getId(), Set.of(RoleName.FACULTY_MANAGER), "Password@123");

        period = new RegistrationPeriod();
        period.setName("Đợt HK1");
        period.setType(PeriodType.MON_HOC);
        LocalDateTime now = LocalDateTime.now();
        period.setLecturerStart(now.minusDays(5));
        period.setLecturerEnd(now.plusDays(5));
        period.setStudentStart(now.minusDays(2));
        period.setStudentEnd(now.plusDays(10));
        period.setStatus(PeriodStatus.IN_PROGRESS);
        period = periods.save(period);
        authenticate(facultyManager.getUsername(), "FACULTY_MANAGER");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void councilMustHaveThreeToFiveMembersAndRoles() {
        Council council = councilService.createCouncil("HĐ 1", period.getId(), LocalDateTime.now().plusDays(7), "A1");
        councilService.addMember(council.getId(), lecturer2.getId(), CouncilMemberRole.CHAIR);
        assertThatThrownBy(() -> councilService.activateCouncil(council.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("từ 3 đến 5");

        councilService.addMember(council.getId(), lecturer3.getId(), CouncilMemberRole.SECRETARY);
        councilService.addMember(council.getId(), lecturer4.getId(), CouncilMemberRole.MEMBER);
        councilService.activateCouncil(council.getId());
        assertThat(councilService.getCouncil(council.getId()).getStatus().name()).isEqualTo("ACTIVE");
    }

    @Test
    void councilRejectsMoreThanFiveDuplicateChairSecretaryAndDuplicateLecturer() {
        Council council = councilService.createCouncil("HĐ 2", period.getId(), LocalDateTime.now().plusDays(7), "A1");
        councilService.addMember(council.getId(), lecturer1.getId(), CouncilMemberRole.CHAIR);
        assertThatThrownBy(() -> councilService.addMember(council.getId(), lecturer2.getId(), CouncilMemberRole.CHAIR))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CHAIR");
        councilService.addMember(council.getId(), lecturer2.getId(), CouncilMemberRole.SECRETARY);
        assertThatThrownBy(() -> councilService.addMember(council.getId(), lecturer3.getId(), CouncilMemberRole.SECRETARY))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("SECRETARY");
        councilService.addMember(council.getId(), lecturer3.getId(), CouncilMemberRole.MEMBER);
        councilService.addMember(council.getId(), lecturer4.getId(), CouncilMemberRole.MEMBER);
        User extra = userService.create("lecturer5", "GV E", "lec5@test.local", null, dept.getId(), Set.of(RoleName.LECTURER), "Password@123");
        User extra2 = userService.create("lecturer6", "GV F", "lec6@test.local", null, dept.getId(), Set.of(RoleName.LECTURER), "Password@123");
        councilService.addMember(council.getId(), extra.getId(), CouncilMemberRole.MEMBER);
        assertThatThrownBy(() -> councilService.addMember(council.getId(), extra2.getId(), CouncilMemberRole.MEMBER))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("tối đa 5");
        assertThatThrownBy(() -> councilService.addMember(council.getId(), lecturer1.getId(), CouncilMemberRole.MEMBER))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("đã có trong hội đồng");
    }

    @Test
    void reviewerAndEvaluatorCannotBeAdvisorAndMustBeAssigned() {
        Topic topic = preparedApprovedTopic();
        assertThatThrownBy(() -> reviewerAssignmentService.assignReviewer(
                topic.getId(), lecturer1.getId(), facultyManager.getUsername(), LocalDateTime.now().plusDays(3)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("không được phản biện đề tài mình đang hướng dẫn");

        reviewerAssignmentService.assignReviewer(topic.getId(), lecturer2.getId(), facultyManager.getUsername(), LocalDateTime.now().plusDays(3));

        authenticate(lecturer3.getUsername(), "LECTURER");
        assertThatThrownBy(() -> evaluationService.saveDraft(topic.getId(), lecturer3.getUsername(), EvaluationType.REVIEWER, List.of(), "note"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("chưa được phân công");

        authenticate(lecturer1.getUsername(), "LECTURER");
        assertThatThrownBy(() -> evaluationService.saveDraft(topic.getId(), lecturer1.getUsername(), EvaluationType.REVIEWER, List.of(), "note"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("hướng dẫn");
    }

    @Test
    void scoreRangeSubmitMandatoryLockPublishAndStudentVisibility() {
        Topic topic = preparedApprovedTopic();
        seedCriteria();
        authenticate(facultyManager.getUsername(), "FACULTY_MANAGER");
        reviewerAssignmentService.assignReviewer(topic.getId(), lecturer2.getId(), facultyManager.getUsername(), LocalDateTime.now().plusDays(3));
        Council council = councilService.createCouncil("HĐ chấm", period.getId(), LocalDateTime.now().plusDays(7), "B1");
        councilService.addMember(council.getId(), lecturer2.getId(), CouncilMemberRole.CHAIR);
        councilService.addMember(council.getId(), lecturer3.getId(), CouncilMemberRole.SECRETARY);
        councilService.addMember(council.getId(), lecturer4.getId(), CouncilMemberRole.MEMBER);
        councilService.assignTopic(council.getId(), topic.getId());
        councilService.activateCouncil(council.getId());

        EvaluationCriterion c1 = criteria.findByRegistrationPeriodIdAndIsActiveTrueOrderByDisplayOrderAsc(period.getId()).get(0);
        EvaluationScore invalid = score(c1, new BigDecimal("11"));
        authenticate(lecturer2.getUsername(), "LECTURER");
        assertThatThrownBy(() -> evaluationService.saveDraft(topic.getId(), lecturer2.getUsername(), EvaluationType.REVIEWER, List.of(invalid), "bad"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("0 đến 10");

        EvaluationScore ok = score(c1, new BigDecimal("8.00"));
        Evaluation draft = evaluationService.saveDraft(topic.getId(), lecturer2.getUsername(), EvaluationType.REVIEWER, List.of(ok), "tốt");
        assertThatThrownBy(() -> evaluationService.submitEvaluation(draft.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("tiêu chí bắt buộc");

        List<EvaluationScore> full = criteria.findByRegistrationPeriodIdAndIsActiveTrueOrderByDisplayOrderAsc(period.getId()).stream()
                .map(c -> score(c, new BigDecimal("8.125")))
                .toList();
        Evaluation submitted = evaluationService.saveDraft(topic.getId(), lecturer2.getUsername(), EvaluationType.REVIEWER, full, "đủ");
        evaluationService.submitEvaluation(submitted.getId());
        
        // Need to submit for council members too to allow publishing
        Evaluation c1Eval = evaluationService.saveDraft(topic.getId(), lecturer2.getUsername(), EvaluationType.COUNCIL_MEMBER, full, "ok");
        evaluationService.submitEvaluation(c1Eval.getId());
        
        authenticate(lecturer3.getUsername(), "LECTURER");
        Evaluation c2Eval = evaluationService.saveDraft(topic.getId(), lecturer3.getUsername(), EvaluationType.COUNCIL_MEMBER, full, "ok");
        evaluationService.submitEvaluation(c2Eval.getId());
        
        authenticate(lecturer4.getUsername(), "LECTURER");
        Evaluation c3Eval = evaluationService.saveDraft(topic.getId(), lecturer4.getUsername(), EvaluationType.COUNCIL_MEMBER, full, "ok");
        evaluationService.submitEvaluation(c3Eval.getId());

        authenticate(facultyManager.getUsername(), "FACULTY_MANAGER");
        evaluationService.lockEvaluation(submitted.getId());
        assertThatThrownBy(() -> {
            authenticate(lecturer2.getUsername(), "LECTURER");
            evaluationService.saveDraft(topic.getId(), lecturer2.getUsername(), EvaluationType.REVIEWER, full, "sửa");
        }).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("khóa");

        authenticate(facultyManager.getUsername(), "FACULTY_MANAGER");
        TopicResult result = topicResultService.calculateResult(topic.getId());
        assertThat(result.getFinalScore()).isEqualTo(new BigDecimal("8.13"));

        authenticate(lecturer3.getUsername(), "LECTURER");
        assertThatThrownBy(() -> topicResultService.confirmResult(topic.getId(), lecturer3.getUsername()))
                .isInstanceOf(AccessDeniedException.class);

        authenticate(lecturer2.getUsername(), "LECTURER");
        topicResultService.confirmResult(topic.getId(), lecturer2.getUsername());

        authenticate(facultyManager.getUsername(), "FACULTY_MANAGER");
        topicResultService.publishResult(topic.getId(), facultyManager.getUsername());
        assertThatThrownBy(() -> topicResultService.calculateResult(topic.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("đã công bố");

        authenticate(student1.getUsername(), "STUDENT");
        TopicResult published = topicResultService.getPublishedResultForStudent(topic.getId(), student1.getUsername());
        assertThat(published.getStatus()).isEqualTo(TopicResultStatus.PUBLISHED);

        authenticate(student2.getUsername(), "STUDENT");
        assertThatThrownBy(() -> topicResultService.getPublishedResultForStudent(topic.getId(), student2.getUsername()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void studentCannotSeeUnpublishedResultAndUnauthorizedGets403() throws Exception {
        Topic topic = preparedApprovedTopic();
        mvc.perform(get("/evaluations/results").with(user("student1").roles("STUDENT")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/evaluations/my-result").param("topicId", String.valueOf(topic.getId()))
                        .with(user("student1").roles("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void announcementIsRoleFilteredAndDraftHidden() {
        authenticate(facultyManager.getUsername(), "FACULTY_MANAGER");
        announcementService.saveAnnouncement("Cho SV", "<script>x</script>hello", List.of("STUDENT"), facultyManager.getUsername(), true, null);
        announcementService.saveAnnouncement("Nháp", "draft", List.of("LECTURER"), facultyManager.getUsername(), false, null);

        assertThat(announcementService.getActiveAnnouncementsForRole("STUDENT"))
                .extracting(a -> a.getTitle())
                .contains("Cho SV")
                .doesNotContain("Nháp");
        assertThat(announcementService.getActiveAnnouncementsForRole("LECTURER"))
                .extracting(a -> a.getTitle())
                .doesNotContain("Cho SV");
        assertThat(announcementService.getActiveAnnouncementsForRole("STUDENT").get(0).getContent())
                .contains("&lt;script&gt;");
        assertThat(announcementService.listForManager().stream().anyMatch(a -> a.getStatus() == AnnouncementStatus.DRAFT)).isTrue();
    }

    @Test
    void lecturerCannotOpenUnassignedCouncilOrEvaluationForm() throws Exception {
        Topic topic = preparedApprovedTopic();
        User outsider = userService.create("lecturer-outside", "GV ngoài", "outside@test.local", null,
                dept.getId(), Set.of(RoleName.LECTURER), "Password@123");
        authenticate(facultyManager.getUsername(), "FACULTY_MANAGER");
        Council council = councilService.createCouncil("HĐ bảo mật", period.getId(), LocalDateTime.now().plusDays(3), "B2");
        councilService.addMember(council.getId(), lecturer2.getId(), CouncilMemberRole.CHAIR);
        councilService.addMember(council.getId(), lecturer3.getId(), CouncilMemberRole.SECRETARY);
        councilService.addMember(council.getId(), lecturer4.getId(), CouncilMemberRole.MEMBER);
        councilService.assignTopic(council.getId(), topic.getId());
        reviewerAssignmentService.assignReviewer(topic.getId(), lecturer2.getId(),
                facultyManager.getUsername(), LocalDateTime.now().plusDays(2));

        mvc.perform(get("/councils/{id}", council.getId())
                        .with(user(lecturer1.getUsername()).roles("LECTURER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/evaluations/topic/{id}", topic.getId())
                        .param("type", "REVIEWER")
                        .with(user(lecturer3.getUsername()).roles("LECTURER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/topics/detail/{id}", topic.getId())
                        .with(user(outsider.getUsername()).roles("LECTURER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/topics/detail/{id}", topic.getId())
                        .with(user(lecturer3.getUsername()).roles("LECTURER")))
                .andExpect(status().isOk());
        assertThat(topicService.findTopicsForLecturer(lecturer3.getId(), period.getId(), null,
                null, null, org.springframework.data.domain.PageRequest.of(0, 10)).getContent())
                .extracting(Topic::getId).contains(topic.getId());
        mvc.perform(get("/councils/{id}", council.getId())
                        .with(user(lecturer2.getUsername()).roles("LECTURER")))
                .andExpect(status().isOk());
        mvc.perform(get("/evaluations/topic/{id}", topic.getId())
                        .param("type", "REVIEWER")
                        .with(user(lecturer2.getUsername()).roles("LECTURER")))
                .andExpect(status().isOk());
        assertThat(reviewerAssignments.findByTopicIdAndReviewerId(topic.getId(), lecturer2.getId())
                .orElseThrow().getStatus()).isEqualTo(ReviewerAssignmentStatus.IN_PROGRESS);
    }

    @Test
    void lecturerCannotSubmitAnotherLecturersEvaluation() {
        Topic topic = preparedApprovedTopic();
        seedCriteria();
        authenticate(facultyManager.getUsername(), "FACULTY_MANAGER");
        reviewerAssignmentService.assignReviewer(topic.getId(), lecturer2.getId(),
                facultyManager.getUsername(), LocalDateTime.now().plusDays(2));
        authenticate(lecturer2.getUsername(), "LECTURER");
        Evaluation draft = evaluationService.saveDraft(topic.getId(), lecturer2.getUsername(),
                EvaluationType.REVIEWER, fullScores("7.50"), "draft");

        authenticate(lecturer3.getUsername(), "LECTURER");
        assertThatThrownBy(() -> evaluationService.submitEvaluation(draft.getId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("phiếu chấm của mình");
    }

    @Test
    void submitIsBlockedAfterReviewerDeadline() {
        Topic topic = preparedApprovedTopic();
        seedCriteria();
        authenticate(facultyManager.getUsername(), "FACULTY_MANAGER");
        var assignment = reviewerAssignmentService.assignReviewer(topic.getId(), lecturer2.getId(),
                facultyManager.getUsername(), LocalDateTime.now().plusDays(2));
        authenticate(lecturer2.getUsername(), "LECTURER");
        Evaluation draft = evaluationService.saveDraft(topic.getId(), lecturer2.getUsername(),
                EvaluationType.REVIEWER, fullScores("8.00"), "draft");
        assignment.setDeadline(LocalDateTime.now().minusMinutes(1));
        assignment.setStatus(ReviewerAssignmentStatus.IN_PROGRESS);
        reviewerAssignments.save(assignment);

        assertThatThrownBy(() -> evaluationService.submitEvaluation(draft.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("quá hạn");
    }

    @Test
    void editingSubmittedEvaluationReturnsItToDraftUntilResubmitted() {
        Topic topic = preparedApprovedTopic();
        seedCriteria();
        authenticate(facultyManager.getUsername(), "FACULTY_MANAGER");
        reviewerAssignmentService.assignReviewer(topic.getId(), lecturer2.getId(),
                facultyManager.getUsername(), LocalDateTime.now().plusDays(2));
        authenticate(lecturer2.getUsername(), "LECTURER");
        Evaluation evaluation = evaluationService.saveDraft(topic.getId(), lecturer2.getUsername(),
                EvaluationType.REVIEWER, fullScores("8.00"), "lần đầu");
        evaluationService.submitEvaluation(evaluation.getId());

        Evaluation edited = evaluationService.saveDraft(topic.getId(), lecturer2.getUsername(),
                EvaluationType.REVIEWER, fullScores("9.00"), "đã sửa");

        assertThat(edited.getStatus()).isEqualTo(EvaluationStatus.DRAFT);
        assertThat(edited.getSubmissionTime()).isNull();
        assertThat(reviewerAssignments.findByTopicIdAndReviewerId(topic.getId(), lecturer2.getId())
                .orElseThrow().getStatus()).isEqualTo(ReviewerAssignmentStatus.IN_PROGRESS);
    }

    @Test
    void councilGradingRequiresActiveCouncilAndStopsWhenCompleted() {
        Topic topic = preparedApprovedTopic();
        seedCriteria();
        authenticate(facultyManager.getUsername(), "FACULTY_MANAGER");
        Council council = createCouncilWithMembersAndTopic(topic);
        authenticate(lecturer2.getUsername(), "LECTURER");
        assertThatThrownBy(() -> evaluationService.saveDraft(topic.getId(), lecturer2.getUsername(),
                EvaluationType.COUNCIL_MEMBER, fullScores("8.00"), "draft council"))
                .isInstanceOf(BusinessRuleException.class);

        authenticate(facultyManager.getUsername(), "FACULTY_MANAGER");
        councilService.activateCouncil(council.getId());
        authenticate(lecturer2.getUsername(), "LECTURER");
        evaluationService.saveDraft(topic.getId(), lecturer2.getUsername(),
                EvaluationType.COUNCIL_MEMBER, fullScores("8.00"), "active council");
        authenticate(facultyManager.getUsername(), "FACULTY_MANAGER");
        councilService.completeCouncil(council.getId());
        authenticate(lecturer2.getUsername(), "LECTURER");
        assertThatThrownBy(() -> evaluationService.saveDraft(topic.getId(), lecturer2.getUsername(),
                EvaluationType.COUNCIL_MEMBER, fullScores("9.00"), "late edit"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void councilRejectsNonLecturerAndCannotChangeStructureAfterActivation() {
        Topic topic = preparedApprovedTopic();
        authenticate(facultyManager.getUsername(), "FACULTY_MANAGER");
        Council council = councilService.createCouncil("HĐ luật", period.getId(), LocalDateTime.now().plusDays(3), "B3");
        assertThatThrownBy(() -> councilService.addMember(council.getId(), student1.getId(), CouncilMemberRole.MEMBER))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("vai trò Giảng viên");
        councilService.addMember(council.getId(), lecturer2.getId(), CouncilMemberRole.CHAIR);
        councilService.addMember(council.getId(), lecturer3.getId(), CouncilMemberRole.SECRETARY);
        councilService.addMember(council.getId(), lecturer4.getId(), CouncilMemberRole.MEMBER);
        councilService.assignTopic(council.getId(), topic.getId());
        councilService.activateCouncil(council.getId());

        assertThatThrownBy(() -> councilService.addMember(council.getId(), lecturer1.getId(), CouncilMemberRole.MEMBER))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("đã kích hoạt");
        Long assignmentId = councilAssignments.findByCouncilId(council.getId()).get(0).getId();
        assertThatThrownBy(() -> councilService.removeTopic(council.getId(), assignmentId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("đã kích hoạt");
    }

    @Test
    void wrongPeriodAndInactiveCriteriaAreRejected() {
        Topic topic = preparedApprovedTopic();
        authenticate(facultyManager.getUsername(), "FACULTY_MANAGER");
        reviewerAssignmentService.assignReviewer(topic.getId(), lecturer2.getId(),
                facultyManager.getUsername(), LocalDateTime.now().plusDays(2));

        RegistrationPeriod anotherPeriod = new RegistrationPeriod();
        anotherPeriod.setName("Đợt khác");
        anotherPeriod.setType(PeriodType.MON_HOC);
        anotherPeriod.setLecturerStart(LocalDateTime.now().minusDays(1));
        anotherPeriod.setLecturerEnd(LocalDateTime.now().plusDays(1));
        anotherPeriod.setStudentStart(LocalDateTime.now().minusDays(1));
        anotherPeriod.setStudentEnd(LocalDateTime.now().plusDays(1));
        anotherPeriod.setStatus(PeriodStatus.IN_PROGRESS);
        anotherPeriod = periods.save(anotherPeriod);
        EvaluationCriterion wrongPeriod = criterion("Sai đợt", anotherPeriod, true);
        EvaluationCriterion inactive = criterion("Ngừng dùng", period, false);

        authenticate(lecturer2.getUsername(), "LECTURER");
        assertThatThrownBy(() -> evaluationService.saveDraft(topic.getId(), lecturer2.getUsername(),
                EvaluationType.REVIEWER, List.of(score(wrongPeriod, BigDecimal.TEN)), "wrong"))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("không thuộc đợt");
        assertThatThrownBy(() -> evaluationService.saveDraft(topic.getId(), lecturer2.getUsername(),
                EvaluationType.REVIEWER, List.of(score(inactive, BigDecimal.TEN)), "inactive"))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("không còn hoạt động");
    }

    @Test
    void draftCouncilCannotProduceResultAndAnnouncementsSupportMultipleRoles() {
        Topic topic = preparedApprovedTopic();
        authenticate(facultyManager.getUsername(), "FACULTY_MANAGER");
        createCouncilWithMembersAndTopic(topic);
        assertThatThrownBy(() -> topicResultService.calculateResult(topic.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("chưa được gán vào hội đồng");

        announcementService.saveAnnouncement("Cho SV", "sv", List.of("STUDENT"),
                facultyManager.getUsername(), true, null);
        announcementService.saveAnnouncement("Cho GV", "gv", List.of("LECTURER"),
                facultyManager.getUsername(), true, null);
        assertThat(announcementService.getActiveAnnouncementsForRoles(Set.of("STUDENT", "LECTURER")))
                .extracting(a -> a.getTitle()).containsExactlyInAnyOrder("Cho SV", "Cho GV");
        assertThatThrownBy(() -> announcementService.saveAnnouncement("Rỗng", "x", List.of(),
                facultyManager.getUsername(), true, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ít nhất một vai trò");
    }

    @Test
    void resultCannotBeCalculatedByLecturerOrPublishedWithMissingCouncilBallots() {
        Topic topic = preparedApprovedTopic();
        seedCriteria();
        authenticate(facultyManager.getUsername(), "FACULTY_MANAGER");
        Council council = createCouncilWithMembersAndTopic(topic);
        councilService.activateCouncil(council.getId());

        authenticate(lecturer2.getUsername(), "LECTURER");
        assertThatThrownBy(() -> topicResultService.calculateResult(topic.getId()))
                .isInstanceOf(AccessDeniedException.class);
        Evaluation oneBallot = evaluationService.saveDraft(topic.getId(), lecturer2.getUsername(),
                EvaluationType.COUNCIL_MEMBER, fullScores("8.00"), "only one");
        evaluationService.submitEvaluation(oneBallot.getId());

        authenticate(facultyManager.getUsername(), "FACULTY_MANAGER");
        TopicResult result = new TopicResult();
        result.setTopic(topic);
        result.setCouncilAssignment(councilAssignments.findByCouncilId(council.getId()).get(0));
        result.setFinalScore(new BigDecimal("8.00"));
        result.setStatus(TopicResultStatus.CONFIRMED);
        results.save(result);
        assertThatThrownBy(() -> topicResultService.publishResult(topic.getId(), facultyManager.getUsername()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("chưa nộp điểm");
    }

    @Test
    void reviewerMustBeLecturerAndManagerFormsRender() throws Exception {
        Topic topic = preparedApprovedTopic();
        authenticate(facultyManager.getUsername(), "FACULTY_MANAGER");
        assertThatThrownBy(() -> reviewerAssignmentService.assignReviewer(topic.getId(), student2.getId(),
                facultyManager.getUsername(), LocalDateTime.now().plusDays(2)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("vai trò Giảng viên");

        mvc.perform(get("/announcements/create")
                        .with(user(facultyManager.getUsername()).roles("FACULTY_MANAGER")))
                .andExpect(status().isOk());
        mvc.perform(get("/councils/create")
                        .with(user(facultyManager.getUsername()).roles("FACULTY_MANAGER")))
                .andExpect(status().isOk());
    }

    @Test
    void roundingUsesHalfUpTwoDecimals() {
        BigDecimal avg = new BigDecimal("8.125").add(new BigDecimal("8.125"))
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        assertThat(avg).isEqualTo(new BigDecimal("8.13"));
    }

    private Topic preparedApprovedTopic() {
        TopicForm form = new TopicForm();
        form.setCode("DT99");
        form.setTitle("Đề tài chấm điểm");
        form.setDepartmentId(dept.getId());
        form.setRegistrationPeriodId(period.getId());
        form.setAdvisorIds(List.of(lecturer1.getId()));
        Topic topic = topicService.createTopic(form, lecturer1.getUsername());
        topicService.submitTopic(topic.getId(), lecturer1.getUsername());
        topicService.approveTopic(topic.getId());
        topicService.publishTopic(topic.getId());
        period.setStatus(PeriodStatus.STUDENT_REGISTRATION);
        periods.save(period);
        StudentGroup group = groupService.createGroup(period.getId(), student1.getUsername());
        var reg = registrationService.registerTopic(group.getId(), topic.getId(), student1.getUsername());
        registrationService.approveRegistration(reg.getId(), facultyManager.getUsername());
        period.setStatus(PeriodStatus.IN_PROGRESS);
        periods.save(period);
        return topicService.getTopicById(topic.getId());
    }

    private void seedCriteria() {
        EvaluationCriterion c1 = new EvaluationCriterion();
        c1.setName("Nội dung");
        c1.setRegistrationPeriod(period);
        c1.setDisplayOrder(1);
        c1.setIsMandatory(true);
        c1.setIsActive(true);
        criteria.save(c1);
        EvaluationCriterion c2 = new EvaluationCriterion();
        c2.setName("Kỹ thuật");
        c2.setRegistrationPeriod(period);
        c2.setDisplayOrder(2);
        c2.setIsMandatory(true);
        c2.setIsActive(true);
        criteria.save(c2);
    }

    private List<EvaluationScore> fullScores(String value) {
        return criteria.findByRegistrationPeriodIdAndIsActiveTrueOrderByDisplayOrderAsc(period.getId()).stream()
                .map(c -> score(c, new BigDecimal(value)))
                .toList();
    }

    private Council createCouncilWithMembersAndTopic(Topic topic) {
        Council council = councilService.createCouncil("HĐ kiểm thử", period.getId(), LocalDateTime.now().plusDays(3), "B4");
        councilService.addMember(council.getId(), lecturer2.getId(), CouncilMemberRole.CHAIR);
        councilService.addMember(council.getId(), lecturer3.getId(), CouncilMemberRole.SECRETARY);
        councilService.addMember(council.getId(), lecturer4.getId(), CouncilMemberRole.MEMBER);
        councilService.assignTopic(council.getId(), topic.getId());
        return council;
    }

    private EvaluationCriterion criterion(String name, RegistrationPeriod registrationPeriod, boolean active) {
        EvaluationCriterion criterion = new EvaluationCriterion();
        criterion.setName(name);
        criterion.setRegistrationPeriod(registrationPeriod);
        criterion.setDisplayOrder(99);
        criterion.setIsMandatory(true);
        criterion.setIsActive(active);
        return criteria.save(criterion);
    }

    private EvaluationScore score(EvaluationCriterion criterion, BigDecimal value) {
        EvaluationScore score = new EvaluationScore();
        score.setCriterion(criterion);
        score.setScore(value);
        return score;
    }

    private void authenticate(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
