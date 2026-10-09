package com.srirammart.service;

import com.srirammart.model.Notification;
import com.srirammart.model.NotificationType;
import com.srirammart.model.User;
import com.srirammart.repo.NotificationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository repo;

    public NotificationService(NotificationRepository repo) { this.repo = repo; }

    @Transactional
    public Notification push(User user, NotificationType type, String title, String message, String link, String linkLabel) {
        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setLink(link);
        n.setLinkLabel(linkLabel);
        return repo.save(n);
    }

    public List<Notification> forUser(User u) { return repo.findByUserOrderByCreatedAtDesc(u); }
    public long unread(User u) { return repo.countByUserAndSeenFalse(u); }
    public long count(User u, NotificationType t) { return repo.countByUserAndType(u, t); }

    @Transactional
    public void markAllSeen(User u) { repo.markAllSeen(u); }
}
