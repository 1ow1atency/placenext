package com.placenext.placenext.service;

import com.placenext.placenext.model.Application;
import com.placenext.placenext.model.Branch;
import com.placenext.placenext.model.DriveType;
import com.placenext.placenext.model.OfferStatus;
import com.placenext.placenext.model.StudentProfile;
import com.placenext.placenext.repository.ApplicationRepository;
import com.placenext.placenext.repository.StudentProfileRepository;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ReportService {

    private final StudentProfileRepository studentProfileRepository;
    private final ApplicationRepository applicationRepository;

    public ReportService(StudentProfileRepository studentProfileRepository,
                         ApplicationRepository applicationRepository) {
        this.studentProfileRepository = studentProfileRepository;
        this.applicationRepository = applicationRepository;
    }

    public Report build() {
        List<StudentProfile> students = studentProfileRepository.findAll();
        List<Application> placed = applicationRepository
                .findByOfferStatusOrderByRespondedAtDesc(OfferStatus.ACCEPTED);

        Report r = new Report();
        r.totalStudents = students.size();
        r.totalPlaced = placed.size();
        r.overallPercent = percent(r.totalPlaced, r.totalStudents);

        // ----- Branch-wise -----
        for (Branch b : Branch.values()) {
            long total = students.stream().filter(s -> s.getBranch() == b).count();
            if (total == 0) {
                continue;
            }
            long placedInBranch = placed.stream()
                    .filter(a -> a.getStudent().getBranch() == b).count();
            r.branches.add(new BranchRow(b.name(), total, placedInBranch, percent(placedInBranch, total)));
        }

        // ----- Company-wise -----
        Map<String, List<Application>> byCompany = new LinkedHashMap<>();
        for (Application a : placed) {
            byCompany.computeIfAbsent(a.getDrive().getCompanyName(), k -> new ArrayList<>()).add(a);
        }
        for (Map.Entry<String, List<Application>> e : byCompany.entrySet()) {
            List<Double> lpas = fullTimeLpas(e.getValue());
            Double avg = lpas.isEmpty() ? null : round2(lpas.stream().mapToDouble(d -> d).average().orElse(0));
            r.companies.add(new CompanyRow(e.getKey(), e.getValue().size(), avg));
        }
        r.companies.sort(Comparator.comparingLong(CompanyRow::getHires).reversed());

        // ----- Package stats (full-time only) -----
        List<Double> lpas = fullTimeLpas(placed);
        r.fullTimeOffers = lpas.size();
        r.internshipOffers = placed.stream()
                .filter(a -> a.getDrive().getType() == DriveType.INTERNSHIP).count();
        if (!lpas.isEmpty()) {
            r.highestLpa = round2(lpas.stream().mapToDouble(d -> d).max().orElse(0));
            r.lowestLpa = round2(lpas.stream().mapToDouble(d -> d).min().orElse(0));
            r.averageLpa = round2(lpas.stream().mapToDouble(d -> d).average().orElse(0));
        }

        return r;
    }

    private List<Double> fullTimeLpas(List<Application> applications) {
        return applications.stream()
                .filter(a -> a.getDrive().getType() != DriveType.INTERNSHIP)
                .map(a -> a.getDrive().getCtcLpa())
                .filter(Objects::nonNull)
                .toList();
    }

    private double percent(long part, long total) {
        return total == 0 ? 0 : Math.round(part * 1000.0 / total) / 10.0;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    // ----- Result types used by the page -----

    @Getter
    public static class Report {
        private long totalStudents;
        private long totalPlaced;
        private double overallPercent;
        private long fullTimeOffers;
        private long internshipOffers;
        private Double highestLpa;
        private Double lowestLpa;
        private Double averageLpa;
        private final List<BranchRow> branches = new ArrayList<>();
        private final List<CompanyRow> companies = new ArrayList<>();
    }

    @Getter
    public static class BranchRow {
        private final String branch;
        private final long total;
        private final long placed;
        private final double percent;

        public BranchRow(String branch, long total, long placed, double percent) {
            this.branch = branch;
            this.total = total;
            this.placed = placed;
            this.percent = percent;
        }
    }

    @Getter
    public static class CompanyRow {
        private final String company;
        private final long hires;
        private final Double averageLpa;

        public CompanyRow(String company, long hires, Double averageLpa) {
            this.company = company;
            this.hires = hires;
            this.averageLpa = averageLpa;
        }
    }
}