package com.group9.topicmanagement.service;

import com.group9.topicmanagement.config.UploadConfig;
import com.group9.topicmanagement.domain.User;
import com.group9.topicmanagement.domain.enums.RegistrationStatus;
import com.group9.topicmanagement.domain.registration.ReportSubmission;
import com.group9.topicmanagement.domain.registration.TopicRegistration;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.repository.ReportSubmissionRepository;
import com.group9.topicmanagement.repository.TopicRegistrationRepository;
import com.group9.topicmanagement.repository.UserRepository;
import com.group9.topicmanagement.web.form.ReportSubmissionForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ReportSubmissionService {

    private final ReportSubmissionRepository reportRepository;
    private final TopicRegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final UploadConfig uploadConfig;

    public ReportSubmissionService(ReportSubmissionRepository reportRepository,
                                   TopicRegistrationRepository registrationRepository,
                                   UserRepository userRepository,
                                   UploadConfig uploadConfig) {
        this.reportRepository = reportRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.uploadConfig = uploadConfig;
    }

    public ReportSubmission submitReport(ReportSubmissionForm form, String submitterUsername) {
        TopicRegistration registration = registrationRepository.findById(form.getTopicRegistrationId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký đề tài"));

        if (!registration.getStudentGroup().getLeader().getUsername().equalsIgnoreCase(submitterUsername)) {
            throw new BusinessRuleException("Chỉ trưởng nhóm mới được quyền nộp báo cáo");
        }

        if (registration.getStatus() != RegistrationStatus.APPROVED) {
            throw new BusinessRuleException("Chỉ có thể nộp báo cáo khi đề tài đã được phê duyệt chính thức");
        }

        MultipartFile file = form.getFile();
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Vui lòng chọn tập tin báo cáo để nộp");
        }

        User submitter = userRepository.findByUsernameIgnoreCase(submitterUsername)
                .orElseThrow(() -> new BusinessRuleException("Không tìm thấy người nộp"));

        // Tính version
        Optional<ReportSubmission> latestOpt = reportRepository.findLatestByRegistrationId(registration.getId());
        int nextVersion = latestOpt.map(r -> r.getVersion() + 1).orElse(1);

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null) originalFileName = "report.pdf";

        String extension = "";
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalFileName.substring(dotIndex);
        }

        String storedFileName = UUID.randomUUID().toString() + extension;
        Path targetPath = uploadConfig.getUploadDirectory().resolve(storedFileName);

        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Không thể lưu tập tin báo cáo", e);
        }

        ReportSubmission submission = new ReportSubmission();
        submission.setStudentGroup(registration.getStudentGroup());
        submission.setTopicRegistration(registration);
        submission.setSubmitter(submitter);
        submission.setOriginalFileName(originalFileName);
        submission.setStoredFileName(storedFileName);
        submission.setContentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        submission.setFileSize(file.getSize());
        submission.setVersion(nextVersion);
        submission.setNote(form.getNote());

        return reportRepository.save(submission);
    }

    public List<ReportSubmission> getSubmissionHistory(Long registrationId) {
        return reportRepository.findByRegistrationIdOrderByVersionDesc(registrationId);
    }

    public ReportSubmission getSubmissionById(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lượt nộp báo cáo"));
    }
}
