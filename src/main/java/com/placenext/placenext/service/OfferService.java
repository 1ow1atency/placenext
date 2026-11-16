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

/**
 * Placement policy:
 *  - A selected student has one pending offer per drive they were selected in.
 *  - Accepting an offer places the student. Their other pending offers are
 *    declined and their other open applications are withdrawn.
 *  - A placed student cannot accept a second offer or apply to new drives.
 *
 * Emails: the recruiter is told when their offer is accepted or declined
 * (including offers auto-declined because the student accepted elsewhere),
 * and the student gets a confirmation when they accept.
 */
@Service
public class OfferService {

    private final ApplicationRepository applicationRepository;
    private final EmailService emailService;

    public OfferService(ApplicationRepository applicationRepository,
                        EmailService emailService) {
        this.applicationRepository = applicationRepository;
        this.emailService = emailService;
    }

    public boolean isPlaced(StudentProfile student) {
        return applicationRepository.existsByStudentAndOfferStatus(student, OfferStatus.ACCEPTED);
    }

    @Transactional
    public void accept(StudentProfile student, Long applicationId) {
        Application offer = ownedPendingOffer(student, applicationId);

        if (isPlaced(student)) {
            throw new IllegalStateException("You have already accepted an offer.");
        }

        LocalDateTime now = LocalDateTime.now();
        offer.setOfferStatus(OfferStatus.ACCEPTED);
        offer.setRespondedAt(now);
        offer.setUpdatedAt(now);
        applicationRepository.save(offer);

        notifyRecruiterAccepted(offer);
        notifyStudentAccepted(offer);

        List<Application> others = applicationRepository.findByStudentOrderByAppliedAtDesc(student);
        for (Application other : others) {
            if (other.getId().equals(offer.getId())) {
                continue;
            }
            if (other.isOfferPending()) {
                other.setOfferStatus(OfferStatus.DECLINED);
                other.setRespondedAt(now);
                other.setUpdatedAt(now);
                applicationRepository.save(other);
                notifyRecruiterDeclined(other, true);
            } else if (other.getStatus() == ApplicationStatus.APPLIED
                    || other.getStatus() == ApplicationStatus.SHORTLISTED) {
                other.setStatus(ApplicationStatus.WITHDRAWN);
                other.setUpdatedAt(now);
                applicationRepository.save(other);
            }
        }
    }

    @Transactional
    public void decline(StudentProfile student, Long applicationId) {
        Application offer = ownedPendingOffer(student, applicationId);

        LocalDateTime now = LocalDateTime.now();
        offer.setOfferStatus(OfferStatus.DECLINED);
        offer.setRespondedAt(now);
        offer.setUpdatedAt(now);
        applicationRepository.save(offer);

        notifyRecruiterDeclined(offer, false);
    }

    private Application ownedPendingOffer(StudentProfile student, Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found."));

        if (!application.getStudent().getId().equals(student.getId())) {
            throw new IllegalArgumentException("Offer not found.");
        }
        if (!application.isOfferPending()) {
            throw new IllegalStateException("This offer has already been answered.");
        }
        return application;
    }

    // ---------- Emails (text built here, inside the DB session) ----------

    private void notifyRecruiterAccepted(Application app) {
        Drive d = app.getDrive();
        String student = app.getStudent().getUser().getFullName();
        emailService.send(d.getRecruiter().getUser().getEmail(),
                student + " accepted your offer",
                "Hi " + d.getRecruiter().getUser().getFullName() + ",\n\n"
                        + student + " (Roll No " + app.getStudent().getRollNo() + ", "
                        + app.getStudent().getBranch().name() + ") has accepted your offer for \""
                        + d.getTitle() + "\".\n\n"
                        + "The placement office will be in touch about the next steps.");
    }

    private void notifyRecruiterDeclined(Application app, boolean acceptedElsewhere) {
        Drive d = app.getDrive();
        String student = app.getStudent().getUser().getFullName();
        String why = acceptedElsewhere
                ? " has accepted another offer, so your offer for \"" + d.getTitle() + "\" was automatically declined."
                : " has declined your offer for \"" + d.getTitle() + "\".";
        emailService.send(d.getRecruiter().getUser().getEmail(),
                student + " declined your offer",
                "Hi " + d.getRecruiter().getUser().getFullName() + ",\n\n"
                        + student + " (Roll No " + app.getStudent().getRollNo() + ")" + why);
    }

    private void notifyStudentAccepted(Application app) {
        Drive d = app.getDrive();
        emailService.send(app.getStudent().getUser().getEmail(),
                "You accepted the offer from " + d.getCompanyName(),
                "Hi " + app.getStudent().getUser().getFullName() + ",\n\n"
                        + "This confirms that you accepted the offer from " + d.getCompanyName()
                        + " for \"" + d.getTitle() + "\".\n"
                        + "Package: " + d.getCompensation() + "\n\n"
                        + "Congratulations on getting placed! Your other pending offers were declined "
                        + "and your open applications were withdrawn, as per the placement policy.");
    }
}