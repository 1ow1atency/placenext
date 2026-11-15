package com.placenext.placenext.controller;

import com.placenext.placenext.dto.DriveEligibility;
import com.placenext.placenext.model.*;
import com.placenext.placenext.repository.RecruiterProfileRepository;
import com.placenext.placenext.repository.StudentProfileRepository;
import com.placenext.placenext.repository.UserRepository;
import com.placenext.placenext.service.DriveService;
import com.placenext.placenext.service.EligibilityService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class DashboardController {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final DriveService driveService;
    private final EligibilityService eligibilityService;

    public DashboardController(UserRepository userRepository,
                               StudentProfileRepository studentProfileRepository,
                               RecruiterProfileRepository recruiterProfileRepository,
                               DriveService driveService,
                               EligibilityService eligibilityService) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.recruiterProfileRepository = recruiterProfileRepository;
        this.driveService = driveService;
        this.eligibilityService = eligibilityService;
    }

    @GetMapping("/student/dashboard")
    public String studentDashboard(Authentication auth, Model model) {
        User user = currentUser(auth);
        StudentProfile profile = studentProfileRepository.findByUser(user).orElse(null);

        List<DriveEligibility> eligible = eligibilityService.evaluate(profile, driveService.openDrives())
                .stream().filter(DriveEligibility::isEligible).toList();

        model.addAttribute("user", user);
        model.addAttribute("profile", profile);
        model.addAttribute("profileComplete", profile != null && profile.isComplete());
        model.addAttribute("eligibleCount", eligible.size());
        model.addAttribute("upcoming", eligible.stream().limit(3).toList());
        return "student/dashboard";
    }

    @GetMapping("/recruiter/dashboard")
    public String recruiterDashboard(Authentication auth, Model model) {
        User user = currentUser(auth);
        RecruiterProfile recruiter = recruiterProfileRepository.findByUser(user).orElse(null);
        List<Drive> drives = recruiter == null ? List.of() : driveService.forRecruiter(recruiter);

        long active = drives.stream()
                .filter(d -> d.getStatus() == DriveStatus.APPROVED && !d.isDeadlinePassed()).count();
        long pending = drives.stream()
                .filter(d -> d.getStatus() == DriveStatus.PENDING).count();

        model.addAttribute("user", user);
        model.addAttribute("recruiter", recruiter);
        model.addAttribute("activeCount", active);
        model.addAttribute("pendingCount", pending);
        model.addAttribute("recentDrives", drives.stream().limit(3).toList());
        return "recruiter/dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Authentication auth, Model model) {
        List<Drive> pending = driveService.pending();

        model.addAttribute("user", currentUser(auth));
        model.addAttribute("studentCount", userRepository.countByRole(Role.STUDENT));
        model.addAttribute("recruiterCount", userRepository.countByRole(Role.RECRUITER));
        model.addAttribute("driveCount", driveService.countAll());
        model.addAttribute("pendingCount", pending.size());
        model.addAttribute("pendingDrives", pending.stream().limit(5).toList());
        return "admin/dashboard";
    }

    private User currentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName()).orElseThrow();
    }
}