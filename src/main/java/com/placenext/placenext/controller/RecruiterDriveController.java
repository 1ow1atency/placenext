package com.placenext.placenext.controller;

import com.placenext.placenext.dto.DriveForm;
import com.placenext.placenext.model.Branch;
import com.placenext.placenext.model.DriveType;
import com.placenext.placenext.model.RecruiterProfile;
import com.placenext.placenext.model.User;
import com.placenext.placenext.repository.RecruiterProfileRepository;
import com.placenext.placenext.repository.UserRepository;
import com.placenext.placenext.service.DriveService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/recruiter/drives")
public class RecruiterDriveController {

    private final UserRepository userRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final DriveService driveService;

    public RecruiterDriveController(UserRepository userRepository,
                                    RecruiterProfileRepository recruiterProfileRepository,
                                    DriveService driveService) {
        this.userRepository = userRepository;
        this.recruiterProfileRepository = recruiterProfileRepository;
        this.driveService = driveService;
    }

    @GetMapping
    public String list(Authentication auth, Model model) {
        RecruiterProfile recruiter = currentRecruiter(auth);
        model.addAttribute("recruiter", recruiter);
        model.addAttribute("drives", driveService.forRecruiter(recruiter));
        return "recruiter/drives";
    }

    @GetMapping("/new")
    public String newDrive(Authentication auth, Model model) {
        DriveForm form = new DriveForm();
        form.setEligibleBatch(LocalDate.now().getYear() + 1);
        model.addAttribute("form", form);
        addFormOptions(model, currentRecruiter(auth));
        return "recruiter/drive-form";
    }

    @PostMapping
    public String create(Authentication auth,
                         @Valid @ModelAttribute("form") DriveForm form,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirect) {
        RecruiterProfile recruiter = currentRecruiter(auth);

        // The pay field required depends on the drive type
        if (form.getType() == DriveType.INTERNSHIP && form.getStipendPerMonth() == null) {
            result.rejectValue("stipendPerMonth", "required", "Enter the monthly stipend");
        }
        if (form.getType() == DriveType.FULL_TIME && form.getCtcLpa() == null) {
            result.rejectValue("ctcLpa", "required", "Enter the package in LPA");
        }

        if (result.hasErrors()) {
            addFormOptions(model, recruiter);
            return "recruiter/drive-form";
        }

        driveService.create(recruiter, form);
        redirect.addFlashAttribute("created", form.getTitle().trim());
        return "redirect:/recruiter/drives";
    }

    private void addFormOptions(Model model, RecruiterProfile recruiter) {
        model.addAttribute("recruiter", recruiter);
        model.addAttribute("branches", Branch.values());
        model.addAttribute("types", DriveType.values());
    }

    private RecruiterProfile currentRecruiter(Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        return recruiterProfileRepository.findByUser(user).orElseThrow();
    }
}