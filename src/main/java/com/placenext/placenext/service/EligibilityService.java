package com.placenext.placenext.service;

import com.placenext.placenext.dto.DriveEligibility;
import com.placenext.placenext.model.Drive;
import com.placenext.placenext.model.StudentProfile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EligibilityService {

    /** Returns the reasons a student can't apply. An empty list means eligible. */
    public List<String> check(StudentProfile profile, Drive drive) {
        List<String> reasons = new ArrayList<>();

        if (profile == null || !profile.isComplete()) {
            reasons.add("Complete your profile and upload your resume first");
            return reasons;
        }
        if (!drive.getAllowedBranches().contains(profile.getBranch())) {
            reasons.add("Open to " + drive.getBranchCodes() + " only");
        }
        if (!drive.getEligibleBatch().equals(profile.getBatch())) {
            reasons.add("Only for the batch of " + drive.getEligibleBatch());
        }
        if (profile.getCgpa() < drive.getMinCgpa()) {
            reasons.add("Needs CGPA " + drive.getMinCgpa() + " or above (yours: " + profile.getCgpa() + ")");
        }
        if (profile.getActiveBacklogs() > drive.getMaxBacklogs()) {
            reasons.add(drive.getMaxBacklogs() == 0
                    ? "No active backlogs allowed (you have " + profile.getActiveBacklogs() + ")"
                    : "Max " + drive.getMaxBacklogs() + " backlogs allowed (you have " + profile.getActiveBacklogs() + ")");
        }
        return reasons;
    }

    /** Checks every drive and returns eligible ones first. */
    public List<DriveEligibility> evaluate(StudentProfile profile, List<Drive> drives) {
        List<DriveEligibility> eligible = new ArrayList<>();
        List<DriveEligibility> notEligible = new ArrayList<>();
        for (Drive d : drives) {
            DriveEligibility e = new DriveEligibility(d, check(profile, d));
            (e.isEligible() ? eligible : notEligible).add(e);
        }
        eligible.addAll(notEligible);
        return eligible;
    }
}