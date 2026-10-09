package com.srirammart.service;

import com.srirammart.config.AppUserDetails;
import com.srirammart.model.Role;
import com.srirammart.model.User;
import com.srirammart.repo.UserRepository;
import com.srirammart.web.ProfileForm;
import com.srirammart.web.RegisterForm;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService implements UserDetailsService {
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_.]{4,30}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^[0-9]{10}$");
    private static final Pattern PINCODE = Pattern.compile("^[1-9][0-9]{5}$");
    private static final Pattern PASSWORD = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,64}$");

    private final UserRepository repo;
    private final PasswordEncoder encoder;
    private final NotificationService notifications;

    public UserService(UserRepository repo, PasswordEncoder encoder, NotificationService notifications) {
        this.repo = repo;
        this.encoder = encoder;
        this.notifications = notifications;
    }

    @Override
    public UserDetails loadUserByUsername(String input) throws UsernameNotFoundException {
        String key = input == null ? "" : input.trim();
        User u = repo.findByUsernameIgnoreCase(key).or(() -> repo.findByEmailIgnoreCase(key))
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
        return new AppUserDetails(u);
    }

    public User get(AppUserDetails d) { return repo.findById(d.getId()).orElseThrow(); }
    public User get(Long id) { return repo.findById(id).orElseThrow(); }

    public List<String> validate(RegisterForm f) {
        List<String> e = new ArrayList<>();
        if (blank(f.getFullName()) || f.getFullName().trim().length() < 2) e.add("Enter your full name.");
        if (blank(f.getUsername()) || !USERNAME.matcher(f.getUsername().trim()).matches())
            e.add("Username must be 4-30 characters: letters, numbers, dot or underscore.");
        else if (repo.existsByUsernameIgnoreCase(f.getUsername().trim())) e.add("That username is already taken.");
        if (blank(f.getEmail()) || !EMAIL.matcher(f.getEmail().trim()).matches()) e.add("Enter a valid email address.");
        else if (repo.existsByEmailIgnoreCase(f.getEmail().trim())) e.add("An account with this email already exists.");
        if (!blank(f.getPhone()) && !PHONE.matcher(f.getPhone().trim()).matches()) e.add("Phone number must be 10 digits.");
        e.addAll(passwordErrors(f.getPassword()));
        if (f.getPassword() != null && !f.getPassword().equals(f.getConfirm())) e.add("Passwords do not match.");
        if ("SELLER".equals(f.getAccountType()) && blank(f.getStoreName())) e.add("Enter your store name to register as a seller.");
        return e;
    }

    public List<String> passwordErrors(String pw) {
        List<String> e = new ArrayList<>();
        if (pw == null || !PASSWORD.matcher(pw).matches())
            e.add("Password needs 8+ characters with an uppercase letter, a lowercase letter and a number.");
        return e;
    }

    @Transactional
    public User register(RegisterForm f) {
        User u = new User();
        u.setFullName(f.getFullName().trim());
        u.setUsername(f.getUsername().trim());
        u.setEmail(f.getEmail().trim().toLowerCase());
        u.setPhone(blank(f.getPhone()) ? null : f.getPhone().trim());
        u.setPasswordHash(encoder.encode(f.getPassword()));
        boolean seller = "SELLER".equals(f.getAccountType());
        u.setRole(seller ? Role.SELLER : Role.BUYER);
        if (seller) {
            u.setStoreName(f.getStoreName().trim());
            u.setApproved(false);
            u.setStoreRating(0.0);
            u.setStoreRatings(0);
        }
        u = repo.save(u);
        if (!seller) {
            notifications.push(u, com.srirammart.model.NotificationType.SYSTEM, "Welcome to SriramMart",
                    "Your account is ready. Explore today's deals and use code WELCOME10 on your first order.", "/deals", "View deals");
        }
        return u;
    }

    @Transactional
    public void updateProfile(User u, ProfileForm f, List<String> errors) {
        if (blank(f.getFullName()) || f.getFullName().trim().length() < 2) errors.add("Enter your full name.");
        if (!blank(f.getPhone()) && !PHONE.matcher(f.getPhone().trim()).matches()) errors.add("Phone number must be 10 digits.");
        if (!blank(f.getPincode()) && !PINCODE.matcher(f.getPincode().trim()).matches()) errors.add("Pincode must be 6 digits.");
        if (!errors.isEmpty()) return;
        u.setFullName(f.getFullName().trim());
        u.setPhone(blank(f.getPhone()) ? null : f.getPhone().trim());
        u.setAddressLine(trimOrNull(f.getAddressLine()));
        u.setCity(trimOrNull(f.getCity()));
        u.setStateName(trimOrNull(f.getStateName()));
        u.setPincode(trimOrNull(f.getPincode()));
        repo.save(u);
    }

    @Transactional
    public List<String> changePassword(User u, String current, String next, String confirm) {
        List<String> e = new ArrayList<>();
        if (current == null || !encoder.matches(current, u.getPasswordHash())) e.add("Current password is incorrect.");
        e.addAll(passwordErrors(next));
        if (next != null && !next.equals(confirm)) e.add("New passwords do not match.");
        if (e.isEmpty()) {
            u.setPasswordHash(encoder.encode(next));
            repo.save(u);
            notifications.push(u, com.srirammart.model.NotificationType.ACCOUNT, "Account security",
                    "Your password was successfully changed.", null, null);
        }
        return e;
    }

    @Transactional
    public void setEnabled(Long id, boolean enabled) {
        User u = repo.findById(id).orElseThrow();
        if (u.getRole() == Role.ADMIN) return; // never lock out admins from the UI
        u.setEnabled(enabled);
        repo.save(u);
    }

    @Transactional
    public void approveSeller(Long id) {
        User u = repo.findById(id).orElseThrow();
        if (u.getRole() == Role.SELLER) { u.setApproved(true); repo.save(u); }
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }
    private static String trimOrNull(String s) { return blank(s) ? null : s.trim(); }
}
