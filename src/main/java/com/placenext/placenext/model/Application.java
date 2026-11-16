package com.placenext.placenext.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "applications",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "drive_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile student;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "drive_id", nullable = false)
    private Drive drive;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    /** Null until the student is selected; then PENDING, ACCEPTED or DECLINED. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private OfferStatus offerStatus;

    /** When the offer was made (the student was selected). */
    private LocalDateTime offerAt;

    /** When the student accepted or declined. */
    private LocalDateTime respondedAt;

    private LocalDateTime appliedAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    /** Selected and the student has not answered yet (also covers rows selected before offers existed). */
    public boolean isOfferPending() {
        return status == ApplicationStatus.SELECTED
                && (offerStatus == null || offerStatus == OfferStatus.PENDING);
    }

    public boolean isOfferAccepted() {
        return status == ApplicationStatus.SELECTED && offerStatus == OfferStatus.ACCEPTED;
    }

    public boolean isOfferDeclined() {
        return status == ApplicationStatus.SELECTED && offerStatus == OfferStatus.DECLINED;
    }
}