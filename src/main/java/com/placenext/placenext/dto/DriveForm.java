package com.placenext.placenext.dto;

import com.placenext.placenext.model.Branch;
import com.placenext.placenext.model.DriveType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class DriveForm {

    @NotBlank(message = "Job title is required")
    @Size(max = 120)
    private String title;

    @NotNull(message = "Select full-time or internship")
    private DriveType type = DriveType.FULL_TIME;

    @Size(max = 4000, message = "Keep the description under 4000 characters")
    private String description;

    @Size(max = 120)
    private String location;

    @DecimalMin(value = "0.0", message = "Must be a positive number")
    @DecimalMax(value = "200.0", message = "Enter the package in LPA")
    private Double ctcLpa;

    @Min(value = 0, message = "Must be a positive number")
    private Integer stipendPerMonth;

    @NotNull(message = "Set an application deadline")
    @FutureOrPresent(message = "Deadline can't be in the past")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate deadline;

    @NotNull(message = "Minimum CGPA is required")
    @DecimalMin(value = "0.0", message = "CGPA must be between 0 and 10")
    @DecimalMax(value = "10.0", message = "CGPA must be between 0 and 10")
    private Double minCgpa = 6.0;

    @NotNull(message = "Max backlogs is required")
    @Min(value = 0, message = "Can't be negative")
    private Integer maxBacklogs = 0;

    @NotNull(message = "Passing year is required")
    @Min(value = 2020, message = "Enter a valid passing year")
    @Max(value = 2035, message = "Enter a valid passing year")
    private Integer eligibleBatch;

    @NotEmpty(message = "Select at least one branch")
    private Set<Branch> allowedBranches = new HashSet<>();
}
