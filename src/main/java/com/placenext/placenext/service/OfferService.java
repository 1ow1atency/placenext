package com.placenext.placenext.service;

import com.placenext.placenext.model.Application;
import com.placenext.placenext.model.ApplicationStatus;
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
 */
@Service
public class OfferService {

    private final ApplicationRepository applicationRepository;

    public OfferService(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
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
}