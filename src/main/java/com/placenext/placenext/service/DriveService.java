package com.placenext.placenext.service;

import com.placenext.placenext.dto.DriveForm;
import com.placenext.placenext.model.Drive;
import com.placenext.placenext.model.DriveStatus;
import com.placenext.placenext.model.DriveType;
import com.placenext.placenext.model.RecruiterProfile;
import com.placenext.placenext.repository.DriveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
public class DriveService {

    private final DriveRepository driveRepository;
    private final EmailService emailService;

    public DriveService(DriveRepository driveRepository,
                        EmailService emailService) {
        this.driveRepository = driveRepository;
        this.emailService = emailService;
    }

    // ---------- Recruiter ----------
    @Transactional
    public Drive create(RecruiterProfile recruiter, DriveForm form) {
        Drive drive = new Drive();
        drive.setRecruiter(recruiter);
        drive.setTitle(form.getTitle().trim());
        drive.setType(form.getType());
        drive.setDescription(blankToNull(form.getDescription()));
        drive.setLocation(blankToNull(form.getLocation()));

        // Keep only the pay field that matches the drive type
        if (form.getType() == DriveType.INTERNSHIP) {
            drive.setStipendPerMonth(form.getStipendPerMonth());
            drive.setCtcLpa(null);
        } else {
            drive.setCtcLpa(form.getCtcLpa());
            drive.setStipendPerMonth(null);
        }

        drive.setDeadline(form.getDeadline());
        drive.setMinCgpa(form.getMinCgpa());
        drive.setMaxBacklogs(form.getMaxBacklogs());
        drive.setEligibleBatch(form.getEligibleBatch());
        drive.setAllowedBranches(new HashSet<>(form.getAllowedBranches()));
        drive.setStatus(DriveStatus.PENDING);
        return driveRepository.save(drive);
    }

    public List<Drive> forRecruiter(RecruiterProfile recruiter) {
        return driveRepository.findByRecruiterOrderByCreatedAtDesc(recruiter);
    }

    // ---------- Admin ----------
    public List<Drive> pending() {
        return driveRepository.findByStatusOrderByCreatedAtAsc(DriveStatus.PENDING);
    }

    public List<Drive> all() {
        return driveRepository.findAllByOrderByCreatedAtDesc();
    }

    public long countAll() {
        return driveRepository.count();
    }

    public long countPending() {
        return driveRepository.countByStatus(DriveStatus.PENDING);
    }

    @Transactional
    public void approve(Long id) {
        Drive drive = driveRepository.findById(id).orElseThrow();
        boolean changed = drive.getStatus() != DriveStatus.APPROVED;
        drive.setStatus(DriveStatus.APPROVED);
        drive.setRejectionReason(null);

        if (changed) {
            emailService.send(recruiterEmail(drive),
                    "Your drive \"" + drive.getTitle() + "\" is approved",
                    "Hi " + recruiterName(drive) + ",\n\n"
                            + "Your drive \"" + drive.getTitle() + "\" has been approved by the placement office "
                            + "and is now visible to eligible students.\n"
                            + "Application deadline: " + drive.getDeadline() + "\n\n"
                            + "You can track applicants from your PlaceNext dashboard.");
        }
    }

    @Transactional
    public void reject(Long id, String reason) {
        Drive drive = driveRepository.findById(id).orElseThrow();
        boolean changed = drive.getStatus() != DriveStatus.REJECTED;
        String cleanReason = blankToNull(reason);
        drive.setStatus(DriveStatus.REJECTED);
        drive.setRejectionReason(cleanReason);

        if (changed) {
            emailService.send(recruiterEmail(drive),
                    "Your drive \"" + drive.getTitle() + "\" was not approved",
                    "Hi " + recruiterName(drive) + ",\n\n"
                            + "Your drive \"" + drive.getTitle() + "\" was not approved by the placement office.\n"
                            + "Reason: " + (cleanReason == null ? "No reason given." : cleanReason) + "\n\n"
                            + "You can post a corrected drive from your PlaceNext dashboard.");
        }
    }

    // ---------- Students ----------
    public List<Drive> openDrives() {
        return driveRepository.findByStatusAndDeadlineGreaterThanEqualOrderByDeadlineAsc(
                DriveStatus.APPROVED, LocalDate.now());
    }

    public Optional<Drive> findById(Long id) {
        return driveRepository.findById(id);
    }

    // ---------- Helpers ----------
    private String recruiterEmail(Drive drive) {
        return drive.getRecruiter().getUser().getEmail();
    }

    private String recruiterName(Drive drive) {
        return drive.getRecruiter().getUser().getFullName();
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}