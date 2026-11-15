package com.placenext.placenext.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRecruiterForm {

    @NotBlank(message = "Name is required")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email")
    private String email;

    @NotBlank(message = "Company name is required")
    @Size(max = 120)
    private String companyName;

    @Size(max = 80)
    private String designation;

    @NotBlank(message = "Set a temporary password")
    @Size(min = 8, message = "At least 8 characters")
    private String tempPassword;
}