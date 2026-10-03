package com.group9.topicmanagement.service;

import com.group9.topicmanagement.config.UploadConfig;
import com.group9.topicmanagement.model.Role;
import com.group9.topicmanagement.model.User;
import com.group9.topicmanagement.model.council.Council;
import com.group9.topicmanagement.model.council.CouncilAssignment;
import com.group9.topicmanagement.model.enums.CouncilStatus;
import com.group9.topicmanagement.model.enums.ReviewerAssignmentStatus;
import com.group9.topicmanagement.model.enums.RoleName;
import com.group9.topicmanagement.model.evaluation.ReviewerAssignment;
import com.group9.topicmanagement.model.registration.ReportSubmission;
import com.group9.topicmanagement.model.registration.TopicRegistration;
import com.group9.topicmanagement.model.topic.Topic;
import com.group9.topicmanagement.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportSubmissionAccessTest {
    @Mock ReportSubmissionRepository reportRepository;
    @Mock TopicRegistrationRepository registrationRepository;
    @Mock UserRepository userRepository;
    @Mock ReviewerAssignmentRepository reviewerAssignmentRepository;
    @Mock CouncilAssignmentRepository councilAssignmentRepository;
    @Mock CouncilMemberRepository councilMemberRepository;
    @Mock UploadConfig uploadConfig;
    @Mock Clock clock;
    @InjectMocks ReportSubmissionService service;

    User viewer;
    Topic topic;
    TopicRegistration registration;
    ReportSubmission report;

    @BeforeEach void setUp() {
        viewer = new User();
        ReflectionTestUtils.setField(viewer, "id", 10L);
        viewer.setUsername("reviewer");
        Role role = new Role();
        role.setName(RoleName.LECTURER);
        viewer.getRoles().add(role);
        User proposer = new User();
        ReflectionTestUtils.setField(proposer, "id", 20L);
        topic = new Topic();
        ReflectionTestUtils.setField(topic, "id", 1L);
        topic.setProposer(proposer);
        registration = new TopicRegistration();
        registration.setTopic(topic);
        report = new ReportSubmission();
        report.setTopicRegistration(registration);
        when(userRepository.findByUsernameIgnoreCase("reviewer")).thenReturn(Optional.of(viewer));
        lenient().when(registrationRepository.findById(2L)).thenReturn(Optional.of(registration));
        lenient().when(reportRepository.findById(3L)).thenReturn(Optional.of(report));
    }

    @ParameterizedTest
    @EnumSource(value = ReviewerAssignmentStatus.class, names = "CANCELLED", mode = EnumSource.Mode.EXCLUDE)
    void assignedReviewerCanReadHistoryAndDownloadEvenAfterDeadline(ReviewerAssignmentStatus status) {
        ReviewerAssignment assignment = new ReviewerAssignment();
        assignment.setStatus(status);
        assignment.setDeadline(java.time.LocalDateTime.now().minusDays(1));
        when(reviewerAssignmentRepository.findByTopicIdAndReviewerId(1L, 10L)).thenReturn(Optional.of(assignment));
        assertCanRead();
    }

    @Test void cancelledReviewerCannotRead() {
        ReviewerAssignment assignment = new ReviewerAssignment();
        assignment.setStatus(ReviewerAssignmentStatus.CANCELLED);
        when(reviewerAssignmentRepository.findByTopicIdAndReviewerId(1L, 10L)).thenReturn(Optional.of(assignment));
        assertCannotRead();
    }

    @ParameterizedTest
    @EnumSource(value = CouncilStatus.class, names = {"ACTIVE", "COMPLETED"})
    void assignedCouncilMemberCanRead(CouncilStatus status) {
        mockCouncil(status);
        when(councilMemberRepository.existsByCouncilIdAndMemberId(4L, 10L)).thenReturn(true);
        assertCanRead();
    }

    @ParameterizedTest
    @EnumSource(value = CouncilStatus.class, names = {"DRAFT", "CANCELLED"})
    void draftOrCancelledCouncilDoesNotGrantAccess(CouncilStatus status) {
        mockCouncil(status);
        assertCannotRead();
        verifyNoInteractions(councilMemberRepository);
    }

    @Test void lecturerOutsideAssignedCouncilCannotRead() {
        mockCouncil(CouncilStatus.ACTIVE);
        when(councilMemberRepository.existsByCouncilIdAndMemberId(4L, 10L)).thenReturn(false);
        assertCannotRead();
    }

    @Test void unassignedLecturerCannotRead() { assertCannotRead(); }

    @Test void advisorStillCanRead() {
        topic.getAdvisors().add(viewer);
        assertCanRead();
        verifyNoInteractions(reviewerAssignmentRepository, councilAssignmentRepository);
    }

    private void mockCouncil(CouncilStatus status) {
        Council council = new Council();
        council.setId(4L);
        council.setStatus(status);
        CouncilAssignment assignment = new CouncilAssignment();
        assignment.setCouncil(council);
        assignment.setTopic(topic);
        when(councilAssignmentRepository.findByTopicId(1L)).thenReturn(List.of(assignment));
    }

    private void assertCanRead() {
        assertThat(service.getSubmissionByIdForUser(3L, "reviewer")).isSameAs(report);
        assertThat(service.getRegistrationForUser(2L, "reviewer")).isSameAs(registration);
        assertThatCode(() -> service.getSubmissionHistoryForUser(2L, "reviewer")).doesNotThrowAnyException();
    }

    private void assertCannotRead() {
        assertThatThrownBy(() -> service.getSubmissionByIdForUser(3L, "reviewer")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.getSubmissionHistoryForUser(2L, "reviewer")).isInstanceOf(AccessDeniedException.class);
        verify(reportRepository, never()).findByTopicRegistration_IdOrderByVersionDesc(anyLong());
    }
}
