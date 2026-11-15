package com.placenext.placenext;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Authentication auth) {
        if (hasRole(auth, "ROLE_ADMIN")) return "redirect:/admin/dashboard";
        if (hasRole(auth, "ROLE_RECRUITER")) return "redirect:/recruiter/dashboard";
        return "redirect:/student/dashboard";
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }
}