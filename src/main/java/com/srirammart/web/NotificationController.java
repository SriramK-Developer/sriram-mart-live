package com.srirammart.web;

import com.srirammart.config.AppUserDetails;
import com.srirammart.model.Notification;
import com.srirammart.model.NotificationType;
import com.srirammart.model.User;
import com.srirammart.service.NotificationService;
import com.srirammart.service.UserService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class NotificationController {
    private final NotificationService notifications;
    private final UserService users;

    public NotificationController(NotificationService notifications, UserService users) {
        this.notifications = notifications;
        this.users = users;
    }

    @GetMapping("/notifications")
    public String list(@RequestParam(defaultValue = "ALL") String tab, @AuthenticationPrincipal AppUserDetails me, Model model) {
        User u = users.get(me);
        List<Notification> all = notifications.forUser(u);
        NotificationType filter = null;
        for (NotificationType t : NotificationType.values()) if (t.name().equalsIgnoreCase(tab)) filter = t;
        List<Notification> shown = new ArrayList<>();
        for (Notification n : all) if (filter == null || n.getType() == filter) shown.add(n);
        model.addAttribute("notes", shown);
        model.addAttribute("tab", filter == null ? "ALL" : filter.name());
        model.addAttribute("totalCount", all.size());
        model.addAttribute("types", NotificationType.values());
        model.addAttribute("typeCounts", new java.util.LinkedHashMap<String, Long>() {{
            for (NotificationType t : NotificationType.values()) put(t.name(), notifications.count(u, t));
        }});
        model.addAttribute("pageTitle", "Notifications");
        return "notifications";
    }

    @PostMapping("/notifications/read-all")
    public String readAll(@AuthenticationPrincipal AppUserDetails me) {
        notifications.markAllSeen(users.get(me));
        return "redirect:/notifications";
    }
}
