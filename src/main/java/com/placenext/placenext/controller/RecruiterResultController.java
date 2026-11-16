package com.placenext.placenext.controller;

import com.placenext.placenext.model.Application;
import com.placenext.placenext.model.ApplicationStatus;
import com.placenext.placenext.model.Drive;
import com.placenext.placenext.model.DriveStatus;
import com.placenext.placenext.model.RecruiterProfile;
import com.placenext.placenext.model.Round;
import com.placenext.placenext.model.RoundOutcome;
import com.placenext.placenext.model.User;
import com.placenext.placenext.repository.ApplicationRepository;
import com.placenext.placenext.repository.RecruiterProfileRepository;
import com.placenext.placenext.repository.RoundRepository;
import com.placenext.placenext.repository.UserRepository;
import com.placenext.placenext.service.DriveService;
import com.placenext.placenext.service.RoundResultService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/recruiter/drives/{driveId}/rounds/{roundId}/results")
public class RecruiterResultController {

    private final UserRepository userRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final DriveService driveService;
    private final RoundRepository roundRepository;
    private final ApplicationRepository applicationRepository;
    private final RoundResultService roundResultService;

    public RecruiterResultController(UserRepository userRepository,
                                     RecruiterProfileRepository recruiterProfileRepository,
                                     DriveService driveService,
                                     RoundRepository roundRepository,
                                     ApplicationRepository applicationRepository,
                                     RoundResultService roundResultService) {
        this.userRepository = userRepository;
        this.recruiterProfileRepository = recruiterProfileRepository;
        this.driveService = driveService;
        this.roundRepository = roundRepository;
        this.applicationRepository = applicationRepository;
        this.roundResultService = roundResultService;
    }

    @GetMapping
    public String view(@PathVariable("driveId") Long driveId,
                       @PathVariable("roundId") Long roundId,
                       Authentication auth, Model model) {
        Drive drive = ownedDrive(driveId, auth);
        if (drive == null) return "redirect:/recruiter/drives";
        Round round = roundOf(drive, roundId);
        if (round == null) return "redirect:/recruiter/drives/" + driveId + "/rounds";

        Map<Long, RoundOutcome> results = roundResultService.forRound(round);

        // Shortlisted/selected applicants, plus anyone who already has a result in this round.
        List<Application> applicants = applicationRepository
                .findByDriveAndStatusNotOrderByAppliedAtDesc(drive, ApplicationStatus.WITHDRAWN)
                .stream()
                .filter(a -> a.getStatus() == ApplicationStatus.SHORTLISTED
                        || a.getStatus() == ApplicationStatus.SELECTED
                        || results.containsKey(a.getId()))
                .toList();

        model.addAttribute("drive", drive);
        model.addAttribute("round", round);
        model.addAttribute("applicants", applicants);
        model.addAttribute("results", results);
        return "recruiter/round-results";
    }

    @PostMapping("/{applicationId}")
    public String record(@PathVariable("driveId") Long driveId,
                         @PathVariable("roundId") Long roundId,
                         @PathVariable("applicationId") Long applicationId,
                         @RequestParam("outcome") RoundOutcome outcome,
                         Authentication auth,
                         RedirectAttributes redirect) {
        Drive drive = ownedDrive(driveId, auth);
        if (drive == null) return "redirect:/recruiter/drives";
        Round round = roundOf(drive, roundId);
        if (round == null) return "redirect:/recruiter/drives/" + driveId + "/rounds";

        try {
            roundResultService.record(round, applicationId, outcome);
            redirect.addFlashAttribute("message",
                    outcome == RoundOutcome.FAILED
                            ? "Marked as failed. The application was rejected."
                            : "Marked as passed.");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/recruiter/drives/" + driveId + "/rounds/" + roundId + "/results";
    }

    private Round roundOf(Drive drive, Long roundId) {
        Round round = roundRepository.findById(roundId).orElse(null);
        if (round == null || !round.getDrive().getId().equals(drive.getId())) {
            return null;
        }
        return round;
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