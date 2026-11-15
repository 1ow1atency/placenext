package com.placenext.placenext.controller;

import com.placenext.placenext.dto.StudentProfileForm;
import com.placenext.placenext.model.Branch;
import com.placenext.placenext.model.StudentProfile;
import com.placenext.placenext.model.User;
import com.placenext.placenext.repository.UserRepository;
import com.placenext.placenext.service.FileStorageService;
import com.placenext.placenext.service.StudentProfileService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Optional;

@Controller
@RequestMapping("/student")
public class StudentProfileController {

    private final UserRepository userRepository;
    private final StudentProfileService profileService;
    private final FileStorageService fileStorageService;

    public StudentProfileController(UserRepository userRepository,
                                    StudentProfileService profileService,
                                    FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.profileService = profileService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/profile")
    public String profile(Authentication auth, Model model) {
        User user = currentUser(auth);
        Optional<StudentProfile> profile = profileService.findByUser(user);
        model.addAttribute("form", profile.map(StudentProfileForm::from).orElseGet(StudentProfileForm::new));
        addCommon(model, user, profile.orElse(null));
        return "student/profile";
    }

    @PostMapping("/profile")
    public String saveProfile(Authentication auth,
                              @Valid @ModelAttribute("form") StudentProfileForm form,
                              BindingResult result,
                              @RequestParam(value = "resume", required = false) MultipartFile resume,
                              Model model,
                              RedirectAttributes redirect) {
        User user = currentUser(auth);
        StudentProfile existing = profileService.findByUser(user).orElse(null);

        if (!result.hasFieldErrors("rollNo") && profileService.rollNoTaken(form.getRollNo(), user)) {
            result.rejectValue("rollNo", "duplicate", "This roll number is already registered");
        }
        if (result.hasErrors()) {
            addCommon(model, user, existing);
            return "student/profile";
        }

        try {
            profileService.save(user, form, resume);
        } catch (IllegalArgumentException e) {
            model.addAttribute("resumeError", e.getMessage());
            addCommon(model, user, existing);
            return "student/profile";
        } catch (IOException e) {
            model.addAttribute("resumeError", "Couldn't save the file. Please try again.");
            addCommon(model, user, existing);
            return "student/profile";
        }

        redirect.addFlashAttribute("saved", true);
        return "redirect:/student/profile";
    }

    @GetMapping("/resume")
    public ResponseEntity<Resource> viewResume(Authentication auth) throws MalformedURLException {
        User user = currentUser(auth);
        Optional<StudentProfile> profile = profileService.findByUser(user)
                .filter(p -> p.getResumeFileName() != null);
        if (profile.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Resource file = fileStorageService.loadResume(profile.get().getResumeFileName());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"resume.pdf\"")
                .body(file);
    }

    private void addCommon(Model model, User user, StudentProfile profile) {
        model.addAttribute("user", user);
        model.addAttribute("profile", profile);
        model.addAttribute("branches", Branch.values());
    }

    private User currentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName()).orElseThrow();
    }
}