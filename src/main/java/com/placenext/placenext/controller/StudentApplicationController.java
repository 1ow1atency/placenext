package com.placenext.placenext.controller;

import com.placenext.placenext.model.Drive;
import com.placenext.placenext.model.StudentProfile;
import com.placenext.placenext.model.User;
import com.placenext.placenext.repository.StudentProfileRepository;
import com.placenext.placenext.repository.UserRepository;
import com.placenext.placenext.service.ApplicationService;
import com.placenext.placenext.service.DriveService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/student")
public class StudentApplicationController {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final DriveService driveService;
    private final ApplicationService applicationService;

    public StudentApplicationController(UserRepository userRepository,
                                        StudentProfileRepository studentProfileRepository,
                                        DriveService driveService,
                                        ApplicationService applicationService) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.driveService = driveService;
        this.applicationService = applicationService;
    }

    @GetMapping("/applications")
    public String list(Authentication auth, Model model) {
        StudentProfile profile = currentProfile(auth);
        model.addAttribute("applications",
                profile == null ? java.util.List.of() : applicationService.forStudent(profile));
        return "student/applications";
    }

    @PostMapping("/drives/{id}/apply")
    public String apply(@PathVariable("id") Long id, Authentication auth, RedirectAttributes redirect) {
        StudentProfile profile = currentProfile(auth);
        Drive drive = driveService.findById(id).orElse(null);
        if (drive == null) {
            return "redirect:/student/drives";
        }
        if (profile == null || !profile.isComplete()) {
            redirect.addFlashAttribute("error", "Complete your profile and upload your resume first.");
            return "redirect:/student/drives/" + id;
        }
        try {
            applicationService.apply(profile, drive);
            redirect.addFlashAttribute("message", "Application submitted! Track it under My applications.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/drives/" + id;
    }

    @PostMapping("/applications/{id}/withdraw")
    public String withdraw(@PathVariable("id") Long id, Authentication auth, RedirectAttributes redirect) {
        StudentProfile profile = currentProfile(auth);
        try {
            applicationService.withdraw(profile, id);
            redirect.addFlashAttribute("message", "Application withdrawn.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/applications";
    }

    private StudentProfile currentProfile(Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        return studentProfileRepository.findByUser(user).orElse(null);
    }
}