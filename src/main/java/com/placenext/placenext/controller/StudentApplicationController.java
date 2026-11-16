package com.placenext.placenext.controller;

import com.placenext.placenext.model.Application;
import com.placenext.placenext.model.Drive;
import com.placenext.placenext.model.Round;
import com.placenext.placenext.model.StudentProfile;
import com.placenext.placenext.model.User;
import com.placenext.placenext.repository.StudentProfileRepository;
import com.placenext.placenext.repository.UserRepository;
import com.placenext.placenext.service.ApplicationService;
import com.placenext.placenext.service.DriveService;
import com.placenext.placenext.service.RoundResultService;
import com.placenext.placenext.service.RoundService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class StudentApplicationController {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final DriveService driveService;
    private final ApplicationService applicationService;
    private final RoundService roundService;
    private final RoundResultService roundResultService;

    public StudentApplicationController(UserRepository userRepository,
                                        StudentProfileRepository studentProfileRepository,
                                        DriveService driveService,
                                        ApplicationService applicationService,
                                        RoundService roundService,
                                        RoundResultService roundResultService) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.driveService = driveService;
        this.applicationService = applicationService;
        this.roundService = roundService;
        this.roundResultService = roundResultService;
    }

    @GetMapping("/student/applications")
    public String list(Authentication auth, Model model) {
        StudentProfile profile = currentProfile(auth);
        List<Application> applications = profile == null
                ? List.of()
                : applicationService.forStudent(profile);

        Map<Long, List<Round>> rounds = new HashMap<>();
        for (Application a : applications) {
            Long driveId = a.getDrive().getId();
            if (!rounds.containsKey(driveId)) {
                rounds.put(driveId, roundService.forDrive(a.getDrive()));
            }
        }

        model.addAttribute("applications", applications);
        model.addAttribute("rounds", rounds);
        model.addAttribute("results", roundResultService.forApplications(applications));
        return "student/applications";
    }

    @PostMapping("/student/drives/{driveId}/apply")
    public String apply(@PathVariable("driveId") Long driveId, Authentication auth, RedirectAttributes ra) {
        StudentProfile profile = currentProfile(auth);
        Drive drive = driveService.findById(driveId).orElse(null);
        if (profile == null || drive == null) {
            ra.addFlashAttribute("error", "Complete your profile before applying.");
            return "redirect:/student/drives";
        }
        try {
            applicationService.apply(profile, drive);
            ra.addFlashAttribute("success", "Application submitted. Good luck!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/drives/" + driveId;
    }

    @PostMapping("/student/applications/{id}/withdraw")
    public String withdraw(@PathVariable("id") Long id, Authentication auth, RedirectAttributes ra) {
        StudentProfile profile = currentProfile(auth);
        if (profile == null) {
            return "redirect:/student/applications";
        }
        try {
            applicationService.withdraw(profile, id);
            ra.addFlashAttribute("success", "Application withdrawn.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/applications";
    }

    private StudentProfile currentProfile(Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        return studentProfileRepository.findByUser(user).orElse(null);
    }
}