package com.group9.topicmanagement.service;

import com.group9.topicmanagement.config.UploadConfig;
import com.group9.topicmanagement.domain.RegistrationPeriod;
import com.group9.topicmanagement.domain.User;
import com.group9.topicmanagement.domain.enums.RegistrationStatus;
import com.group9.topicmanagement.domain.enums.RoleName;
import com.group9.topicmanagement.domain.registration.ReportSubmission;
import com.group9.topicmanagement.domain.registration.TopicRegistration;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.exception.NotFoundException;
import com.group9.topicmanagement.repository.ReportSubmissionRepository;
import com.group9.topicmanagement.repository.TopicRegistrationRepository;
import com.group9.topicmanagement.repository.UserRepository;
import com.group9.topicmanagement.controller.form.ReportSubmissionForm;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class ReportSubmissionService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx", "zip");

    private final ReportSubmissionRepository reportRepository;
    private final TopicRegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final UploadConfig uploadConfig;
    private final Clock clock;

    public ReportSubmissionService(ReportSubmissionRepository reportRepository,
                                   TopicRegistrationRepository registrationRepository,
                                   UserRepository userRepository,
                                   UploadConfig uploadConfig,
                                   Clock clock) {
        this.reportRepository = reportRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.uploadConfig = uploadConfig;
        this.clock = clock;
    }

    public ReportSubmission submitReport(ReportSubmissionForm form, String submitterUsername) {
        TopicRegistration registration = getRegistrationForSubmission(form.getTopicRegistrationId(), submitterUsername);
        MultipartFile file = form.getFile();
        validateFile(file);

        User submitter = getUser(submitterUsername);
        Optional<ReportSubmission> latestOpt = reportRepository.findLatestByRegistrationId(registration.getId());
        int nextVersion = latestOpt.map(report -> report.getVersion() + 1).orElse(1);

        String originalFileName = sanitizeOriginalFileName(file.getOriginalFilename());
        String extension = extensionOf(originalFileName);
        String storedFileName = UUID.randomUUID() + "." + extension;
        Path uploadDirectory = uploadConfig.getUploadDirectory().toAbsolutePath().normalize();
        Path targetPath = uploadDirectory.resolve(storedFileName).normalize();
        if (!targetPath.startsWith(uploadDirectory)) {
            throw new BusinessRuleException("Tên tập tin không hợp lệ");
        }

        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException error) {
            throw new BusinessRuleException("Không thể lưu tập tin báo cáo");
        }

        ReportSubmission submission = new ReportSubmission();
        submission.setStudentGroup(registration.getStudentGroup());
        submission.setTopicRegistration(registration);
        submission.setSubmitter(submitter);
        submission.setOriginalFileName(originalFileName);
        submission.setStoredFileName(storedFileName);
        submission.setContentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
        submission.setFileSize(file.getSize());
        submission.setVersion(nextVersion);
        submission.setNote(form.getNote() == null || form.getNote().isBlank() ? null : form.getNote().trim());
        return reportRepository.save(submission);
    }

    @Transactional(readOnly = true)
    public TopicRegistration getRegistrationForSubmission(Long registrationId, String username) {
        TopicRegistration registration = getRegistration(registrationId);
        if (!registration.getStudentGroup().getLeader().getUsername().equalsIgnoreCase(username)) {
            throw new AccessDeniedException("Chỉ trưởng nhóm mới được quyền nộp báo cáo");
        }
        if (registration.getStatus() != RegistrationStatus.APPROVED) {
            throw new BusinessRuleException("Chỉ có thể nộp báo cáo khi đăng ký đề tài đã được duyệt");
        }
        ensureReportDeadline(registration.getRegistrationPeriod());
        return registration;
    }

    @Transactional(readOnly = true)
    public boolean canSubmitReport(TopicRegistration registration, String username) {
        if (registration == null
                || registration.getStatus() != RegistrationStatus.APPROVED
                || !registration.getStudentGroup().getLeader().getUsername().equalsIgnoreCase(username)) return false;
        LocalDateTime deadline = effectiveReportDeadline(registration.getRegistrationPeriod());
        return deadline == null || !LocalDateTime.now(clock).isAfter(deadline);
    }

    @Transactional(readOnly = true)
    public List<ReportSubmission> getSubmissionHistoryForUser(Long registrationId, String username) {
        getRegistrationForUser(registrationId, username);
        return reportRepository.findByRegistrationIdOrderByVersionDesc(registrationId);
    }

    @Transactional(readOnly = true)
    public TopicRegistration getRegistrationForUser(Long registrationId, String username) {
        TopicRegistration registration = getRegistration(registrationId);
        ensureCanView(registration, getUser(username));
        return registration;
    }

    @Transactional(readOnly = true)
    public ReportSubmission getSubmissionByIdForUser(Long id, String username) {
        ReportSubmission submission = reportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lượt nộp báo cáo"));
        ensureCanView(submission.getTopicRegistration(), getUser(username));
        return submission;
    }

    @PreAuthorize("hasAnyRole('LECTURER','FACULTY_MANAGER','ADMIN')")
    @Transactional(readOnly = true)
    public List<ReportSubmission> getSubmissionHistoryForEvaluation(Long registrationId) {
        getRegistration(registrationId);
        return reportRepository.findByRegistrationIdOrderByVersionDesc(registrationId);
    }

    private void ensureCanView(TopicRegistration registration, User viewer) {
        Set<RoleName> roles = viewer.getRoles().stream().map(role -> role.getName()).collect(java.util.stream.Collectors.toSet());
        if (roles.contains(RoleName.ADMIN) || roles.contains(RoleName.FACULTY_MANAGER)) return;
        if (roles.contains(RoleName.STUDENT)) {
            boolean belongsToGroup = registration.getStudentGroup().getMembers().stream()
                    .anyMatch(member -> member.getMember().getId().equals(viewer.getId()));
            if (belongsToGroup) return;
        }
        if (roles.contains(RoleName.LECTURER)) {
            boolean relatedLecturer = registration.getTopic().getProposer().getId().equals(viewer.getId())
                    || registration.getTopic().getAdvisors().stream().anyMatch(advisor -> advisor.getId().equals(viewer.getId()));
            if (relatedLecturer) return;
        }
        throw new AccessDeniedException("Bạn không có quyền xem báo cáo của nhóm này");
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessRuleException("Vui lòng chọn tập tin báo cáo để nộp");
        if (file.getSize() > MAX_FILE_SIZE) throw new BusinessRuleException("Tập tin báo cáo không được vượt quá 10 MB");
        String fileName = sanitizeOriginalFileName(file.getOriginalFilename());
        String extension = extensionOf(fileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessRuleException("Chỉ chấp nhận tập tin PDF, DOC, DOCX hoặc ZIP");
        }
    }

    private String sanitizeOriginalFileName(String value) {
        String fileName = value == null || value.isBlank() ? "report.pdf" : value.replace('\\', '/');
        try {
            fileName = Path.of(fileName).getFileName().toString();
        } catch (InvalidPathException exception) {
            throw new BusinessRuleException("Tên tập tin không hợp lệ");
        }
        if (fileName.contains("..") || fileName.length() > 255) throw new BusinessRuleException("Tên tập tin không hợp lệ");
        return fileName;
    }

    private String extensionOf(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 1 || dotIndex == fileName.length() - 1) throw new BusinessRuleException("Tập tin phải có phần mở rộng hợp lệ");
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private void ensureReportDeadline(RegistrationPeriod period) {
        LocalDateTime deadline = effectiveReportDeadline(period);
        if (deadline != null && LocalDateTime.now(clock).isAfter(deadline)) {
            throw new BusinessRuleException("Đã hết hạn nộp báo cáo");
        }
    }

    private LocalDateTime effectiveReportDeadline(RegistrationPeriod period) {
        return period.getReportSubmissionDeadline() != null ? period.getReportSubmissionDeadline() : period.getStudentEnd();
    }

    private TopicRegistration getRegistration(Long id) {
        return registrationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đăng ký đề tài"));
    }

    private User getUser(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
    }
}
