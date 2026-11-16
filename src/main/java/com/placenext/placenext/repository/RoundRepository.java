package com.placenext.placenext.repository;

import com.placenext.placenext.model.Drive;
import com.placenext.placenext.model.Round;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoundRepository extends JpaRepository<Round, Long> {

    List<Round> findByDriveOrderByRoundNoAsc(Drive drive);

    long countByDrive(Drive drive);
}