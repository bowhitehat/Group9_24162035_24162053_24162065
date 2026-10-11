package com.group9.topicmanagement;

import com.group9.topicmanagement.dto.AnnouncementForm;
import com.group9.topicmanagement.model.evaluation.TopicResult;
import com.group9.topicmanagement.model.enums.TopicResultStatus;
import com.group9.topicmanagement.model.topic.Topic;
import com.group9.topicmanagement.service.EmailService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class Member3RegressionTest extends Member3BusinessRulesTest {
    @MockitoBean EmailService mail;
    @Autowired PlatformTransactionManager transactions;

    @Test void emptyCouncilCannotActivateAndUiExplainsPrerequisites() throws Exception {
        var council = councilService.createCouncil("Hội đồng rỗng", periods.findAll().get(0).getId(),
                LocalDateTime.now().plusDays(3), "A1");
        for (int i = 2; i <= 4; i++) {
            var role = i == 2 ? com.group9.topicmanagement.model.enums.CouncilMemberRole.CHAIR
                    : i == 3 ? com.group9.topicmanagement.model.enums.CouncilMemberRole.SECRETARY
                    : com.group9.topicmanagement.model.enums.CouncilMemberRole.MEMBER;
            councilService.addMember(council.getId(), users.findByUsernameIgnoreCase("lecturer" + i).orElseThrow().getId(), role);
        }
        assertThat(councilService.canActivate(council, true)).isFalse();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> councilService.activateCouncil(council.getId()))
                .isInstanceOf(com.group9.topicmanagement.exception.BusinessRuleException.class)
                .hasMessageContaining("ít nhất một đề tài");
        mvc.perform(get("/councils/" + council.getId()).with(user("faculty1").roles("FACULTY_MANAGER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("ít nhất một đề tài")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(
                        "action=\"/councils/" + council.getId() + "/activate\""))));
    }

    @Test void incompleteCouncilCannotCompleteOrLoseGradingAccess() {
        Topic topic = preparedApprovedTopic();
        seedCriteria();
        var council = createCouncilWithMembersAndTopic(topic);
        councilService.activateCouncil(council.getId());
        assertThat(councilService.canComplete(councilService.getCouncil(council.getId()), true)).isFalse();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> councilService.completeCouncil(council.getId()))
                .isInstanceOf(com.group9.topicmanagement.exception.BusinessRuleException.class);
        authenticate("lecturer2", "LECTURER");
        var ballot = evaluationService.saveDraft(topic.getId(), "lecturer2",
                com.group9.topicmanagement.model.enums.EvaluationType.COUNCIL_MEMBER, fullScores("8.00"), "1/3");
        evaluationService.submitEvaluation(ballot.getId());
        authenticate("faculty1", "FACULTY_MANAGER");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> councilService.completeCouncil(council.getId()))
                .isInstanceOf(com.group9.topicmanagement.exception.BusinessRuleException.class);
        assertThat(councilService.getCouncil(council.getId()).getStatus())
                .isEqualTo(com.group9.topicmanagement.model.enums.CouncilStatus.ACTIVE);
        assertThat(councilService.canGradeCouncil(council.getId(), "lecturer3")).isTrue();
    }

    @Test void completionRequiresMandatoryScoresAndCanCompleteMatchesBackend() {
        Topic topic = preparedApprovedTopic();
        seedCriteria();
        var council = createCouncilWithMembersAndTopic(topic);
        councilService.activateCouncil(council.getId());
        submitCurrentCouncil(topic, false);
        assertThat(councilService.canComplete(councilService.getCouncil(council.getId()), true)).isTrue();
        var ballot = evaluations.findByTopicId(topic.getId()).get(0);
        var removed = scores.findByEvaluationId(ballot.getId()).get(0);
        scores.delete(removed);
        assertThat(councilService.canComplete(councilService.getCouncil(council.getId()), true)).isFalse();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> councilService.completeCouncil(council.getId()))
                .isInstanceOf(com.group9.topicmanagement.exception.BusinessRuleException.class);
        removed.setId(null);
        scores.save(removed);
        councilService.completeCouncil(council.getId());
        assertThat(councilService.getCouncil(council.getId()).getStatus())
                .isEqualTo(com.group9.topicmanagement.model.enums.CouncilStatus.COMPLETED);
    }

    @Test void scoredCouncilCannotBeCancelledOrReassignedAndLockedHistoryIsPreserved() {
        Topic topic = preparedApprovedTopic();
        seedCriteria();
        var council = createCouncilWithMembersAndTopic(topic);
        councilService.activateCouncil(council.getId());
        submitCurrentCouncil(topic, true);
        assertThat(councilService.canCancel(councilService.getCouncil(council.getId()), true)).isFalse();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> councilService.cancelCouncil(council.getId()))
                .isInstanceOf(com.group9.topicmanagement.exception.BusinessRuleException.class);
        councilService.completeCouncil(council.getId());
        var other = councilService.createCouncil("Không ghi đè", topic.getRegistrationPeriod().getId(),
                LocalDateTime.now().plusDays(3), "A2");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> councilService.assignTopic(other.getId(), topic.getId()))
                .isInstanceOf(com.group9.topicmanagement.exception.BusinessRuleException.class)
                .hasMessageContaining("đã có phiếu chấm");
        assertThat(evaluations.findByTopicId(topic.getId())).hasSize(3)
                .allMatch(e -> e.getStatus() == com.group9.topicmanagement.model.enums.EvaluationStatus.LOCKED);
        assertThat(topicResultService.averageOfValidEvaluations(topic.getId())).hasValue(new BigDecimal("8.00"));
    }

    @Test void legacyHistoricalBallotsDoNotAffectCurrentScoreLockingOrExportRows() throws Exception {
        Topic topic = preparedApprovedTopic();
        seedCriteria();
        var previous = createCouncilWithMembersAndTopic(topic);
        previous.setStatus(com.group9.topicmanagement.model.enums.CouncilStatus.COMPLETED);
        councils.save(previous);
        var current = createCouncilWithMembersAndTopic(topic);
        councilService.activateCouncil(current.getId());
        submitCurrentCouncil(topic, true);
        // Pre-existing historical data from older builds, not a new business operation.
        var historicalUser = userService.create("historical", "GV lịch sử", "historical@test.local", null,
                topic.getDepartment().getId(), java.util.Set.of(com.group9.topicmanagement.model.enums.RoleName.LECTURER), "Password@123");
        var member = new com.group9.topicmanagement.model.council.CouncilMember();
        member.setCouncil(previous); member.setMember(historicalUser);
        member.setRole(com.group9.topicmanagement.model.enums.CouncilMemberRole.MEMBER);
        member = councilMembers.save(member);
        var old = new com.group9.topicmanagement.model.evaluation.Evaluation();
        old.setTopic(topic); old.setEvaluator(historicalUser); old.setCouncilMember(member);
        old.setEvaluationType(com.group9.topicmanagement.model.enums.EvaluationType.COUNCIL_MEMBER);
        old.setStatus(com.group9.topicmanagement.model.enums.EvaluationStatus.SUBMITTED);
        old = evaluations.save(old);
        for (var score : fullScores("2.00")) { score.setEvaluation(old); scores.save(score); }
        var result = topicResultService.calculateResult(topic.getId());
        assertThat(result.getFinalScore()).isEqualByComparingTo("8.00");
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        assertThat(topicResultService.listResults(auth)).hasSize(1);
        authenticate("historical", "LECTURER");
        assertThat(topicResultService.listResults(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication())).isEmpty();
        authenticate("lecturer2", "LECTURER");
        assertThat(topicResultService.canConfirm(topic.getId(), "lecturer2")).isTrue();
        topicResultService.confirmResult(topic.getId(), "lecturer2");
        authenticate("faculty1", "FACULTY_MANAGER");
        topicResultService.publishResult(topic.getId(), "faculty1");
        var bytes = mvc.perform(get("/evaluations/results/export/excel").with(user("faculty1").roles("FACULTY_MANAGER")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(bytes))) {
            assertThat(workbook.getSheetAt(0).getLastRowNum()).isEqualTo(1);
        }
    }

    @Test void legacyBallotOfSameLecturerIsNeverReusedByNewCouncil() {
        Topic topic = preparedApprovedTopic();
        seedCriteria();
        var previous = createCouncilWithMembersAndTopic(topic);
        previous.setStatus(com.group9.topicmanagement.model.enums.CouncilStatus.COMPLETED);
        councils.save(previous);
        var current = createCouncilWithMembersAndTopic(topic);
        councilService.activateCouncil(current.getId());
        authenticate("lecturer2", "LECTURER");
        var ballot = evaluationService.saveDraft(topic.getId(), "lecturer2",
                com.group9.topicmanagement.model.enums.EvaluationType.COUNCIL_MEMBER, fullScores("2.00"), "Lịch sử");
        // Emulate the legacy combination of an old ballot and a newer assignment.
        ballot.setCouncilMember(councilMembers.findByCouncilIdAndMemberId(previous.getId(),
                users.findByUsernameIgnoreCase("lecturer2").orElseThrow().getId()).orElseThrow());
        evaluations.save(ballot);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> evaluationService.getForForm(topic.getId(), "lecturer2",
                com.group9.topicmanagement.model.enums.EvaluationType.COUNCIL_MEMBER)).hasMessageContaining("hội đồng cũ");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> evaluationService.saveDraft(topic.getId(), "lecturer2",
                com.group9.topicmanagement.model.enums.EvaluationType.COUNCIL_MEMBER, fullScores("9.00"), "Không được ghi đè"))
                .hasMessageContaining("hội đồng cũ");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> evaluationService.submitEvaluation(ballot.getId()))
                .hasMessageContaining("hội đồng cũ");
        assertThat(evaluations.findById(ballot.getId()).orElseThrow().getStatus())
                .isEqualTo(com.group9.topicmanagement.model.enums.EvaluationStatus.DRAFT);
        assertThat(scores.findByEvaluationId(ballot.getId())).allMatch(s -> s.getScore().compareTo(new BigDecimal("2.00")) == 0);
    }

    private void submitCurrentCouncil(Topic topic, boolean lock) {
        for (String lecturer : List.of("lecturer2", "lecturer3", "lecturer4")) {
            authenticate(lecturer, "LECTURER");
            var ballot = evaluationService.saveDraft(topic.getId(), lecturer,
                    com.group9.topicmanagement.model.enums.EvaluationType.COUNCIL_MEMBER, fullScores("8.00"), "Hiện tại");
            evaluationService.submitEvaluation(ballot.getId());
            if (lock) {
                authenticate("faculty1", "FACULTY_MANAGER");
                evaluationService.lockEvaluation(ballot.getId());
            }
        }
        authenticate("faculty1", "FACULTY_MANAGER");
    }

    @Test void activeCouncilTakesPriorityAndHistoricalChairCannotConfirm() {
        Topic topic = preparedApprovedTopic();
        var previous = createCouncilWithMembersAndTopic(topic);
        councilService.activateCouncil(previous.getId());
        // Legacy database fixture: old versions allowed completing an unmarked council.
        previous.setStatus(com.group9.topicmanagement.model.enums.CouncilStatus.COMPLETED);
        councils.save(previous);
        var current = createCouncilWithMembersAndTopic(topic);
        // Swap chair and secretary in the new council, while preserving the old council.
        var currentMembers = councilMembers.findByCouncilId(current.getId());
        for (var member : currentMembers) {
            if (member.getRole() == com.group9.topicmanagement.model.enums.CouncilMemberRole.CHAIR) {
                member.setRole(com.group9.topicmanagement.model.enums.CouncilMemberRole.SECRETARY);
            } else if (member.getRole() == com.group9.topicmanagement.model.enums.CouncilMemberRole.SECRETARY) {
                member.setRole(com.group9.topicmanagement.model.enums.CouncilMemberRole.CHAIR);
            }
            councilMembers.save(member);
        }
        councilService.activateCouncil(current.getId());
        // A newer historical assignment must not outrank the active council.
        var historical = new com.group9.topicmanagement.model.council.CouncilAssignment();
        historical.setTopic(topic);
        var completed = councilService.createCouncil("Hội đồng lịch sử", topic.getRegistrationPeriod().getId(),
                LocalDateTime.now().plusDays(3), "B5");
        completed.setStatus(com.group9.topicmanagement.model.enums.CouncilStatus.COMPLETED);
        historical.setCouncil(councils.save(completed));
        councilAssignments.save(historical);
        assertThat(councilAssignments.findByTopicIdAndCouncil_StatusIn(topic.getId(),
                List.of(com.group9.topicmanagement.model.enums.CouncilStatus.ACTIVE,
                        com.group9.topicmanagement.model.enums.CouncilStatus.COMPLETED)).get(0)
                .getCouncil().getId()).isEqualTo(current.getId());
        assertThat(councilService.isChairOfTopic(topic.getId(),
                users.findByUsernameIgnoreCase("lecturer2").orElseThrow().getId())).isFalse();
        assertThat(councilService.isChairOfTopic(topic.getId(),
                users.findByUsernameIgnoreCase("lecturer3").orElseThrow().getId())).isTrue();
    }

    @Test void calculationUsesCurrentCouncilInsteadOfUnmarkedCompletedCouncil() throws Exception {
        Topic topic = preparedApprovedTopic();
        seedCriteria();
        var previous = createCouncilWithMembersAndTopic(topic);
        councilService.activateCouncil(previous.getId());
        previous.setStatus(com.group9.topicmanagement.model.enums.CouncilStatus.COMPLETED);
        councils.save(previous);
        var current = createCouncilWithMembersAndTopic(topic);
        councilService.activateCouncil(current.getId());
        reviewerAssignmentService.assignReviewer(topic.getId(),
                users.findByUsernameIgnoreCase("lecturer2").orElseThrow().getId(),
                "faculty1", LocalDateTime.now().plusDays(2));
        assertThat(topicResultService.canCalculate(topic.getId(), "faculty1")).isFalse();
        for (String lecturer : List.of("lecturer2", "lecturer3", "lecturer4")) {
            authenticate(lecturer, "LECTURER");
            var ballot = evaluationService.saveDraft(topic.getId(), lecturer,
                    com.group9.topicmanagement.model.enums.EvaluationType.COUNCIL_MEMBER,
                    fullScores("8.00"), "Hội đồng hiện tại");
            evaluationService.submitEvaluation(ballot.getId());
            // The outstanding reviewer ballot must still block calculation.
            assertThat(topicResultService.canCalculate(topic.getId(), "faculty1")).isFalse();
        }
        authenticate("lecturer2", "LECTURER");
        var review = evaluationService.saveDraft(topic.getId(), "lecturer2",
                com.group9.topicmanagement.model.enums.EvaluationType.REVIEWER,
                fullScores("10.00"), "Phản biện");
        evaluationService.submitEvaluation(review.getId());
        authenticate("faculty1", "FACULTY_MANAGER");
        for (var ballot : evaluations.findByTopicId(topic.getId())) evaluationService.lockEvaluation(ballot.getId());
        assertThat(topicResultService.areAllEvaluationsSubmitted(topic.getId())).isTrue();
        assertThat(topicResultService.canCalculate(topic.getId(), "faculty1")).isTrue();
        mvc.perform(get("/evaluations/topic/" + topic.getId() + "/results")
                .with(user("faculty1").roles("FACULTY_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "/evaluations/topic/" + topic.getId() + "/calculate")));
        authenticate("faculty1", "FACULTY_MANAGER");
        var result = topicResultService.calculateResult(topic.getId());
        assertThat(result.getFinalScore()).isEqualByComparingTo("8.50");
        assertThat(result.getCouncilAssignment().getCouncil().getId()).isEqualTo(current.getId());
        councilService.completeCouncil(current.getId());
        assertThat(topicResultService.canCalculate(topic.getId(), "faculty1")).isTrue();
        authenticate("lecturer2", "LECTURER");
        topicResultService.confirmResult(topic.getId(), "lecturer2");
        authenticate("faculty1", "FACULTY_MANAGER");
        assertThat(topicResultService.canPublish(topic.getId(), "faculty1")).isTrue();
    }

    @Test void reviewerCreateRendersApprovedTopicAndPeriodDateLimitsWithoutOpenSession() throws Exception {
        Topic topic = preparedApprovedTopic();
        var period = topic.getRegistrationPeriod();
        period.setType(com.group9.topicmanagement.model.enums.PeriodType.KLTN);
        var deadline = java.time.LocalDateTime.now().plusDays(7).withSecond(0).withNano(0);
        period.setReviewDeadline(deadline);
        periods.save(period);
        var response = mvc.perform(get("/reviewer-assignments/create")
                .with(user("faculty1").roles("FACULTY_MANAGER")))
                .andExpect(status().isOk()).andReturn();
        assertThat(response.getResponse().getContentAsString()).contains("DT99", "data-type=\"KLTN\"",
                "data-status=\"IN_PROGRESS\"", "data-review-deadline=\"" + deadline,
                "data-date-form=\"reviewer\"");
    }

    @Test void editAnnouncementPreservesTextAndEscapesHtmlExactlyOnce() throws Exception {
        String content = "Nội dung A & B <script>alert(1)</script>";
        var announcement = announcementService.saveAnnouncement("Thông báo thử", content,
                List.of("STUDENT"), "faculty1", true, null);
        var response = mvc.perform(get("/announcements/" + announcement.getId() + "/edit")
                .with(user("faculty1").roles("FACULTY_MANAGER")))
                .andExpect(status().isOk()).andReturn();
        var form = (AnnouncementForm) response.getModelAndView().getModel().get("announcementForm");
        assertThat(form.getContent()).isEqualTo(content);
        mvc.perform(post("/announcements/" + announcement.getId() + "/edit")
                .with(user("faculty1").roles("FACULTY_MANAGER")).with(csrf())
                .param("title", form.getTitle()).param("content", form.getContent())
                .param("targetRoles", "STUDENT").param("publish", "true"))
                .andExpect(status().is3xxRedirection());
        assertThat(announcements.findById(announcement.getId()).orElseThrow().getContent())
                .isEqualTo(announcement.getContent()).doesNotContain("&amp;amp;", "<script>");
        mvc.perform(get("/announcements/" + announcement.getId() + "/edit")
                .with(user("student1").roles("STUDENT"))).andExpect(status().isForbidden());
    }

    @Test void publishedResultRendersForOwnerOnlyAndLegacyLinkStillWorks() throws Exception {
        Topic topic = publishedFixture();
        String period = topic.getRegistrationPeriod().getId().toString();
        mvc.perform(get("/evaluations/my-result").param("periodId", period)
                .with(user("student1").roles("STUDENT")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("8.50")));
        mvc.perform(get("/evaluations/my-result").param("topicId", topic.getId().toString())
                .with(user("student1").roles("STUDENT"))).andExpect(status().isOk());
        mvc.perform(get("/evaluations/my-result").param("periodId", period)
                .with(user("student2").roles("STUDENT"))).andExpect(status().isForbidden());
        mvc.perform(get("/evaluations/my-result").param("topicId", topic.getId().toString())
                .with(user("student2").roles("STUDENT"))).andExpect(status().isForbidden());
        mvc.perform(get("/evaluations/my-result").param("periodId", "999999")
                .with(user("student1").roles("STUDENT"))).andExpect(status().isForbidden());
    }

    @Test void studentCanSelectRegisteredPeriodWithoutSeeingUnpublishedScores() throws Exception {
        Topic topic = preparedApprovedTopic();
        var response = mvc.perform(get("/evaluations/my-result")
                .param("periodId", topic.getRegistrationPeriod().getId().toString())
                .with(user("student1").roles("STUDENT")))
                .andExpect(status().isOk()).andReturn();
        assertThat(registrationService.listStudentPeriods("student1"))
                .extracting(p -> p.id()).containsExactly(topic.getRegistrationPeriod().getId());
        assertThat(response.getModelAndView().getModel().get("result")).isNull();
        assertThat(response.getResponse().getContentAsString()).contains("Đợt HK1", "Chưa có kết quả");
        mvc.perform(get("/evaluations/my-result").param("periodId", "invalid")
                .with(user("student1").roles("STUDENT"))).andExpect(status().isBadRequest());
    }

    @Test void resultTableAndExportsWorkWithClosedPersistenceContext() throws Exception {
        publishedFixture();
        mvc.perform(get("/evaluations/results").with(user("faculty1").roles("FACULTY_MANAGER")))
                .andExpect(status().isOk());
        for (String type : List.of("excel", "pdf")) {
            mvc.perform(get("/evaluations/results/export/" + type).with(user("faculty1").roles("FACULTY_MANAGER")))
                    .andExpect(status().isOk());
            mvc.perform(get("/evaluations/results/export/" + type).with(user("student1").roles("STUDENT")))
                    .andExpect(status().isForbidden());
            var outsider = mvc.perform(get("/evaluations/results/export/" + type)
                    .with(user("lecturer1").roles("LECTURER"))).andExpect(status().isOk()).andReturn();
            if (type.equals("excel")) {
                try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(
                        new java.io.ByteArrayInputStream(outsider.getResponse().getContentAsByteArray()))) {
                    assertThat(workbook.getSheetAt(0).getLastRowNum()).isZero();
                } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
            }
        }
    }

    @Test void rolledBackAssignmentDoesNotSendEmail() {
        Topic topic = preparedApprovedTopic();
        Long reviewer = users.findByUsernameIgnoreCase("lecturer2").orElseThrow().getId();
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            reviewerAssignmentService.assignReviewer(topic.getId(), reviewer, "faculty1", LocalDateTime.now().plusDays(2));
            verifyNoInteractions(mail);
            tx.setRollbackOnly();
        });
        assertThat(reviewerAssignments.findByTopicIdAndReviewerId(topic.getId(), reviewer)).isEmpty();
        verifyNoInteractions(mail);
    }

    @Test void committedAssignmentSendsEmailAfterCommit() {
        Topic topic = preparedApprovedTopic();
        Long reviewer = users.findByUsernameIgnoreCase("lecturer2").orElseThrow().getId();
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            reviewerAssignmentService.assignReviewer(topic.getId(), reviewer, "faculty1", LocalDateTime.now().plusDays(2));
            verifyNoInteractions(mail);
        });
        verify(mail).sendNotification(eq("lec2@test.local"), contains("DT99"), contains("/evaluations/topic/" + topic.getId()));
    }

    @Test void publishNotifiesEveryMemberOnceAndNeverForRollback() {
        Topic topic = preparedApprovedTopic();
        var period = periods.findById(topic.getRegistrationPeriod().getId()).orElseThrow();
        period.setStatus(com.group9.topicmanagement.model.enums.PeriodStatus.STUDENT_REGISTRATION);
        periods.save(period);
        period.setStatus(com.group9.topicmanagement.model.enums.PeriodStatus.IN_PROGRESS);
        periods.save(period);
        seedCriteria();
        var council = createCouncilWithMembersAndTopic(topic);
        councilService.activateCouncil(council.getId());
        reviewerAssignmentService.assignReviewer(topic.getId(), users.findByUsernameIgnoreCase("lecturer2").orElseThrow().getId(),
                "faculty1", LocalDateTime.now().plusDays(2));
        for (String lecturer : List.of("lecturer2", "lecturer3", "lecturer4")) {
            authenticate(lecturer, "LECTURER");
            var ballot = evaluationService.saveDraft(topic.getId(), lecturer,
                    com.group9.topicmanagement.model.enums.EvaluationType.COUNCIL_MEMBER, fullScores("8.13"), "Tốt");
            evaluationService.submitEvaluation(ballot.getId());
        }
        authenticate("lecturer2", "LECTURER");
        var review = evaluationService.saveDraft(topic.getId(), "lecturer2",
                com.group9.topicmanagement.model.enums.EvaluationType.REVIEWER, fullScores("8.13"), "Tốt");
        evaluationService.submitEvaluation(review.getId());
        authenticate("faculty1", "FACULTY_MANAGER");
        for (var ballot : evaluations.findByTopicId(topic.getId())) evaluationService.lockEvaluation(ballot.getId());
        topicResultService.calculateResult(topic.getId());
        authenticate("lecturer2", "LECTURER");
        topicResultService.confirmResult(topic.getId(), "lecturer2");
        authenticate("faculty1", "FACULTY_MANAGER");
        reset(mail);
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            topicResultService.publishResult(topic.getId(), "faculty1");
            verifyNoInteractions(mail);
            tx.setRollbackOnly();
        });
        assertThat(results.findByTopicId(topic.getId()).orElseThrow().getStatus()).isEqualTo(TopicResultStatus.CONFIRMED);
        verifyNoInteractions(mail);
        topicResultService.publishResult(topic.getId(), "faculty1");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> topicResultService.publishResult(topic.getId(), "faculty1"))
                .isInstanceOf(com.group9.topicmanagement.exception.BusinessRuleException.class);
        verify(mail).sendNotification(eq("sv1@test.local"), contains("Kết quả"), contains("8.13"));
        verify(mail).sendNotification(eq("sv3@test.local"), contains("Kết quả"), contains("8.13"));
        verify(mail).sendNotification(eq("sv4@test.local"), contains("Kết quả"), contains("8.13"));
        verifyNoMoreInteractions(mail);
    }

    @Test void fullMailQueueDoesNotUndoAssignmentOrProduceServerError() {
        Topic topic = preparedApprovedTopic();
        doThrow(new org.springframework.core.task.TaskRejectedException("queue full"))
                .when(mail).sendNotification(anyString(), anyString(), anyString());
        Long reviewer = users.findByUsernameIgnoreCase("lecturer2").orElseThrow().getId();
        reviewerAssignmentService.assignReviewer(topic.getId(), reviewer, "faculty1", LocalDateTime.now().plusDays(2));
        assertThat(reviewerAssignments.findByTopicIdAndReviewerId(topic.getId(), reviewer)).isPresent();
    }

    private Topic publishedFixture() {
        Topic topic = preparedApprovedTopic();
        var council = createCouncilWithMembersAndTopic(topic);
        TopicResult result = new TopicResult();
        result.setTopic(topic);
        result.setCouncilAssignment(councilAssignments.findByCouncilId(council.getId()).get(0));
        result.setFinalScore(new BigDecimal("8.50"));
        result.setStatus(TopicResultStatus.PUBLISHED);
        results.save(result);
        return topic;
    }
}
