package com.placenext.placenext.repository;

import com.placenext.placenext.model.StudentProfile;
import com.placenext.placenext.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {
    Optional<StudentProfile> findByUser(User user);
    boolean existsByRollNoAndUser_IdNot(String rollNo, Long userId);
}