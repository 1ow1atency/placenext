package com.placenext.placenext.controller;

import com.placenext.placenext.model.Role;
import com.placenext.placenext.model.User;
import com.placenext.placenext.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final UserRepository userRepository;

    public DashboardController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/student/dashboard")
    public String studentDashboard(Authentication auth, Model model) {
        model.addAttribute("user", currentUser(auth));
        return "student/dashboard";
    }

    @GetMapping("/recruiter/dashboard")
    public String recruiterDashboard(Authentication auth, Model model) {
        model.addAttribute("user", currentUser(auth));
        return "recruiter/dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Authentication auth, Model model) {
        model.addAttribute("user", currentUser(auth));
        model.addAttribute("studentCount", userRepository.countByRole(Role.STUDENT));
        model.addAttribute("recruiterCount", userRepository.countByRole(Role.RECRUITER));
        return "admin/dashboard";
    }

    private User currentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName()).orElseThrow();
    }
}
