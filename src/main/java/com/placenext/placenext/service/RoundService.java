package com.placenext.placenext.service;

import com.placenext.placenext.dto.RoundForm;
import com.placenext.placenext.model.Drive;
import com.placenext.placenext.model.Round;
import com.placenext.placenext.repository.RoundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RoundService {

    private final RoundRepository roundRepository;
    private final RoundResultService roundResultService;

    public RoundService(RoundRepository roundRepository, RoundResultService roundResultService) {
        this.roundRepository = roundRepository;
        this.roundResultService = roundResultService;
    }

    public List<Round> forDrive(Drive drive) {
        return roundRepository.findByDriveOrderByRoundNoAsc(drive);
    }

    public Optional<Round> findById(Long id) {
        return roundRepository.findById(id);
    }

    /** Throws IllegalArgumentException with a friendly message if the schedule isn't valid. */
    @Transactional
    public Round create(Drive drive, RoundForm form) {
        if (form.getScheduledAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("The round must be scheduled in the future.");
        }
        if (form.getScheduledAt().toLocalDate().isBefore(drive.getDeadline())) {
            throw new IllegalArgumentException("A round can't be scheduled before the application deadline ("
                    + drive.getDeadline() + ").");
        }

        Round round = new Round();
        round.setDrive(drive);
        round.setRoundNo((int) roundRepository.countByDrive(drive) + 1);
        round.setType(form.getType());
        round.setScheduledAt(form.getScheduledAt());
        round.setVenue(form.getVenue().trim());
        String instructions = form.getInstructions();
        round.setInstructions(instructions == null || instructions.isBlank() ? null : instructions.trim());
        return roundRepository.save(round);
    }

    /** Deletes a round (and its recorded results) and renumbers the rest. */
    @Transactional
    public void delete(Drive drive, Long roundId) {
        Round round = roundRepository.findById(roundId).orElseThrow();
        if (!round.getDrive().getId().equals(drive.getId())) {
            throw new IllegalArgumentException("That round doesn't belong to this drive.");
        }
        roundResultService.deleteForRound(round);
        roundRepository.delete(round);
        roundRepository.flush();

        int n = 1;
        for (Round r : roundRepository.findByDriveOrderByRoundNoAsc(drive)) {
            r.setRoundNo(n++);
        }
    }
}