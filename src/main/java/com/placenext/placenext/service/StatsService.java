package com.placenext.placenext.service;

import com.placenext.placenext.model.Application;
import com.placenext.placenext.model.ApplicationStatus;
import com.placenext.placenext.model.DriveStatus;
import com.placenext.placenext.model.OfferStatus;
import com.placenext.placenext.model.Role;
import com.placenext.placenext.repository.ApplicationRepository;
import com.placenext.placenext.repository.DriveRepository;
import com.placenext.placenext.repository.UserRepository;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StatsService {

    private final UserRepository userRepository;
    private final DriveRepository driveRepository;
    private final ApplicationRepository applicationRepository;

    public StatsService(UserRepository userRepository,
                        DriveRepository driveRepository,
                        ApplicationRepository applicationRepository) {
        this.userRepository = userRepository;
        this.driveRepository = driveRepository;
        this.applicationRepository = applicationRepository;
    }

    public Stats overview() {
        Stats s = new Stats();

        s.studentCount = userRepository.countByRole(Role.STUDENT);
        s.recruiterCount = userRepository.countByRole(Role.RECRUITER);

        var drives = driveRepository.findAll();
        s.driveCount = drives.size();
        s.approvedDrives = drives.stream().filter(d -> d.getStatus() == DriveStatus.APPROVED).count();
        s.pendingDrives = drives.stream().filter(d -> d.getStatus() == DriveStatus.PENDING).count();
        s.rejectedDrives = drives.stream().filter(d -> d.getStatus() == DriveStatus.REJECTED).count();

        List<Application> applications = applicationRepository.findAll();
        s.applicationCount = applications.stream()
                .filter(a -> a.getStatus() != ApplicationStatus.WITHDRAWN).count();
        s.shortlistedCount = applications.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.SHORTLISTED).count();
        s.pendingOffers = applications.stream().filter(Application::isOfferPending).count();

        s.placedCount = applicationRepository.countByOfferStatus(OfferStatus.ACCEPTED);
        s.placementPercent = s.studentCount == 0
                ? 0
                : Math.round(s.placedCount * 1000.0 / s.studentCount) / 10.0;

        return s;
    }

    @Getter
    public static class Stats {
        private long studentCount;
        private long recruiterCount;
        private long driveCount;
        private long approvedDrives;
        private long pendingDrives;
        private long rejectedDrives;
        private long applicationCount;
        private long shortlistedCount;
        private long pendingOffers;
        private long placedCount;
        private double placementPercent;
    }
}