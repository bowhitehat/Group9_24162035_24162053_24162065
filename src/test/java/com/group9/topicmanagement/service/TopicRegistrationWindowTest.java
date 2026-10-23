package com.group9.topicmanagement.service;

import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.model.RegistrationPeriod;
import com.group9.topicmanagement.model.User;
import com.group9.topicmanagement.model.enums.PeriodStatus;
import com.group9.topicmanagement.model.enums.RegistrationStatus;
import com.group9.topicmanagement.model.registration.TopicRegistration;
import com.group9.topicmanagement.model.studentgroup.GroupMember;
import com.group9.topicmanagement.model.studentgroup.StudentGroup;
import com.group9.topicmanagement.model.topic.Topic;
import com.group9.topicmanagement.model.topic.TopicStatus;
import com.group9.topicmanagement.repository.RegistrationPeriodRepository;
import com.group9.topicmanagement.repository.StudentGroupRepository;
import com.group9.topicmanagement.repository.TopicRegistrationRepository;
import com.group9.topicmanagement.repository.TopicRepository;
import com.group9.topicmanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TopicRegistrationWindowTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 23, 10, 0);

    private TopicRegistrationRepository registrations;
    private TopicRepository topics;
    private TopicRegistrationService service;
    private RegistrationPeriod period;

    @BeforeEach
    void setUp() {
        registrations = mock(TopicRegistrationRepository.class);
        topics = mock(TopicRepository.class);
        StudentGroupRepository groups = mock(StudentGroupRepository.class);
        Clock clock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
        service = new TopicRegistrationService(registrations, groups, topics,
                mock(RegistrationPeriodRepository.class), mock(UserRepository.class), clock);

        period = new RegistrationPeriod();
        ReflectionTestUtils.setField(period, "id", 1L);
        period.setStatus(PeriodStatus.STUDENT_REGISTRATION);
        period.setStudentStart(NOW.minusHours(1));
        period.setStudentEnd(NOW.plusHours(1));

        StudentGroup group = new StudentGroup();
        group.setId(2L);
        group.setRegistrationPeriod(period);
        for (long id = 1; id <= 3; id++) {
            User student = new User();
            ReflectionTestUtils.setField(student, "id", id);
            student.setUsername("student" + id);
            GroupMember member = new GroupMember();
            member.setGroup(group);
            member.setMember(student);
            member.setLeader(id == 1);
            group.getMembers().add(member);
            if (id == 1) group.setLeader(student);
        }
        when(groups.findById(2L)).thenReturn(Optional.of(group));
    }

    @Test
    void reportsNotStartedBeforeStudentWindowWithoutSaving() {
        period.setStudentStart(NOW.plusSeconds(1));

        assertThatThrownBy(() -> service.registerTopic(2L, 3L, "student1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Chưa đến thời gian đăng ký đề tài. Mở từ 23/10/2026 10:00.");
        verifyNoInteractions(topics);
        verify(registrations, never()).save(any());
    }

    @Test
    void reportsExpiredAfterStudentWindowWithoutSaving() {
        period.setStudentEnd(NOW.minusSeconds(1));

        assertThatThrownBy(() -> service.registerTopic(2L, 3L, "student1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Đã quá thời hạn đăng ký đề tài. Hạn cuối là 23/10/2026 09:59.");
        verifyNoInteractions(topics);
        verify(registrations, never()).save(any());
    }

    @Test
    void stillRequiresStudentRegistrationStatusEvenInsideWindow() {
        period.setStatus(PeriodStatus.DRAFT);

        assertThatThrownBy(() -> service.registerTopic(2L, 3L, "student1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Khoa chưa mở giai đoạn sinh viên đăng ký đề tài")
                .hasMessageContaining("Trạng thái đợt: Nháp");
        verifyNoInteractions(topics);
        verify(registrations, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {-2, -1, 0})
    void acceptsInsideWindowIncludingStartAndEnd(int startOffsetHours) {
        period.setStudentStart(NOW.plusHours(startOffsetHours));
        period.setStudentEnd(period.getStudentStart().plusHours(2));
        Topic topic = new Topic();
        topic.setId(3L);
        topic.setRegistrationPeriod(period);
        topic.setStatus(TopicStatus.PUBLISHED);
        when(topics.findById(3L)).thenReturn(Optional.of(topic));
        when(registrations.save(any(TopicRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TopicRegistration result = service.registerTopic(2L, 3L, "student1");

        assertThat(result.getStatus()).isEqualTo(RegistrationStatus.PENDING);
        assertThat(result.getTopic()).isSameAs(topic);
        verify(registrations).save(result);
    }
}
