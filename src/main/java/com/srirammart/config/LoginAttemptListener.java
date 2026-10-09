package com.srirammart.config;

import com.srirammart.repo.UserRepository;
import java.time.LocalDateTime;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Locks an account for 15 minutes after 5 wrong passwords. */
@Component
public class LoginAttemptListener {
    private static final int MAX_ATTEMPTS = 5;
    private final UserRepository users;

    public LoginAttemptListener(UserRepository users) { this.users = users; }

    @EventListener
    @Transactional
    public void onFailure(AuthenticationFailureBadCredentialsEvent e) {
        String name = String.valueOf(e.getAuthentication().getName());
        users.findByUsernameIgnoreCase(name).or(() -> users.findByEmailIgnoreCase(name)).ifPresent(u -> {
            int n = u.getFailedAttempts() + 1;
            if (n >= MAX_ATTEMPTS) {
                u.setLockUntil(LocalDateTime.now().plusMinutes(15));
                n = 0;
            }
            u.setFailedAttempts(n);
            users.save(u);
        });
    }

    @EventListener
    @Transactional
    public void onSuccess(AuthenticationSuccessEvent e) {
        if (e.getAuthentication().getPrincipal() instanceof AppUserDetails d) {
            users.findById(d.getId()).ifPresent(u -> {
                if (u.getFailedAttempts() != 0 || u.getLockUntil() != null) {
                    u.setFailedAttempts(0);
                    u.setLockUntil(null);
                    users.save(u);
                }
            });
        }
    }
}
