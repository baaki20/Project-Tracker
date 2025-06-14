package com.buildmaster.projecttracker.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "admins")
@Data
@NoArgsConstructor
public class Admin {
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

    @Column(name = "access_level")
    private String accessLevel;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "can_modify_users")
    private boolean canModifyUsers;

    @Column(name = "can_modify_projects")
    private boolean canModifyProjects;

    public Admin(User user) {
        this.user = user;
        this.userId = user.getId();
        this.active = true;
        this.dateJoined = LocalDateTime.now();
        this.canModifyUsers = true;
        this.canModifyProjects = true;
        this.accessLevel = "FULL";
    }
}
