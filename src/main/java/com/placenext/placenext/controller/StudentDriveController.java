package com.placenext.placenext.controller;

import com.placenext.placenext.dto.DriveEligibility;
import com.placenext.placenext.model.Application;
import com.placenext.placenext.model.ApplicationStatus;
import com.placenext.placenext.model.Drive;
import com.placenext.placenext.model.DriveStatus;
import com.placenext.placenext.model.StudentProfile;
import com.placenext.placenext.model.User;
import com.placenext.placenext.repository.StudentProfileRepository;
import com.placenext.placenext.repository.UserRepository;
import com.placenext.placenext.service.ApplicationService;
import com.placenext.placenext.service.DriveService;
import com.placenext.placenext.service.EligibilityService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/student/drives")
public class StudentDriveController {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final DriveService driveService;
    private final EligibilityService eligibilityService;
    private final ApplicationService applicationService;

    public StudentDriveController(UserRepository userRepository,
                                  StudentProfileRepository studentProfileRepository,
                                  DriveService driveService,
                                  EligibilityService eligibilityService,
                                  ApplicationService applicationService) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.driveService = driveService;
        this.eligibilityService = eligibilityService;
        this.applicationService = applicationService;
    }

    @GetMapping
    public String list(Authentication auth, Model model) {
        StudentProfile profile = currentProfile(auth);
        List<DriveEligibility> results = eligibilityService.evaluate(profile, driveService.openDrives());

        model.addAttribute("profile", profile);
        model.addAttribute("eligible", results.stream().filter(DriveEligibility::isEligible).toList());
        model.addAttribute("notEligible", results.stream().filter(r -> !r.isEligible()).toList());
        return "student/drives";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id, Authentication auth, Model model) {
        Drive drive = driveService.findById(id).orElse(null);
        if (drive == null || drive.getStatus() != DriveStatus.APPROVED) {
            return "redirect:/student/drives";
        }
        StudentProfile profile = currentProfile(auth);
        Application application = profile == null ? null : applicationService.find(profile, drive).orElse(null);

        boolean hasApplied = application != null && application.getStatus() != ApplicationStatus.WITHDRAWN;
        boolean wasWithdrawn = application != null && application.getStatus() == ApplicationStatus.WITHDRAWN;
        List<String> reasons = eligibilityService.check(profile, drive);

        model.addAttribute("drive", drive);
        model.addAttribute("reasons", reasons);
        model.addAttribute("hasApplied", hasApplied);
        model.addAttribute("wasWithdrawn", wasWithdrawn);
        model.addAttribute("statusLabel", hasApplied ? application.getStatus().getLabel() : "");
        model.addAttribute("statusClass", hasApplied ? "status-" + application.getStatus().name() : "");
        model.addAttribute("deadlinePassed", drive.isDeadlinePassed());
        model.addAttribute("canApply", !hasApplied && reasons.isEmpty() && !drive.isDeadlinePassed());
        return "student/drive-detail";
    }

    private StudentProfile currentProfile(Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        return studentProfileRepository.findByUser(user).orElse(null);
    }
}