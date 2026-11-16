package com.placenext.placenext.controller;

import com.placenext.placenext.model.StudentProfile;
import com.placenext.placenext.model.User;
import com.placenext.placenext.repository.StudentProfileRepository;
import com.placenext.placenext.repository.UserRepository;
import com.placenext.placenext.service.OfferService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/student/offers")
public class StudentOfferController {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final OfferService offerService;

    public StudentOfferController(UserRepository userRepository,
                                  StudentProfileRepository studentProfileRepository,
                                  OfferService offerService) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.offerService = offerService;
    }

    @PostMapping("/{applicationId}/accept")
    public String accept(@PathVariable("applicationId") Long applicationId,
                         Authentication auth, RedirectAttributes ra) {
        StudentProfile student = currentProfile(auth);
        if (student == null) {
            return "redirect:/student/applications";
        }
        try {
            offerService.accept(student, applicationId);
            ra.addFlashAttribute("success",
                    "Congratulations! You accepted the offer. Your other open applications were withdrawn.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/applications";
    }

    @PostMapping("/{applicationId}/decline")
    public String decline(@PathVariable("applicationId") Long applicationId,
                          Authentication auth, RedirectAttributes ra) {
        StudentProfile student = currentProfile(auth);
        if (student == null) {
            return "redirect:/student/applications";
        }
        try {
            offerService.decline(student, applicationId);
            ra.addFlashAttribute("success", "Offer declined.");
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