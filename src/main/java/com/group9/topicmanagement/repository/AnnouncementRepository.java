package com.group9.topicmanagement.repository;
import com.group9.topicmanagement.domain.announcement.Announcement;
import com.group9.topicmanagement.domain.enums.AnnouncementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByStatusOrderByPublishedTimeDesc(AnnouncementStatus status);
}