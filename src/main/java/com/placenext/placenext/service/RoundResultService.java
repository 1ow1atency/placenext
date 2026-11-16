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

    public RoundResultService(RoundResultRepository roundResultRepository,
                              ApplicationRepository applicationRepository) {
        this.roundResultRepository = roundResultRepository;
        this.applicationRepository = applicationRepository;
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
        result.setOutcome(outcome);
        result.setUpdatedAt(LocalDateTime.now());
        roundResultRepository.save(result);

        if (outcome == RoundOutcome.FAILED) {
            application.setStatus(ApplicationStatus.REJECTED);
            applicationRepository.save(application);
        }
    }

    @Transactional
    public void deleteForRound(Round round) {
        roundResultRepository.deleteByRound(round);
    }
}