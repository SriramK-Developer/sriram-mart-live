package com.srirammart.repo;

import com.srirammart.model.Notification;
import com.srirammart.model.NotificationType;
import com.srirammart.model.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    long countByUserAndSeenFalse(User user);
    long countByUserAndType(User user, NotificationType type);

    @Transactional
    @Modifying
    @Query("update Notification n set n.seen = true where n.user = :u")
    void markAllSeen(@Param("u") User u);
}
