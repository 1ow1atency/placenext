package com.placenext.placenext.repository;

import com.placenext.placenext.model.Application;
import com.placenext.placenext.model.ApplicationStatus;
import com.placenext.placenext.model.Drive;
import com.placenext.placenext.model.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Optional<Application> findByStudentAndDrive(StudentProfile student, Drive drive);

    List<Application> findByStudentOrderByAppliedAtDesc(StudentProfile student);

    List<Application> findByDriveOrderByAppliedAtDesc(Drive drive);

    List<Application> findByDriveAndStatusNotOrderByAppliedAtDesc(Drive drive, ApplicationStatus status);

    long countByStudent(StudentProfile student);

    long countByStudentAndStatus(StudentProfile student, ApplicationStatus status);

    long countByDrive(Drive drive);

    long countByDriveAndStatusNot(Drive drive, ApplicationStatus status);
}