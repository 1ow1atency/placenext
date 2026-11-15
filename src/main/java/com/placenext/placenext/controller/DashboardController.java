package com.placenext.placenext.controller;

import com.placenext.placenext.model.RecruiterProfile;
import com.placenext.placenext.model.Role;
import com.placenext.placenext.model.StudentProfile;
import com.placenext.placenext.model.User;
import com.placenext.placenext.repository.RecruiterProfileRepository;
import com.placenext.placenext.repository.StudentProfileRepository;
import com.placenext.placenext.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;

    public DashboardController(UserRepository userRepository,
                               StudentProfileRepository studentProfileRepository,
                               RecruiterProfileRepository recruiterProfileRepository) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.recruiterProfileRepository = recruiterProfileRepository;
    }

    @GetMapping("/student/dashboard")
    public String studentDashboard(Authentication auth, Model model) {
        User user = currentUser(auth);
        StudentProfile profile = studentProfileRepository.findByUser(user).orElse(null);
        model.addAttribute("user", user);
        model.addAttribute("profile", profile);
        model.addAttribute("profileComplete", profile != null && profile.isComplete());
        return "student/dashboard";
    }

    @GetMapping("/recruiter/dashboard")
    public String recruiterDashboard(Authentication auth, Model model) {
        User user = currentUser(auth);
        RecruiterProfile recruiter = recruiterProfileRepository.findByUser(user).orElse(null);
        model.addAttribute("user", user);
        model.addAttribute("recruiter", recruiter);
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