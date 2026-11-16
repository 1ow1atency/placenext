package com.placenext.placenext.service;

import com.placenext.placenext.dto.DriveEligibility;
import com.placenext.placenext.model.Branch;
import com.placenext.placenext.model.Drive;
import com.placenext.placenext.model.StudentProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

/**
 * Unit tests for the eligibility rules (no Spring, no database).
 * The student profile is a Mockito "spy" so we can say "profile is complete"
 * without having to upload a real resume.
 */
class EligibilityServiceTest {

    private EligibilityService service;
    private StudentProfile student;
    private Drive drive;

    @BeforeEach
    void setUp() {
        service = new EligibilityService();

        // A student who meets every requirement of the drive below
        student = spy(new StudentProfile());
        doReturn(true).when(student).isComplete();
        student.setBranch(Branch.CSE);
        student.setBatch(2028);
        student.setCgpa(8.0);
        student.setActiveBacklogs(0);

        drive = new Drive();
        drive.setTitle("Test Drive");
        drive.setAllowedBranches(new HashSet<>(Set.of(Branch.CSE, Branch.ECE)));
        drive.setEligibleBatch(2028);
        drive.setMinCgpa(7.0);
        drive.setMaxBacklogs(0);
    }

    @Test
    @DisplayName("Student meeting every rule is eligible")
    void eligibleStudent() {
        assertThat(service.check(student, drive)).isEmpty();
    }

    @Test
    @DisplayName("No profile -> asked to complete profile")
    void nullProfile() {
        List<String> reasons = service.check(null, drive);
        assertThat(reasons).hasSize(1);
        assertThat(reasons.get(0)).contains("Complete your profile");
    }

    @Test
    @DisplayName("Incomplete profile -> asked to complete profile, nothing else checked")
    void incompleteProfile() {
        doReturn(false).when(student).isComplete();
        List<String> reasons = service.check(student, drive);
        assertThat(reasons).hasSize(1);
        assertThat(reasons.get(0)).contains("Complete your profile");
    }

    @Test
    @DisplayName("Branch not allowed -> not eligible")
    void wrongBranch() {
        student.setBranch(Branch.ME);
        List<String> reasons = service.check(student, drive);
        assertThat(reasons).hasSize(1);
        assertThat(reasons.get(0)).contains("only");
    }

    @Test
    @DisplayName("Different batch -> not eligible")
    void wrongBatch() {
        student.setBatch(2027);
        List<String> reasons = service.check(student, drive);
        assertThat(reasons).hasSize(1);
        assertThat(reasons.get(0)).contains("batch of 2028");
    }

    @Test
    @DisplayName("CGPA below minimum -> not eligible")
    void lowCgpa() {
        student.setCgpa(6.5);
        List<String> reasons = service.check(student, drive);
        assertThat(reasons).hasSize(1);
        assertThat(reasons.get(0)).contains("CGPA");
    }

    @Test
    @DisplayName("CGPA exactly at minimum -> still eligible")
    void cgpaExactlyAtMinimum() {
        student.setCgpa(7.0);
        assertThat(service.check(student, drive)).isEmpty();
    }

    @Test
    @DisplayName("Backlogs when none allowed -> not eligible")
    void tooManyBacklogs() {
        student.setActiveBacklogs(2);
        List<String> reasons = service.check(student, drive);
        assertThat(reasons).hasSize(1);
        assertThat(reasons.get(0)).contains("No active backlogs allowed");
    }

    @Test
    @DisplayName("Several rules broken -> one reason per rule")
    void severalFailures() {
        student.setBranch(Branch.ME);
        student.setCgpa(5.0);
        student.setActiveBacklogs(1);
        assertThat(service.check(student, drive)).hasSize(3);
    }

    @Test
    @DisplayName("evaluate() lists eligible drives first")
    void evaluateOrdersEligibleFirst() {
        Drive closedToCse = new Drive();
        closedToCse.setTitle("ME only");
        closedToCse.setAllowedBranches(new HashSet<>(Set.of(Branch.ME)));
        closedToCse.setEligibleBatch(2028);
        closedToCse.setMinCgpa(6.0);
        closedToCse.setMaxBacklogs(0);

        List<DriveEligibility> result = service.evaluate(student, List.of(closedToCse, drive));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).isEligible()).isTrue();
        assertThat(result.get(1).isEligible()).isFalse();
    }
}