package com.placenext.placenext.controller;

import com.placenext.placenext.model.*;
import com.placenext.placenext.repository.RecruiterProfileRepository;
import com.placenext.placenext.repository.UserRepository;
import com.placenext.placenext.service.ApplicantService;
import com.placenext.placenext.service.DriveService;
import com.placenext.placenext.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/recruiter/drives/{driveId}")
public class RecruiterApplicantController {

    private final UserRepository userRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final DriveService driveService;
    private final ApplicantService applicantService;
    private final FileStorageService fileStorageService;

    public RecruiterApplicantController(UserRepository userRepository,
                                        RecruiterProfileRepository recruiterProfileRepository,
                                        DriveService driveService,
                                        ApplicantService applicantService,
                                        FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.recruiterProfileRepository = recruiterProfileRepository;
        this.driveService = driveService;
        this.applicantService = applicantService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/applicants")
    public String applicants(@PathVariable("driveId") Long driveId,
                             @RequestParam(value = "status", required = false) String status,
                             @RequestParam(value = "minCgpa", required = false) Double minCgpa,
                             Authentication auth, Model model) {
        Drive drive = ownedDrive(driveId, auth);
        if (drive == null) return "redirect:/recruiter/drives";

        ApplicationStatus statusFilter = parseStatus(status);
        List<Application> list = applicantService.applicants(drive, statusFilter, minCgpa);

        model.addAttribute("drive", drive);
        model.addAttribute("applications", list);
        model.addAttribute("totalApplicants", applicantService.countApplicants(drive));
        model.addAttribute("statusFilter", statusFilter == null ? "" : statusFilter.name());
        model.addAttribute("minCgpaFilter", minCgpa);
        model.addAttribute("statuses", new ApplicationStatus[]{
                ApplicationStatus.APPLIED, ApplicationStatus.SHORTLISTED,
                ApplicationStatus.REJECTED, ApplicationStatus.SELECTED});
        return "recruiter/applicants";
    }

    @GetMapping("/applicants/export")
    public ResponseEntity<byte[]> export(@PathVariable("driveId") Long driveId,
                                         @RequestParam(value = "status", required = false) String status,
                                         @RequestParam(value = "minCgpa", required = false) Double minCgpa,
                                         Authentication auth) {
        Drive drive = ownedDrive(driveId, auth);
        if (drive == null) return ResponseEntity.notFound().build();

        String csv = applicantService.toCsv(applicantService.applicants(drive, parseStatus(status), minCgpa));
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"applicants-drive-" + driveId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    @GetMapping("/applicants/{applicationId}/resume")
    public ResponseEntity<Resource> resume(@PathVariable("driveId") Long driveId,
                                           @PathVariable("applicationId") Long applicationId,
                                           Authentication auth) throws MalformedURLException {
        Drive drive = ownedDrive(driveId, auth);
        if (drive == null) return ResponseEntity.notFound().build();

        Application app = applicantService.findById(applicationId).orElse(null);
        if (app == null || !app.getDrive().getId().equals(drive.getId())
                || app.getStudent().getResumeFileName() == null) {
            return ResponseEntity.notFound().build();
        }
        Resource file = fileStorageService.loadResume(app.getStudent().getResumeFileName());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"resume.pdf\"")
                .body(file);
    }

    /** Returns the drive only if it belongs to the logged-in recruiter. */
    private Drive ownedDrive(Long driveId, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        RecruiterProfile recruiter = recruiterProfileRepository.findByUser(user).orElse(null);
        Drive drive = driveService.findById(driveId).orElse(null);
        if (recruiter == null || drive == null
                || !drive.getRecruiter().getId().equals(recruiter.getId())) {
            return null;
        }
        return drive;
    }

    private ApplicationStatus parseStatus(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return ApplicationStatus.valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}