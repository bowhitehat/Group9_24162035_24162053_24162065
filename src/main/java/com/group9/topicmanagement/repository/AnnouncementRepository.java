package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.domain.announcement.Announcement;
import com.group9.topicmanagement.domain.enums.AnnouncementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByStatusOrderByPublishedTimeDesc(AnnouncementStatus status);

    @Query("SELECT DISTINCT a FROM Announcement a LEFT JOIN FETCH a.targetRoles LEFT JOIN FETCH a.creator ORDER BY a.createdAt DESC")
    List<Announcement> findAllWithRoles();

    @Query("SELECT DISTINCT a FROM Announcement a LEFT JOIN FETCH a.targetRoles WHERE a.status = :status ORDER BY a.publishedTime DESC")
    List<Announcement> findPublishedWithRoles(AnnouncementStatus status);
}
