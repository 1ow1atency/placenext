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

    public DriveService(DriveRepository driveRepository) {
        this.driveRepository = driveRepository;
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
        drive.setStatus(DriveStatus.APPROVED);
        drive.setRejectionReason(null);
    }

    @Transactional
    public void reject(Long id, String reason) {
        Drive drive = driveRepository.findById(id).orElseThrow();
        drive.setStatus(DriveStatus.REJECTED);
        drive.setRejectionReason(blankToNull(reason));
    }

    // ---------- Students ----------
    public List<Drive> openDrives() {
        return driveRepository.findByStatusAndDeadlineGreaterThanEqualOrderByDeadlineAsc(
                DriveStatus.APPROVED, LocalDate.now());
    }

    public Optional<Drive> findById(Long id) {
        return driveRepository.findById(id);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}