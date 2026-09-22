package com.group9.topicmanagement.service;

import com.group9.topicmanagement.domain.User;
import com.group9.topicmanagement.domain.announcement.Announcement;
import com.group9.topicmanagement.domain.enums.CouncilStatus;
import com.group9.topicmanagement.domain.enums.PeriodStatus;
import com.group9.topicmanagement.domain.enums.RegistrationStatus;
import com.group9.topicmanagement.domain.enums.ReviewerAssignmentStatus;
import com.group9.topicmanagement.domain.enums.TopicResultStatus;
import com.group9.topicmanagement.domain.topic.TopicStatus;
import com.group9.topicmanagement.domain.evaluation.ReviewerAssignment;
import com.group9.topicmanagement.domain.evaluation.TopicResult;
import com.group9.topicmanagement.domain.registration.ReportSubmission;
import com.group9.topicmanagement.domain.registration.TopicRegistration;
import com.group9.topicmanagement.domain.studentgroup.StudentGroup;
import com.group9.topicmanagement.domain.topic.Topic;
import com.group9.topicmanagement.repository.CouncilRepository;
import com.group9.topicmanagement.repository.RegistrationPeriodRepository;
import com.group9.topicmanagement.repository.ReportSubmissionRepository;
import com.group9.topicmanagement.repository.ReviewerAssignmentRepository;
import com.group9.topicmanagement.repository.StudentGroupRepository;
import com.group9.topicmanagement.repository.TopicRepository;
import com.group9.topicmanagement.repository.TopicResultRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class DashboardService {
    private final RegistrationPeriodRepository periodRepository;
    private final TopicService topicService;
    private final TopicRegistrationService topicRegistrationService;
    private final ReportSubmissionRepository reportSubmissionRepository;
    private final ReviewerAssignmentRepository reviewerAssignmentRepository;
    private final CouncilRepository councilRepository;
    private final TopicResultRepository topicResultRepository;
    private final TopicRepository topicRepository;
    private final StudentGroupRepository studentGroupRepository;
    private final AnnouncementService announcementService;
    private final UserService userService;
    private final TopicResultService topicResultService;

    public DashboardService(RegistrationPeriodRepository periodRepository,
                            TopicService topicService,
                            TopicRegistrationService topicRegistrationService,
                            ReportSubmissionRepository reportSubmissionRepository,
                            ReviewerAssignmentRepository reviewerAssignmentRepository,
                            CouncilRepository councilRepository,
                            TopicResultRepository topicResultRepository,
                            TopicRepository topicRepository,
                            StudentGroupRepository studentGroupRepository,
                            AnnouncementService announcementService,
                            UserService userService,
                            TopicResultService topicResultService) {
        this.periodRepository = periodRepository;
        this.topicService = topicService;
        this.topicRegistrationService = topicRegistrationService;
        this.reportSubmissionRepository = reportSubmissionRepository;
        this.reviewerAssignmentRepository = reviewerAssignmentRepository;
        this.councilRepository = councilRepository;
        this.topicResultRepository = topicResultRepository;
        this.topicRepository = topicRepository;
        this.studentGroupRepository = studentGroupRepository;
        this.announcementService = announcementService;
        this.userService = userService;
        this.topicResultService = topicResultService;
    }

    public Map<String, Object> facultyStats() {
        Map<String, Object> stats = new HashMap<>();
        long openPeriods = periodRepository.findAll().stream()
                .filter(p -> p.getStatus() != PeriodStatus.CLOSED && p.getStatus() != PeriodStatus.DRAFT)
                .count();
        stats.put("openPeriods", openPeriods);
        stats.put("pendingTopics", topicService.countByStatus(TopicStatus.PENDING));
        stats.put("approvedGroups", topicRegistrationService.countApprovedRegistrations());
        stats.put("submittedReports", reportSubmissionRepository.count());
        stats.put("topicsWithoutReviewer", reviewerAssignmentRepository.countApprovedTopicsWithoutReviewer());
        stats.put("reviewersPendingScore", reviewerAssignmentRepository.countByStatusIn(
                List.of(ReviewerAssignmentStatus.ASSIGNED, ReviewerAssignmentStatus.IN_PROGRESS, ReviewerAssignmentStatus.OVERDUE)));
        long missingScores = topicRegistrationService.listApprovedRegistrations().stream()
                .filter(r -> topicResultService.hasMissingMandatoryScores(r.getTopic().getId()))
                .count();
        stats.put("topicsMissingScores", missingScores);
        stats.put("activeCouncils", councilRepository.countByStatus(CouncilStatus.ACTIVE));
        stats.put("confirmedResults", topicResultRepository.countByStatus(TopicResultStatus.CONFIRMED));
        stats.put("publishedResults", topicResultRepository.countByStatus(TopicResultStatus.PUBLISHED));
        stats.put("announcements", announcementService.getActiveAnnouncementsForRole("FACULTY_MANAGER"));
        return stats;
    }

    public Map<String, Object> lecturerStats(String username) {
        User lecturer = userService.getByUsername(username);
        Map<String, Object> stats = new HashMap<>();
        List<Topic> advising = topicRepository.findByAdvisorsId(lecturer.getId(), Pageable.unpaged()).getContent();
        List<ReviewerAssignment> assignments = reviewerAssignmentRepository.findByReviewerIdWithTopic(lecturer.getId());
        stats.put("advisingTopics", advising);
        stats.put("reviewerAssignments", assignments);
        stats.put("gradingTasks", assignments.stream()
                .filter(a -> a.getStatus() != ReviewerAssignmentStatus.SUBMITTED
                        && a.getStatus() != ReviewerAssignmentStatus.CANCELLED)
                .toList());
        stats.put("announcements", announcementService.getActiveAnnouncementsForRole("LECTURER"));
        return stats;
    }

    public Map<String, Object> studentStats(String username) {
        User student = userService.getByUsername(username);
        Map<String, Object> stats = new HashMap<>();
        TopicRegistration registration = topicRegistrationService
                .findRegistrationsForStudent(username, null, null, null, Pageable.ofSize(20))
                .stream().findFirst().orElse(null);
        StudentGroup group = registration != null ? registration.getStudentGroup() : null;
        if (group == null) {
            group = studentGroupRepository.findAll().stream()
                    .filter(g -> g.getMembers().stream().anyMatch(m -> m.getMember().getId().equals(student.getId())))
                    .findFirst().orElse(null);
        }
        stats.put("studentGroup", group);
        stats.put("registration", registration);
        ReportSubmission latestReport = null;
        TopicResult publishedResult = null;
        if (registration != null) {
            List<ReportSubmission> history = reportSubmissionRepository
                    .findByRegistrationIdOrderByVersionDesc(registration.getId());
            latestReport = history.isEmpty() ? null : history.get(0);
            publishedResult = topicResultService.findPublishedForStudent(registration.getTopic().getId(), username)
                    .orElse(null);
        }
        if (registration != null) {
            registration.getTopic().getTitle();
            registration.getStatus();
        }
        if (group != null) {
            group.getMembers().size();
        }
        if (latestReport != null) {
            latestReport.getOriginalFileName();
        }
        if (publishedResult != null) {
            publishedResult.getTopic().getId();
            publishedResult.getFinalScore();
        }
        stats.put("latestReport", latestReport);
        stats.put("publishedResult", publishedResult);
        stats.put("announcements", announcementService.getActiveAnnouncementsForRole("STUDENT"));
        return stats;
    }

    public List<Announcement> announcementsForRole(String role) {
        return announcementService.getActiveAnnouncementsForRole(role);
    }
}
