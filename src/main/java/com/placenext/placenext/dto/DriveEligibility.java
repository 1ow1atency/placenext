package com.placenext.placenext.dto;

import com.placenext.placenext.model.Drive;
import lombok.Getter;

import java.util.List;

@Getter
public class DriveEligibility {

    private final Drive drive;
    private final List<String> reasons;   // empty = eligible

    public DriveEligibility(Drive drive, List<String> reasons) {
        this.drive = drive;
        this.reasons = reasons;
    }

    public boolean isEligible() {
        return reasons.isEmpty();
    }
}
