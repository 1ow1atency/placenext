package com.placenext.placenext.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "drives")
@Getter
@Setter
@NoArgsConstructor
public class Drive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "recruiter_id", nullable = false)
    private RecruiterProfile recruiter;

    @Column(nullable = false, length = 120)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DriveType type;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 120)
    private String location;

    private Double ctcLpa;            // full-time: package in LPA
    private Integer stipendPerMonth;  // internship: stipend in rupees per month

    @Column(nullable = false)
    private LocalDate deadline;

    // ----- Eligibility rules -----
    @Column(nullable = false)
    private Double minCgpa;

    @Column(nullable = false)
    private Integer maxBacklogs;

    @Column(nullable = false)
    private Integer eligibleBatch;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "drive_branches", joinColumns = @JoinColumn(name = "drive_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "branch", length = 10)
    private Set<Branch> allowedBranches = new HashSet<>();

    // ----- Workflow -----
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DriveStatus status = DriveStatus.PENDING;

    private String rejectionReason;

    private LocalDateTime createdAt = LocalDateTime.now();

    // ----- Helpers used by the pages -----
    public String getCompanyName() {
        return recruiter.getCompanyName();
    }

    public boolean isDeadlinePassed() {
        return LocalDate.now().isAfter(deadline);
    }

    public String getCompensation() {
        if (type == DriveType.INTERNSHIP) {
            return stipendPerMonth == null ? "—" : "₹" + String.format("%,d", stipendPerMonth) + " / month";
        }
        return ctcLpa == null ? "—" : new DecimalFormat("0.##").format(ctcLpa) + " LPA";
    }

    public String getBranchCodes() {
        return allowedBranches.stream().map(Enum::name).sorted().collect(Collectors.joining(", "));
    }
}