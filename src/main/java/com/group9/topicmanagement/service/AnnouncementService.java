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
    public Announcement saveAnnouncement(String title, String content, String targetRolesCsv,
                                         String creatorUsername, boolean publish, LocalDateTime expirationTime) {
        User creator = userRepository.findByUsernameIgnoreCase(creatorUsername)
                .orElseThrow(() -> new NotFoundException("Người tạo không tồn tại"));

        Announcement announcement = new Announcement();
        announcement.setTitle(title);
        announcement.setContent(HtmlUtils.htmlEscape(content == null ? "" : content));
        announcement.setCreator(creator);
        announcement.setTargetRoles(resolveRoles(targetRolesCsv));
        announcement.setExpirationTime(expirationTime);

        if (publish) {
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
        LocalDateTime now = LocalDateTime.now();
        return announcementRepository.findPublishedWithRoles(AnnouncementStatus.PUBLISHED).stream()
                .filter(a -> a.getExpirationTime() == null || a.getExpirationTime().isAfter(now))
                .filter(a -> a.getTargetRoles() == null || a.getTargetRoles().isEmpty()
                        || a.getTargetRoles().stream().anyMatch(r -> r.getName().name().equalsIgnoreCase(roleName)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Announcement> listForManager() {
        return announcementRepository.findAllWithRoles();
    }

    private Set<Role> resolveRoles(String csv) {
        Set<Role> roles = new LinkedHashSet<>();
        if (csv == null || csv.isBlank()) {
            return roles;
        }
        for (String token : csv.split(",")) {
            String value = token.trim();
            if (value.isEmpty()) {
                continue;
            }
            RoleName name = RoleName.valueOf(value.toUpperCase());
            Role role = roleRepository.findByName(name)
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy vai trò " + value));
            roles.add(role);
        }
        return roles;
    }
}
