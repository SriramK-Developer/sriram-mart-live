package com.srirammart.web;

import com.srirammart.config.AppUserDetails;
import com.srirammart.model.User;
import com.srirammart.service.UserService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AccountController {
    private final UserService users;

    public AccountController(UserService users) { this.users = users; }

    @GetMapping("/account")
    public String view(@AuthenticationPrincipal AppUserDetails me, Model model) {
        User u = users.get(me);
        if (!model.containsAttribute("form")) {
            ProfileForm f = new ProfileForm();
            f.setFullName(u.getFullName());
            f.setPhone(u.getPhone());
            f.setAddressLine(u.getAddressLine());
            f.setCity(u.getCity());
            f.setStateName(u.getStateName());
            f.setPincode(u.getPincode());
            model.addAttribute("form", f);
        }
        model.addAttribute("pageTitle", "My Account");
        return "account";
    }

    @PostMapping("/account/profile")
    public String profile(ProfileForm form, @AuthenticationPrincipal AppUserDetails me, Model model, RedirectAttributes ra) {
        User u = users.get(me);
        List<String> errors = new ArrayList<>();
        users.updateProfile(u, form, errors);
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            model.addAttribute("form", form);
            model.addAttribute("pageTitle", "My Account");
            return "account";
        }
        ra.addFlashAttribute("flashOk", "Profile updated.");
        return "redirect:/account";
    }

    @PostMapping("/account/password")
    public String password(@RequestParam String current, @RequestParam String next, @RequestParam String confirm,
                           @AuthenticationPrincipal AppUserDetails me, RedirectAttributes ra) {
        List<String> errors = users.changePassword(users.get(me), current, next, confirm);
        if (errors.isEmpty()) ra.addFlashAttribute("flashOk", "Password changed successfully.");
        else ra.addFlashAttribute("flashError", String.join(" ", errors));
        return "redirect:/account";
    }
}
