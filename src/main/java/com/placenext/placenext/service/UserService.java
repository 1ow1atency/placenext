package com.placenext.placenext.service;

import com.placenext.placenext.dto.RegisterForm;
import com.placenext.placenext.model.Role;
import com.placenext.placenext.model.User;
import com.placenext.placenext.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
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
}