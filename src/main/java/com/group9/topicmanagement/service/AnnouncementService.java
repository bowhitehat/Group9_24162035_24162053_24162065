package com.group9.topicmanagement.service;

import com.group9.topicmanagement.domain.Role;
import com.group9.topicmanagement.domain.User;
import com.group9.topicmanagement.domain.announcement.Announcement;
import com.group9.topicmanagement.domain.enums.AnnouncementStatus;
import com.group9.topicmanagement.domain.enums.RoleName;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.exception.NotFoundException;
import com.group9.topicmanagement.repository.AnnouncementRepository;
import com.group9.topicmanagement.repository.RoleRepository;
import com.group9.topicmanagement.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class AnnouncementService {
    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public AnnouncementService(AnnouncementRepository announcementRepository,
                               UserRepository userRepository,
                               RoleRepository roleRepository) {
        this.announcementRepository = announcementRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY_MANAGER')")
    public Announcement saveAnnouncement(String title, String content, List<String> targetRoles,
                                         String creatorUsername, boolean publish, LocalDateTime expirationTime) {
        User creator = userRepository.findByUsernameIgnoreCase(creatorUsername)
                .orElseThrow(() -> new NotFoundException("Người tạo không tồn tại"));

        Announcement announcement = new Announcement();
        announcement.setTitle(title);
        announcement.setContent(HtmlUtils.htmlEscape(content == null ? "" : content));
        announcement.setCreator(creator);
        announcement.setTargetRoles(resolveRoles(targetRoles));
        announcement.setExpirationTime(expirationTime);

        if (publish) {
            requireTargetRoles(announcement);
            validateExpiration(expirationTime);
            announcement.setStatus(AnnouncementStatus.PUBLISHED);
            announcement.setPublishedTime(LocalDateTime.now());
        } else {
            announcement.setStatus(AnnouncementStatus.DRAFT);
        }
        return announcementRepository.save(announcement);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY_MANAGER')")
    public void publish(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông báo"));
        if (announcement.getStatus() == AnnouncementStatus.ARCHIVED) {
            throw new BusinessRuleException("Không công bố thông báo đã lưu trữ");
        }
        requireTargetRoles(announcement);
        validateExpiration(announcement.getExpirationTime());
        announcement.setStatus(AnnouncementStatus.PUBLISHED);
        announcement.setPublishedTime(LocalDateTime.now());
        announcementRepository.save(announcement);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY_MANAGER')")
    public void archive(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông báo"));
        announcement.setStatus(AnnouncementStatus.ARCHIVED);
        announcementRepository.save(announcement);
    }

    @Transactional(readOnly = true)
    public List<Announcement> getActiveAnnouncementsForRole(String roleName) {
        return getActiveAnnouncementsForRoles(Set.of(roleName));
    }

    @Transactional(readOnly = true)
    public List<Announcement> getActiveAnnouncementsForRoles(Set<String> roleNames) {
        LocalDateTime now = LocalDateTime.now();
        return announcementRepository.findPublishedWithRoles(AnnouncementStatus.PUBLISHED).stream()
                .filter(a -> a.getExpirationTime() == null || a.getExpirationTime().isAfter(now))
                .filter(a -> a.getTargetRoles() != null && !a.getTargetRoles().isEmpty())
                .filter(a -> a.getTargetRoles().stream()
                        .anyMatch(r -> roleNames.stream().anyMatch(name -> r.getName().name().equalsIgnoreCase(name))))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Announcement> listForManager() {
        return announcementRepository.findAllWithRoles();
    }

    public boolean canPublish(Announcement announcement) {
        return announcement.getStatus() == AnnouncementStatus.DRAFT
                && announcement.getTargetRoles() != null
                && !announcement.getTargetRoles().isEmpty()
                && (announcement.getExpirationTime() == null
                    || announcement.getExpirationTime().isAfter(LocalDateTime.now()));
    }

    public boolean canArchive(Announcement announcement) {
        return announcement.getStatus() != AnnouncementStatus.ARCHIVED;
    }

    private Set<Role> resolveRoles(List<String> roleNames) {
        Set<Role> roles = new LinkedHashSet<>();
        if (roleNames == null || roleNames.isEmpty()) {
            return roles;
        }
        for (String value : roleNames) {
            final String trimmedValue = value.trim();
            if (trimmedValue.isEmpty()) {
                continue;
            }
            RoleName name = RoleName.valueOf(trimmedValue.toUpperCase());
            Role role = roleRepository.findByName(name)
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy vai trò " + trimmedValue));
            roles.add(role);
        }
        return roles;
    }

    private void requireTargetRoles(Announcement announcement) {
        if (announcement.getTargetRoles() == null || announcement.getTargetRoles().isEmpty()) {
            throw new BusinessRuleException("Phải chọn ít nhất một vai trò nhận thông báo trước khi công bố");
        }
    }

    private void validateExpiration(LocalDateTime expirationTime) {
        if (expirationTime != null && !expirationTime.isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Thời gian hết hạn phải ở tương lai");
        }
    }
}
