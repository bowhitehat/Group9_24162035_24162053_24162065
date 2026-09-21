package com.group9.topicmanagement.service;

import com.group9.topicmanagement.domain.RegistrationPeriod;
import com.group9.topicmanagement.domain.User;
import com.group9.topicmanagement.domain.council.Council;
import com.group9.topicmanagement.domain.council.CouncilMember;
import com.group9.topicmanagement.domain.enums.CouncilMemberRole;
import com.group9.topicmanagement.domain.enums.CouncilStatus;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CouncilServiceTest {

    @Mock private CouncilRepository councilRepository;
    @Mock private CouncilMemberRepository memberRepository;
    @Mock private CouncilAssignmentRepository assignmentRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private UserRepository userRepository;
    @Mock private RegistrationPeriodRepository periodRepository;
    @Mock private TopicRegistrationService topicRegistrationService;

    @InjectMocks
    private CouncilService councilService;

    private Council council;
    private User lecturer;

    @BeforeEach
    void setUp() {
        RegistrationPeriod period = new RegistrationPeriod();
        ReflectionTestUtils.setField(period, "id", 1L);

        council = new Council();
        ReflectionTestUtils.setField(council, "id", 1L);
        council.setRegistrationPeriod(period);
        council.setStatus(CouncilStatus.DRAFT);

        lecturer = new User();
        ReflectionTestUtils.setField(lecturer, "id", 10L);
    }

    @Test
    void addMember_Success() {
        when(councilRepository.findById(1L)).thenReturn(Optional.of(council));
        when(userRepository.findById(10L)).thenReturn(Optional.of(lecturer));
        when(memberRepository.existsByCouncilIdAndMemberId(1L, 10L)).thenReturn(false);
        when(memberRepository.findByCouncilId(1L)).thenReturn(new ArrayList<>());

        councilService.addMember(1L, 10L, CouncilMemberRole.CHAIR);

        verify(memberRepository, times(1)).save(any(CouncilMember.class));
    }

    @Test
    void addMember_ThrowsException_IfAlreadyExists() {
        when(councilRepository.findById(1L)).thenReturn(Optional.of(council));
        when(userRepository.findById(10L)).thenReturn(Optional.of(lecturer));
        when(memberRepository.existsByCouncilIdAndMemberId(1L, 10L)).thenReturn(true);

        BusinessRuleException e = assertThrows(BusinessRuleException.class, () -> {
            councilService.addMember(1L, 10L, CouncilMemberRole.CHAIR);
        });
        assertTrue(e.getMessage().contains("Giảng viên đã có trong hội đồng"));
    }

    @Test
    void addMember_ThrowsException_IfMoreThan5Members() {
        when(councilRepository.findById(1L)).thenReturn(Optional.of(council));
        when(userRepository.findById(10L)).thenReturn(Optional.of(lecturer));
        when(memberRepository.existsByCouncilIdAndMemberId(1L, 10L)).thenReturn(false);
        
        List<CouncilMember> members = new ArrayList<>();
        for(int i=0; i<5; i++) members.add(new CouncilMember());
        when(memberRepository.findByCouncilId(1L)).thenReturn(members);

        BusinessRuleException e = assertThrows(BusinessRuleException.class, () -> {
            councilService.addMember(1L, 10L, CouncilMemberRole.MEMBER);
        });
        assertTrue(e.getMessage().contains("tối đa 5 thành viên"));
    }
    
    @Test
    void addMember_ThrowsException_IfChairAlreadyExists() {
        when(councilRepository.findById(1L)).thenReturn(Optional.of(council));
        when(userRepository.findById(10L)).thenReturn(Optional.of(lecturer));
        when(memberRepository.existsByCouncilIdAndMemberId(1L, 10L)).thenReturn(false);
        
        List<CouncilMember> members = new ArrayList<>();
        CouncilMember existingChair = new CouncilMember();
        existingChair.setRole(CouncilMemberRole.CHAIR);
        members.add(existingChair);
        when(memberRepository.findByCouncilId(1L)).thenReturn(members);

        BusinessRuleException e = assertThrows(BusinessRuleException.class, () -> {
            councilService.addMember(1L, 10L, CouncilMemberRole.CHAIR);
        });
        assertTrue(e.getMessage().contains("đã có CHAIR"));
    }
}
