package com.placenext.placenext.controller;

import com.placenext.placenext.dto.RoundForm;
import com.placenext.placenext.model.Drive;
import com.placenext.placenext.model.DriveStatus;
import com.placenext.placenext.model.RecruiterProfile;
import com.placenext.placenext.model.RoundType;
import com.placenext.placenext.model.User;
import com.placenext.placenext.repository.RecruiterProfileRepository;
import com.placenext.placenext.repository.UserRepository;
import com.placenext.placenext.service.DriveService;
import com.placenext.placenext.service.RoundService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/recruiter/drives/{driveId}/rounds")
public class RecruiterRoundController {

    private final UserRepository userRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final DriveService driveService;
    private final RoundService roundService;

    public RecruiterRoundController(UserRepository userRepository,
                                    RecruiterProfileRepository recruiterProfileRepository,
                                    DriveService driveService,
                                    RoundService roundService) {
        this.userRepository = userRepository;
        this.recruiterProfileRepository = recruiterProfileRepository;
        this.driveService = driveService;
        this.roundService = roundService;
    }

    @GetMapping
    public String list(@PathVariable("driveId") Long driveId, Authentication auth, Model model) {
        Drive drive = ownedDrive(driveId, auth);
        if (drive == null) return "redirect:/recruiter/drives";
        addCommon(model, drive);
        model.addAttribute("form", new RoundForm());
        return "recruiter/rounds";
    }

    @PostMapping
    public String create(@PathVariable("driveId") Long driveId,
                         @Valid @ModelAttribute("form") RoundForm form,
                         BindingResult result,
                         Authentication auth,
                         Model model,
                         RedirectAttributes redirect) {
        Drive drive = ownedDrive(driveId, auth);
        if (drive == null) return "redirect:/recruiter/drives";

        if (!result.hasErrors()) {
            try {
                roundService.create(drive, form);
                redirect.addFlashAttribute("message", "Round scheduled.");
                return "redirect:/recruiter/drives/" + driveId + "/rounds";
            } catch (IllegalArgumentException e) {
                result.rejectValue("scheduledAt", "invalid", e.getMessage());
            }
        }
        addCommon(model, drive);
        return "recruiter/rounds";
    }

    @PostMapping("/{roundId}/delete")
    public String delete(@PathVariable("driveId") Long driveId,
                         @PathVariable("roundId") Long roundId,
                         Authentication auth,
                         RedirectAttributes redirect) {
        Drive drive = ownedDrive(driveId, auth);
        if (drive == null) return "redirect:/recruiter/drives";
        try {
            roundService.delete(drive, roundId);
            redirect.addFlashAttribute("message", "Round deleted.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/recruiter/drives/" + driveId + "/rounds";
    }

    private void addCommon(Model model, Drive drive) {
        model.addAttribute("drive", drive);
        model.addAttribute("rounds", roundService.forDrive(drive));
        model.addAttribute("types", RoundType.values());
    }

    /** Returns the drive only if it is approved and belongs to the logged-in recruiter. */
    private Drive ownedDrive(Long driveId, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        RecruiterProfile recruiter = recruiterProfileRepository.findByUser(user).orElse(null);
        Drive drive = driveService.findById(driveId).orElse(null);
        if (recruiter == null || drive == null
                || !drive.getRecruiter().getId().equals(recruiter.getId())
                || drive.getStatus() != DriveStatus.APPROVED) {
            return null;
        }
        return drive;
    }
}