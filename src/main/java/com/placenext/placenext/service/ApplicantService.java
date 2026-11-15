package com.placenext.placenext.service;

import com.placenext.placenext.model.Application;
import com.placenext.placenext.model.ApplicationStatus;
import com.placenext.placenext.model.Drive;
import com.placenext.placenext.model.StudentProfile;
import com.placenext.placenext.repository.ApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ApplicantService {

    private final ApplicationRepository applicationRepository;

    public ApplicantService(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    public List<Application> applicants(Drive drive, ApplicationStatus status, Double minCgpa) {
        return applicationRepository
                .findByDriveAndStatusNotOrderByAppliedAtDesc(drive, ApplicationStatus.WITHDRAWN)
                .stream()
                .filter(a -> status == null || a.getStatus() == status)
                .filter(a -> minCgpa == null || a.getStudent().getCgpa() >= minCgpa)
                .toList();
    }

    public long countApplicants(Drive drive) {
        return applicationRepository.countByDriveAndStatusNot(drive, ApplicationStatus.WITHDRAWN);
    }

    public Optional<Application> findById(Long id) {
        return applicationRepository.findById(id);
    }

    /** Moves an application forward. Only valid transitions are allowed. */
    @Transactional
    public void changeStatus(Application app, ApplicationStatus target) {
        ApplicationStatus current = app.getStatus();
        boolean allowed =
                (current == ApplicationStatus.APPLIED
                        && (target == ApplicationStatus.SHORTLISTED || target == ApplicationStatus.REJECTED))
                        || (current == ApplicationStatus.SHORTLISTED
                        && (target == ApplicationStatus.SELECTED || target == ApplicationStatus.REJECTED));
        if (!allowed) {
            throw new IllegalArgumentException(
                    "Can't move an application from " + current.getLabel() + " to " + target.getLabel() + ".");
        }
        app.setStatus(target);
        app.setUpdatedAt(LocalDateTime.now());
        applicationRepository.save(app);
    }

    public String toCsv(List<Application> applications) {
        StringBuilder sb = new StringBuilder();
        sb.append("Name,Email,Roll No,Branch,Batch,CGPA,Backlogs,Phone,Status,Applied On\n");
        for (Application a : applications) {
            StudentProfile s = a.getStudent();
            sb.append(csv(s.getUser().getFullName())).append(',')
                    .append(csv(s.getUser().getEmail())).append(',')
                    .append(csv(s.getRollNo())).append(',')
                    .append(csv(s.getBranch().name())).append(',')
                    .append(s.getBatch()).append(',')
                    .append(s.getCgpa()).append(',')
                    .append(s.getActiveBacklogs()).append(',')
                    .append(csv(s.getPhone())).append(',')
                    .append(csv(a.getStatus().getLabel())).append(',')
                    .append(a.getAppliedAt().toLocalDate()).append('\n');
        }
        return sb.toString();
    }

    private String csv(String value) {
        if (value == null) return "";
        String v = value;
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }
}