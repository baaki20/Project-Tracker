package com.buildmaster.projecttracker.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "managers")
@Data
@NoArgsConstructor
public class Manager {
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

    @Column(name = "department")
    private String department;

    @Column(name = "team_size")
    private Integer teamSize;

    @Column(name = "projects_managed")
    private Integer projectsManaged;

    @Column(name = "management_level")
    private String managementLevel;

    public Manager(User user) {
        this.user = user;
        this.userId = user.getId();
        this.active = true;
        this.dateJoined = LocalDateTime.now();
        this.teamSize = 0;
        this.projectsManaged = 0;
    }
}
