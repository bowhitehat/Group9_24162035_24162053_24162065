package com.group9.topicmanagement.service;

import com.group9.topicmanagement.domain.User;
import com.group9.topicmanagement.domain.announcement.Announcement;
import com.group9.topicmanagement.domain.enums.AnnouncementStatus;
import com.group9.topicmanagement.exception.NotFoundException;
import com.group9.topicmanagement.repository.AnnouncementRepository;
import com.group9.topicmanagement.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AnnouncementService {
    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;

    public AnnouncementService(AnnouncementRepository announcementRepository, UserRepository userRepository) {
        this.announcementRepository = announcementRepository;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY_MANAGER')")
    public Announcement saveAnnouncement(String title, String content, String targetRoles, String creatorUsername, boolean publish, LocalDateTime expirationTime) {
        User creator = userRepository.findByUsernameIgnoreCase(creatorUsername)
                .orElseThrow(() -> new NotFoundException("Người tạo không tồn tại"));

        Announcement announcement = new Announcement();
        announcement.setTitle(title);
        announcement.setContent(content);
        announcement.setCreator(creator);
        announcement.setTargetRoles(targetRoles);
        announcement.setExpirationTime(expirationTime);
        
        if (publish) {
            announcement.setStatus(AnnouncementStatus.PUBLISHED);
            announcement.setPublishedTime(LocalDateTime.now());
        } else {
            announcement.setStatus(AnnouncementStatus.DRAFT);
        }

        return announcementRepository.save(announcement);
    }

    public List<Announcement> getActiveAnnouncementsForRole(String roleName) {
        List<Announcement> published = announcementRepository.findByStatusOrderByPublishedTimeDesc(AnnouncementStatus.PUBLISHED);
        LocalDateTime now = LocalDateTime.now();
        
        return published.stream()
                .filter(a -> a.getExpirationTime() == null || a.getExpirationTime().isAfter(now))
                .filter(a -> a.getTargetRoles() == null || a.getTargetRoles().isEmpty() || a.getTargetRoles().contains(roleName))
                .collect(Collectors.toList());
    }
}
