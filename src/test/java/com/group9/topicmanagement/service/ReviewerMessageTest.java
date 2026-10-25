package com.group9.topicmanagement.service;

import com.group9.topicmanagement.model.enums.*;
import com.group9.topicmanagement.model.evaluation.*;
import com.group9.topicmanagement.model.topic.Topic;
import com.group9.topicmanagement.repository.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReviewerMessageTest {
    @Test void listExplainsCancellationExpiryLockAndConfirmationUsingTwoBatchReads() {
        var ballots = mock(EvaluationRepository.class);
        var results = mock(TopicResultRepository.class);
        var service = new ReviewerAssignmentService(mock(ReviewerAssignmentRepository.class), mock(TopicRepository.class),
                mock(UserRepository.class), mock(TopicRegistrationService.class), mock(TransactionalNotificationService.class), ballots, results, mock(EvaluationScopeService.class));
        var cancelled = assignment(1, ReviewerAssignmentStatus.CANCELLED);
        var expired = assignment(2, ReviewerAssignmentStatus.ASSIGNED); expired.setDeadline(LocalDateTime.now().minusDays(1));
        var locked = assignment(3, ReviewerAssignmentStatus.SUBMITTED);
        var confirmed = assignment(4, ReviewerAssignmentStatus.SUBMITTED);
        var editable = assignment(5, ReviewerAssignmentStatus.ASSIGNED);
        var ballot = new Evaluation(); ballot.setReviewerAssignment(locked); ballot.setStatus(EvaluationStatus.LOCKED);
        var result = new TopicResult(); result.setTopic(confirmed.getTopic()); result.setStatus(TopicResultStatus.CONFIRMED);
        when(ballots.findByReviewerAssignmentIdIn(anyList())).thenReturn(List.of(ballot));
        when(results.findByTopicIdIn(anyList())).thenReturn(List.of(result));
        var reasons = service.gradingBlockReasons(List.of(cancelled, expired, locked, confirmed, editable));
        assertThat(reasons.get(1L)).contains("đã bị hủy");
        assertThat(reasons.get(2L)).contains("quá hạn", "Hạn cuối");
        assertThat(reasons.get(3L)).contains("được khoa khóa");
        assertThat(reasons.get(4L)).contains("được xác nhận hoặc công bố");
        assertThat(reasons).doesNotContainKey(5L);
        verify(ballots, times(1)).findByReviewerAssignmentIdIn(anyList());
        verify(results, times(1)).findByTopicIdIn(anyList());
    }

    @Test void emptyListDoesNotQueryBallotsOrResults() {
        var ballots = mock(EvaluationRepository.class); var results = mock(TopicResultRepository.class);
        var service = new ReviewerAssignmentService(mock(ReviewerAssignmentRepository.class), mock(TopicRepository.class),
                mock(UserRepository.class), mock(TopicRegistrationService.class), mock(TransactionalNotificationService.class), ballots, results, mock(EvaluationScopeService.class));
        assertThat(service.gradingBlockReasons(List.of())).isEmpty();
        verifyNoInteractions(ballots, results);
    }

    private ReviewerAssignment assignment(long id, ReviewerAssignmentStatus status) {
        var a = new ReviewerAssignment(); a.setId(id); a.setStatus(status); a.setDeadline(LocalDateTime.now().plusDays(1));
        var t = new Topic(); t.setId(id); a.setTopic(t); return a;
    }
}
