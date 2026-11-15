package com.placenext.placenext.repository;

import com.placenext.placenext.model.Drive;
import com.placenext.placenext.model.DriveStatus;
import com.placenext.placenext.model.RecruiterProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DriveRepository extends JpaRepository<Drive, Long> {

    List<Drive> findByRecruiterOrderByCreatedAtDesc(RecruiterProfile recruiter);

    List<Drive> findByStatusOrderByCreatedAtAsc(DriveStatus status);

    List<Drive> findAllByOrderByCreatedAtDesc();

    List<Drive> findByStatusAndDeadlineGreaterThanEqualOrderByDeadlineAsc(DriveStatus status, LocalDate date);

    long countByStatus(DriveStatus status);
}