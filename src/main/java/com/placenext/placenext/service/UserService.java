package com.placenext.placenext.service;

import com.placenext.placenext.dto.CreateRecruiterForm;
import com.placenext.placenext.dto.RegisterForm;
import com.placenext.placenext.model.RecruiterProfile;
import com.placenext.placenext.model.Role;
import com.placenext.placenext.model.User;
import com.placenext.placenext.repository.RecruiterProfileRepository;
import com.placenext.placenext.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RecruiterProfileRepository recruiterProfileRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.recruiterProfileRepository = recruiterProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email.toLowerCase().trim());
    }

    public User registerStudent(RegisterForm form) {
        User user = new User();
        user.setFullName(form.getFullName().trim());
        user.setEmail(form.getEmail().toLowerCase().trim());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setRole(Role.STUDENT);
        return userRepository.save(user);
    }

    @Transactional
    public User createRecruiter(CreateRecruiterForm form) {
        User user = new User();
        user.setFullName(form.getFullName().trim());
        user.setEmail(form.getEmail().toLowerCase().trim());
        user.setPassword(passwordEncoder.encode(form.getTempPassword()));
        user.setRole(Role.RECRUITER);
        userRepository.save(user);

        RecruiterProfile profile = new RecruiterProfile();
        profile.setUser(user);
        profile.setCompanyName(form.getCompanyName().trim());
        String designation = form.getDesignation();
        profile.setDesignation(designation == null || designation.isBlank() ? null : designation.trim());
        recruiterProfileRepository.save(profile);

        return user;
    }
}