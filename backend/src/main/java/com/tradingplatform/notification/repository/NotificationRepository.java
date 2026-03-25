package com.tradingplatform.notification.repository;

import com.tradingplatform.notification.entity.Notification;
import com.tradingplatform.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Notification entity.
 * Supports NOTF-03 (view notification history) and NOTF-04 (mark as read).
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Find all notifications for a user, ordered by creation date descending.
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find all notifications for a user without pagination.
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Count unread notifications for a user.
     */
    Long countByUserIdAndReadFalse(Long userId);

    /**
     * Mark a single notification as read.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :readAt WHERE n.id = :id AND n.userId = :userId")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    /**
     * Mark all notifications as read for a user.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :readAt WHERE n.userId = :userId AND n.read = false")
    int markAllAsReadByUserId(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    /**
     * Find unread notifications for a user.
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.read = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByUserId(@Param("userId") Long userId);

    /**
     * Find top 50 unread notifications for a user.
     * Per D-13: Notification history limited to 50 unread.
     */
    @Query(value = "SELECT * FROM notifications WHERE user_id = :userId AND is_read = false ORDER BY created_at DESC LIMIT 50", nativeQuery = true)
    List<Notification> findTop50UnreadByUserId(@Param("userId") Long userId);

    /**
     * Checks whether an unread notification of the same type already exists for the reference.
     */
    boolean existsByUserIdAndTypeAndReferenceIdAndReadFalse(Long userId, NotificationType type, Long referenceId);

    Optional<Notification> findFirstByUserIdAndTypeAndReferenceIdAndReferenceTypeAndReadFalseOrderByCreatedAtDesc(
        Long userId,
        NotificationType type,
        Long referenceId,
        String referenceType
    );

    /**
     * Delete notifications older than the specified date.
     * Per D-13: Notifications retained for 30 days.
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate")
    int deleteOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
}
