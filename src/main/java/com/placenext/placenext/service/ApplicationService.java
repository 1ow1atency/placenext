package com.placenext.placenext.service;

import com.placenext.placenext.model.*;
import com.placenext.placenext.repository.ApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final EligibilityService eligibilityService;

    public ApplicationService(ApplicationRepository applicationRepository,
                              EligibilityService eligibilityService) {
        this.applicationRepository = applicationRepository;
        this.eligibilityService = eligibilityService;
    }

    /** Applies to a drive. Throws IllegalArgumentException with a friendly message if not allowed. */
    @Transactional
    public Application apply(StudentProfile student, Drive drive) {
        if (drive.getStatus() != DriveStatus.APPROVED) {
            throw new IllegalArgumentException("This drive is not open for applications.");
        }
        if (drive.isDeadlinePassed()) {
            throw new IllegalArgumentException("The deadline for this drive has passed.");
        }
        List<String> reasons = eligibilityService.check(student, drive);
        if (!reasons.isEmpty()) {
            throw new IllegalArgumentException("You are not eligible: " + reasons.get(0));
        }

        Optional<Application> existing = applicationRepository.findByStudentAndDrive(student, drive);
        if (existing.isPresent()) {
            Application app = existing.get();
            if (app.getStatus() == ApplicationStatus.WITHDRAWN) {
                // Allow re-applying after withdrawing
                app.setStatus(ApplicationStatus.APPLIED);
                app.setAppliedAt(LocalDateTime.now());
                app.setUpdatedAt(LocalDateTime.now());
                return app;
            }
            throw new IllegalArgumentException("You have already applied to this drive.");
        }

        Application app = new Application();
        app.setStudent(student);
        app.setDrive(drive);
        return applicationRepository.save(app);
    }

    /** A student can withdraw only while the application is still just "Applied". */
    @Transactional
    public void withdraw(StudentProfile student, Long applicationId) {
        Application app = applicationRepository.findById(applicationId).orElseThrow();
        if (!app.getStudent().getId().equals(student.getId())) {
            throw new IllegalArgumentException("That application isn't yours.");
        }
        if (app.getStatus() != ApplicationStatus.APPLIED) {
            throw new IllegalArgumentException("You can only withdraw an application that hasn't been reviewed yet.");
        }
        app.setStatus(ApplicationStatus.WITHDRAWN);
        app.setUpdatedAt(LocalDateTime.now());
    }

    public Optional<Application> find(StudentProfile student, Drive drive) {
        return applicationRepository.findByStudentAndDrive(student, drive);
    }

    public List<Application> forStudent(StudentProfile student) {
        return applicationRepository.findByStudentOrderByAppliedAtDesc(student);
    }

    public long countForStudent(StudentProfile student) {
        return applicationRepository.countByStudent(student);
    }

    public long countForStudent(StudentProfile student, ApplicationStatus status) {
        return applicationRepository.countByStudentAndStatus(student, status);
    }
}