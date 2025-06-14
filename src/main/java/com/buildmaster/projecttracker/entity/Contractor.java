package com.buildmaster.projecttracker.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "contractors")
@Data
@NoArgsConstructor
public class Contractor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "date_joined", nullable = false)
    private LocalDateTime dateJoined;

    @Column(name = "specialization")
    private String specialization;

    @Column(name = "hourly_rate")
    private Double hourlyRate;

    @Column(name = "availability_status")
    private String availabilityStatus;

    @Column(name = "contract_end_date")
    private LocalDateTime contractEndDate;

    public Contractor(User user) {
        this.user = user;
        this.userId = user.getId();
        this.active = true;
        this.dateJoined = LocalDateTime.now();
    }
}
