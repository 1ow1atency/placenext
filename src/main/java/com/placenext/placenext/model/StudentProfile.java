package com.placenext.placenext.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_profiles")
@Getter
@Setter
@NoArgsConstructor
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, unique = true, length = 30)
    private String rollNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Branch branch;

    @Column(nullable = false)
    private Integer batch;            // passing year, e.g. 2028

    @Column(nullable = false)
    private Double cgpa;

    @Column(nullable = false)
    private Integer activeBacklogs;

    @Column(nullable = false, length = 15)
    private String phone;

    private String resumeFileName;      // name on disk (random)
    private String resumeOriginalName;  // name the student uploaded

    private LocalDateTime updatedAt;

    // All other fields are required to save, so a profile is complete once a resume is uploaded
    public boolean isComplete() {
        return resumeFileName != null;
    }
}