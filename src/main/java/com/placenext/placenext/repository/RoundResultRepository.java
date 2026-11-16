package com.placenext.placenext.repository;

import com.placenext.placenext.model.Application;
import com.placenext.placenext.model.Round;
import com.placenext.placenext.model.RoundResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoundResultRepository extends JpaRepository<RoundResult, Long> {

    Optional<RoundResult> findByRoundAndApplication(Round round, Application application);

    List<RoundResult> findByApplication(Application application);

    List<RoundResult> findByRound(Round round);

    void deleteByRound(Round round);
}