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
        var registration = registrationService.findApprovedForTopic(topic.getId()).orElseThrow();
        groupService.addMember(registration.getStudentGroup().getId(), "student2", "student1");
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
        verify(mail).sendNotification(eq("sv2@test.local"), contains("Kết quả"), contains("8.13"));
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
