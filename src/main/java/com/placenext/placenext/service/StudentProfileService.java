package com.placenext.placenext.service;

import com.placenext.placenext.dto.StudentProfileForm;
import com.placenext.placenext.model.StudentProfile;
import com.placenext.placenext.model.User;
import com.placenext.placenext.repository.StudentProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class StudentProfileService {

    private final StudentProfileRepository profileRepository;
    private final FileStorageService fileStorageService;

    public StudentProfileService(StudentProfileRepository profileRepository, FileStorageService fileStorageService) {
        this.profileRepository = profileRepository;
        this.fileStorageService = fileStorageService;
    }

    public Optional<StudentProfile> findByUser(User user) {
        return profileRepository.findByUser(user);
    }

    public boolean rollNoTaken(String rollNo, User user) {
        return profileRepository.existsByRollNoAndUser_IdNot(rollNo.trim().toUpperCase(), user.getId());
    }

    @Transactional
    public StudentProfile save(User user, StudentProfileForm form, MultipartFile resume) throws IOException {
        StudentProfile profile = profileRepository.findByUser(user).orElseGet(() -> {
            StudentProfile p = new StudentProfile();
            p.setUser(user);
            return p;
        });

        // Store the new resume first: if it's invalid, nothing gets saved
        if (resume != null && !resume.isEmpty()) {
            String stored = fileStorageService.storeResume(resume);
            if (profile.getResumeFileName() != null) {
                fileStorageService.deleteResume(profile.getResumeFileName());
            }
            profile.setResumeFileName(stored);
            profile.setResumeOriginalName(resume.getOriginalFilename());
        }

        profile.setRollNo(form.getRollNo().trim().toUpperCase());
        profile.setBranch(form.getBranch());
        profile.setBatch(form.getBatch());
        profile.setCgpa(form.getCgpa());
        profile.setActiveBacklogs(form.getActiveBacklogs());
        profile.setPhone(form.getPhone().trim());
        profile.setUpdatedAt(LocalDateTime.now());

        return profileRepository.save(profile);
    }
}