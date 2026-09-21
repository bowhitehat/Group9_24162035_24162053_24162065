package com.group9.topicmanagement.service;

import com.group9.topicmanagement.domain.RegistrationPeriod;
import com.group9.topicmanagement.domain.User;
import com.group9.topicmanagement.domain.enums.PeriodStatus;
import com.group9.topicmanagement.domain.enums.PeriodType;
import com.group9.topicmanagement.domain.registration.TopicRegistration;
import com.group9.topicmanagement.domain.topic.Topic;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.repository.ReviewerAssignmentRepository;
import com.group9.topicmanagement.repository.TopicRepository;
import com.group9.topicmanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewerAssignmentServiceTest {

    @Mock private ReviewerAssignmentRepository assignmentRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private UserRepository userRepository;
    @Mock private TopicRegistrationService topicRegistrationService;

    @InjectMocks
    private ReviewerAssignmentService assignmentService;

    private Topic topic;
    private User reviewer;
    private User assigner;

    @BeforeEach
    void setUp() {
        topic = new Topic();
        ReflectionTestUtils.setField(topic, "id", 1L);
        RegistrationPeriod period = new RegistrationPeriod();
        period.setStatus(PeriodStatus.IN_PROGRESS);
        period.setType(PeriodType.MON_HOC);
        topic.setRegistrationPeriod(period);
        topic.setAdvisors(new HashSet<>());

        reviewer = new User();
        ReflectionTestUtils.setField(reviewer, "id", 10L);
        reviewer.setUsername("GV01");

        assigner = new User();
        assigner.setUsername("FMANAGER");
    }

    @Test
    void assignReviewer_Success() {
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));
        when(userRepository.findById(10L)).thenReturn(Optional.of(reviewer));
        when(userRepository.findByUsernameIgnoreCase("FMANAGER")).thenReturn(Optional.of(assigner));
        when(topicRegistrationService.findApprovedForTopic(1L)).thenReturn(Optional.of(new TopicRegistration()));
        when(assignmentRepository.existsByTopicIdAndReviewerId(1L, 10L)).thenReturn(false);
        when(assignmentRepository.findByTopicIdAndReviewerId(1L, 10L)).thenReturn(Optional.empty());

        assignmentService.assignReviewer(1L, 10L, "FMANAGER", LocalDateTime.now().plusDays(5));

        verify(assignmentRepository, times(1)).save(any());
    }

    @Test
    void assignReviewer_ThrowsException_IfReviewerIsAdvisor() {
        topic.getAdvisors().add(reviewer);

        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));
        when(userRepository.findById(10L)).thenReturn(Optional.of(reviewer));
        when(userRepository.findByUsernameIgnoreCase("FMANAGER")).thenReturn(Optional.of(assigner));
        when(topicRegistrationService.findApprovedForTopic(1L)).thenReturn(Optional.of(new TopicRegistration()));
        when(assignmentRepository.existsByTopicIdAndReviewerId(1L, 10L)).thenReturn(false);

        BusinessRuleException e = assertThrows(BusinessRuleException.class, () -> {
            assignmentService.assignReviewer(1L, 10L, "FMANAGER", LocalDateTime.now());
        });
        assertTrue(e.getMessage().contains("không được phản biện đề tài mình đang hướng dẫn"));
    }
}
