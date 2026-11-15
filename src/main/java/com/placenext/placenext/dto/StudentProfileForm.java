package com.placenext.placenext.dto;

import com.placenext.placenext.model.Branch;
import com.placenext.placenext.model.StudentProfile;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentProfileForm {

    @NotBlank(message = "Roll number is required")
    @Size(max = 30, message = "Roll number is too long")
    private String rollNo;

    @NotNull(message = "Select your branch")
    private Branch branch;

    @NotNull(message = "Passing year is required")
    @Min(value = 2020, message = "Enter a valid passing year")
    @Max(value = 2035, message = "Enter a valid passing year")
    private Integer batch;

    @NotNull(message = "CGPA is required")
    @DecimalMin(value = "0.0", message = "CGPA must be between 0 and 10")
    @DecimalMax(value = "10.0", message = "CGPA must be between 0 and 10")
    private Double cgpa;

    @NotNull(message = "Enter 0 if you have no backlogs")
    @Min(value = 0, message = "Backlogs can't be negative")
    @Max(value = 50, message = "Enter a valid number")
    private Integer activeBacklogs;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit mobile number")
    private String phone;

    public static StudentProfileForm from(StudentProfile p) {
        StudentProfileForm f = new StudentProfileForm();
        f.setRollNo(p.getRollNo());
        f.setBranch(p.getBranch());
        f.setBatch(p.getBatch());
        f.setCgpa(p.getCgpa());
        f.setActiveBacklogs(p.getActiveBacklogs());
        f.setPhone(p.getPhone());
        return f;
    }
}