package com.placenext.placenext.repository;

import com.placenext.placenext.model.RecruiterProfile;
import com.placenext.placenext.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecruiterProfileRepository extends JpaRepository<RecruiterProfile, Long> {
    Optional<RecruiterProfile> findByUser(User user);
    List<RecruiterProfile> findAllByOrderByIdDesc();
}
