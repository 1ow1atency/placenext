package com.placenext.placenext.service;

import com.placenext.placenext.model.Application;
import com.placenext.placenext.model.ApplicationStatus;
import com.placenext.placenext.model.Round;
import com.placenext.placenext.model.RoundOutcome;
import com.placenext.placenext.model.RoundResult;
import com.placenext.placenext.repository.ApplicationRepository;
import com.placenext.placenext.repository.RoundResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RoundResultService {

    private final RoundResultRepository roundResultRepository;
    private final ApplicationRepository applicationRepository;
    private final EmailService emailService;

    public RoundResultService(RoundResultRepository roundResultRepository,
                              ApplicationRepository applicationRepository,
                              EmailService emailService) {
        this.roundResultRepository = roundResultRepository;
        this.applicationRepository = applicationRepository;
        this.emailService = emailService;
    }

    /** appId -> outcome, for one round (used on the recruiter results page). */
    public Map<Long, RoundOutcome> forRound(Round round) {
        Map<Long, RoundOutcome> map = new HashMap<>();
        for (RoundResult r : roundResultRepository.findByRound(round)) {
            map.put(r.getApplication().getId(), r.getOutcome());
        }
        return map;
    }

    /** appId -> (roundId -> outcome), for the student's applications page. */
    public Map<Long, Map<Long, RoundOutcome>> forApplications(List<Application> applications) {
        Map<Long, Map<Long, RoundOutcome>> map = new HashMap<>();
        for (Application a : applications) {
            Map<Long, RoundOutcome> inner = new HashMap<>();
            for (RoundResult r : roundResultRepository.findByApplication(a)) {
                inner.put(r.getRound().getId(), r.getOutcome());
            }
            map.put(a.getId(), inner);
        }
        return map;
    }

    @Transactional
    public void record(Round round, Long applicationId, RoundOutcome outcome) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found."));

        if (!application.getDrive().getId().equals(round.getDrive().getId())) {
            throw new IllegalArgumentException("This application does not belong to this drive.");
        }
        if (application.getStatus() != ApplicationStatus.SHORTLISTED) {
            throw new IllegalStateException("Results can only be recorded for shortlisted applicants.");
        }

        RoundResult result = roundResultRepository.findByRoundAndApplication(round, application)
                .orElseGet(() -> {
                    RoundResult r = new RoundResult();
                    r.setRound(round);
                    r.setApplication(application);
                    return r;
                });

        // Only email when the outcome actually changes (clicking "Passed" twice sends one email)
        boolean changed = result.getOutcome() != outcome;

        LocalDateTime now = LocalDateTime.now();
        result.setOutcome(outcome);
        result.setUpdatedAt(now);
        roundResultRepository.save(result);

        if (outcome == RoundOutcome.FAILED) {
            application.setStatus(ApplicationStatus.REJECTED);
            application.setUpdatedAt(now);
            applicationRepository.save(application);
        }

        if (changed) {
            notifyStudent(application, outcome);
        }
    }

    @Transactional
    public void deleteForRound(Round round) {
        roundResultRepository.deleteByRound(round);
    }

    /** Builds the email text here (inside the DB session) and hands plain Strings to EmailService. */
    private void notifyStudent(Application app, RoundOutcome outcome) {
        String to = app.getStudent().getUser().getEmail();
        String name = app.getStudent().getUser().getFullName();
        String company = app.getDrive().getCompanyName();

        if (outcome == RoundOutcome.PASSED) {
            emailService.send(to,
                    "You cleared a round at " + company,
                    "Hi " + name + ",\n\n"
                            + "Well done! You have passed a selection round for the " + company + " drive.\n"
                            + "Log in to PlaceNext and open My Applications to see your progress and the next round.");
        } else {
            emailService.send(to,
                    "Update on your " + company + " application",
                    "Hi " + name + ",\n\n"
                            + "Thank you for taking part in the " + company + " selection process. "
                            + "Unfortunately, you did not clear this round, so your application has been closed.\n"
                            + "Keep an eye on PlaceNext for other drives you are eligible for. All the best!");
        }
    }
}