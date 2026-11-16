package com.placenext.placenext.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "rounds")
@Getter
@Setter
@NoArgsConstructor
public class Round {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "drive_id", nullable = false)
    private Drive drive;

    @Column(nullable = false)
    private Integer roundNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RoundType type;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Column(length = 200)
    private String venue;            // room, address or meeting link

    @Column(length = 1000)
    private String instructions;

    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean isPast() {
        return scheduledAt.isBefore(LocalDateTime.now());
    }
}