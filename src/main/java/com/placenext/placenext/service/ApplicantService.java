package com.placenext.placenext.service;

import com.placenext.placenext.model.Application;
import com.placenext.placenext.model.ApplicationStatus;
import com.placenext.placenext.model.Drive;
import com.placenext.placenext.model.OfferStatus;
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
    private final EmailService emailService;

    public ApplicantService(ApplicationRepository applicationRepository,
                            EmailService emailService) {
        this.applicationRepository = applicationRepository;
        this.emailService = emailService;
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

        LocalDateTime now = LocalDateTime.now();

        if (target == ApplicationStatus.SELECTED) {
            if (applicationRepository.existsByStudentAndOfferStatus(app.getStudent(), OfferStatus.ACCEPTED)) {
                throw new IllegalArgumentException(
                        "This student has already accepted another offer and is placed.");
            }
            app.setOfferStatus(OfferStatus.PENDING);
            app.setOfferAt(now);
        }

        app.setStatus(target);
        app.setUpdatedAt(now);
        applicationRepository.save(app);

        notifyStudent(app, target);
    }

    /** Builds the email text here (inside the DB session) and hands plain Strings to EmailService. */
    private void notifyStudent(Application app, ApplicationStatus target) {
        StudentProfile s = app.getStudent();
        Drive d = app.getDrive();
        String to = s.getUser().getEmail();
        String name = s.getUser().getFullName();
        String company = d.getCompanyName();

        String subject;
        String body;
        switch (target) {
            case SHORTLISTED -> {
                subject = "You've been shortlisted by " + company;
                body = "Hi " + name + ",\n\n"
                        + "Good news! You have been shortlisted for the " + company + " drive.\n"
                        + "Log in to PlaceNext and open My Applications to see the upcoming rounds.";
            }
            case SELECTED -> {
                subject = "Offer from " + company + "!";
                body = "Hi " + name + ",\n\n"
                        + "Congratulations! " + company + " has selected you.\n"
                        + "Package: " + d.getCompensation() + "\n\n"
                        + "Please log in to PlaceNext and Accept or Decline the offer from My Applications.\n"
                        + "Note: accepting an offer marks you as placed.";
            }
            case REJECTED -> {
                subject = "Update on your " + company + " application";
                body = "Hi " + name + ",\n\n"
                        + "Thank you for applying to " + company + ". Unfortunately, you have not been "
                        + "selected to move forward this time.\n"
                        + "Keep an eye on PlaceNext for other drives you are eligible for. All the best!";
            }
            default -> {
                return;
            }
        }
        emailService.send(to, subject, body);
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