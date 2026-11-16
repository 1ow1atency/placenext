package com.placenext.placenext.service;

import com.placenext.placenext.model.Application;
import com.placenext.placenext.model.ApplicationStatus;
import com.placenext.placenext.model.Branch;
import com.placenext.placenext.model.Drive;
import com.placenext.placenext.model.DriveType;
import com.placenext.placenext.model.OfferStatus;
import com.placenext.placenext.model.RecruiterProfile;
import com.placenext.placenext.model.StudentProfile;
import com.placenext.placenext.model.User;
import com.placenext.placenext.repository.ApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the placement policy in OfferService.
 * The repository and email service are Mockito mocks: no database, no real emails.
 */
class OfferServiceTest {

    private ApplicationRepository repo;
    private EmailService emailService;
    private OfferService service;

    private StudentProfile student;

    @BeforeEach
    void setUp() {
        repo = mock(ApplicationRepository.class);
        emailService = mock(EmailService.class);
        service = new OfferService(repo, emailService);

        student = student(1L, "test@thapar.edu");
        when(repo.existsByStudentAndOfferStatus(student, OfferStatus.ACCEPTED)).thenReturn(false);
    }

    // ---------- Tests ----------

    @Test
    @DisplayName("Accepting an offer places the student and cleans up other applications")
    void acceptPlacesStudent() {
        Application offer        = application(10L, student, drive("hr@acme.com"),  ApplicationStatus.SELECTED, OfferStatus.PENDING);
        Application otherOffer   = application(11L, student, drive("hr@beta.com"),  ApplicationStatus.SELECTED, OfferStatus.PENDING);
        Application openApp      = application(12L, student, drive("hr@gamma.com"), ApplicationStatus.SHORTLISTED, null);
        Application rejectedApp  = application(13L, student, drive("hr@delta.com"), ApplicationStatus.REJECTED, null);

        when(repo.findById(10L)).thenReturn(Optional.of(offer));
        when(repo.findByStudentOrderByAppliedAtDesc(student))
                .thenReturn(List.of(offer, otherOffer, openApp, rejectedApp));

        service.accept(student, 10L);

        assertThat(offer.getOfferStatus()).isEqualTo(OfferStatus.ACCEPTED);
        assertThat(offer.getRespondedAt()).isNotNull();
        assertThat(otherOffer.getOfferStatus()).isEqualTo(OfferStatus.DECLINED);   // auto-declined
        assertThat(openApp.getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);    // auto-withdrawn
        assertThat(rejectedApp.getStatus()).isEqualTo(ApplicationStatus.REJECTED); // untouched

        // recruiter of accepted offer + student confirmation + recruiter of auto-declined offer
        verify(emailService, times(3)).send(anyString(), anyString(), anyString());
        verify(emailService).send(eq("hr@beta.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("A placed student cannot accept a second offer")
    void cannotAcceptTwice() {
        Application offer = application(10L, student, drive("hr@acme.com"), ApplicationStatus.SELECTED, OfferStatus.PENDING);
        when(repo.findById(10L)).thenReturn(Optional.of(offer));
        when(repo.existsByStudentAndOfferStatus(student, OfferStatus.ACCEPTED)).thenReturn(true);

        assertThatThrownBy(() -> service.accept(student, 10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already accepted");

        assertThat(offer.getOfferStatus()).isEqualTo(OfferStatus.PENDING);
        verify(emailService, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("A student cannot answer someone else's offer")
    void cannotAnswerOthersOffer() {
        StudentProfile someoneElse = student(2L, "other@thapar.edu");
        Application offer = application(10L, someoneElse, drive("hr@acme.com"), ApplicationStatus.SELECTED, OfferStatus.PENDING);
        when(repo.findById(10L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> service.accept(student, 10L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.decline(student, 10L))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(offer.getOfferStatus()).isEqualTo(OfferStatus.PENDING);
    }

    @Test
    @DisplayName("Declining an offer marks it declined and emails the recruiter")
    void declineOffer() {
        Application offer = application(10L, student, drive("hr@acme.com"), ApplicationStatus.SELECTED, OfferStatus.PENDING);
        when(repo.findById(10L)).thenReturn(Optional.of(offer));

        service.decline(student, 10L);

        assertThat(offer.getOfferStatus()).isEqualTo(OfferStatus.DECLINED);
        assertThat(offer.getRespondedAt()).isNotNull();
        verify(emailService, times(1)).send(eq("hr@acme.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("An offer that was already answered cannot be answered again")
    void cannotAnswerTwice() {
        Application offer = application(10L, student, drive("hr@acme.com"), ApplicationStatus.SELECTED, OfferStatus.DECLINED);
        when(repo.findById(10L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> service.accept(student, 10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been answered");
    }

    @Test
    @DisplayName("A non-selected application is not an offer")
    void shortlistedIsNotAnOffer() {
        Application app = application(10L, student, drive("hr@acme.com"), ApplicationStatus.SHORTLISTED, null);
        when(repo.findById(10L)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> service.accept(student, 10L))
                .isInstanceOf(IllegalStateException.class);
    }

    // ---------- Test data helpers ----------

    private StudentProfile student(Long id, String email) {
        User user = new User();
        user.setEmail(email);
        user.setFullName("Student " + id);

        StudentProfile s = new StudentProfile();
        s.setId(id);
        s.setUser(user);
        s.setRollNo("10" + id);
        s.setBranch(Branch.CSE);
        return s;
    }

    private Drive drive(String recruiterEmail) {
        User user = new User();
        user.setEmail(recruiterEmail);
        user.setFullName("Recruiter");

        RecruiterProfile recruiter = new RecruiterProfile();
        recruiter.setUser(user);
        recruiter.setCompanyName("Company " + recruiterEmail);

        Drive d = new Drive();
        d.setTitle("SDE Intern");
        d.setType(DriveType.INTERNSHIP);
        d.setStipendPerMonth(20000);
        d.setRecruiter(recruiter);
        return d;
    }

    private Application application(Long id, StudentProfile s, Drive d,
                                    ApplicationStatus status, OfferStatus offerStatus) {
        Application a = new Application();
        a.setId(id);
        a.setStudent(s);
        a.setDrive(d);
        a.setStatus(status);
        a.setOfferStatus(offerStatus);
        return a;
    }
}