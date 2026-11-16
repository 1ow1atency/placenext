package com.placenext.placenext.dto;

import com.placenext.placenext.model.RoundType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
public class RoundForm {

    @NotNull(message = "Select the round type")
    private RoundType type;

    @NotNull(message = "Set the date and time")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime scheduledAt;

    @NotBlank(message = "Enter a venue or meeting link")
    @Size(max = 200)
    private String venue;

    @Size(max = 1000, message = "Keep instructions under 1000 characters")
    private String instructions;
}