package com.srirammart.config;

import com.srirammart.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(10); }

    /** Sends admins and sellers to their own consoles after login; buyers return to where they were going. */
    static class RoleBasedSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
        @Override
        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                            Authentication authentication) throws IOException, ServletException {
            for (GrantedAuthority a : authentication.getAuthorities()) {
                if (a.getAuthority().equals("ROLE_ADMIN")) { getRedirectStrategy().sendRedirect(request, response, "/admin"); return; }
                if (a.getAuthority().equals("ROLE_SELLER")) { getRedirectStrategy().sendRedirect(request, response, "/seller"); return; }
            }
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AppProperties props, UserService users) throws Exception {
        AuthenticationFailureHandler failure = (request, response, ex) -> {
            String code = ex instanceof LockedException ? "locked" : ex instanceof DisabledException ? "disabled" : "bad";
            response.sendRedirect(request.getContextPath() + "/login?error=" + code);
        };
        http
            .authorizeHttpRequests(a -> {
                a.requestMatchers("/css/**", "/js/**", "/img/**", "/uploads/**", "/favicon.ico", "/error", "/login", "/register").permitAll();
                a.requestMatchers("/admin/**").hasRole("ADMIN");
                a.requestMatchers("/seller/**").hasRole("SELLER");
                a.requestMatchers("/cart/**", "/checkout/**", "/wishlist/**", "/orders/**", "/api/cart/**", "/api/wishlist/**").hasRole("BUYER");
                a.anyRequest().authenticated();
            })
            .formLogin(f -> f.loginPage("/login").loginProcessingUrl("/login")
                    .successHandler(new RoleBasedSuccessHandler()).failureHandler(failure).permitAll())
            .logout(l -> l.logoutUrl("/logout").logoutSuccessUrl("/login?logout").invalidateHttpSession(true).deleteCookies("JSESSIONID"))
            .rememberMe(r -> r.key(props.getRememberMeKey()).tokenValiditySeconds(14 * 24 * 3600).userDetailsService(users))
            .sessionManagement(s -> s.sessionFixation(f -> f.migrateSession()))
            .headers(h -> h
                .contentSecurityPolicy(c -> c.policyDirectives("default-src 'self'; img-src 'self' data:; "
                        + "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; "
                        + "script-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'"))
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny));
        return http.build();
    }
}
