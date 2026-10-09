package com.srirammart.web;

import com.srirammart.model.User;
import com.srirammart.service.UserService;
import java.util.List;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {
    private final UserService users;

    public AuthController(UserService users) { this.users = users; }

    private static boolean signedIn(Authentication a) { return a != null && a.isAuthenticated() && !(a instanceof AnonymousAuthenticationToken); }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error, @RequestParam(required = false) String logout,
                        @RequestParam(required = false) String tab, Authentication auth, Model model) {
        if (signedIn(auth)) return "redirect:/";
        String msg = null;
        if ("locked".equals(error)) msg = "Too many failed attempts. This account is locked for 15 minutes.";
        else if ("disabled".equals(error)) msg = "This account is disabled or waiting for admin approval.";
        else if (error != null) msg = "Incorrect username or password.";
        model.addAttribute("error", msg);
        model.addAttribute("loggedOut", logout != null);
        model.addAttribute("tab", "register".equals(tab) ? "register" : "login");
        if (!model.containsAttribute("form")) model.addAttribute("form", new RegisterForm());
        return "login";
    }

    @PostMapping("/register")
    public String register(RegisterForm form, Model model) {
        List<String> errors = users.validate(form);
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            model.addAttribute("form", form);
            model.addAttribute("tab", "register");
            return "login";
        }
        User u = users.register(form);
        model.addAttribute("form", new RegisterForm());
        model.addAttribute("tab", "login");
        model.addAttribute("success", u.isSeller()
                ? "Seller account created. You can sign in once an admin approves your store."
                : "Account created. Sign in with your username and password.");
        return "login";
    }
}
