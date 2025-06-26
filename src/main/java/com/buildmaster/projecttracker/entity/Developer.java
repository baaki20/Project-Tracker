package com.buildmaster.projecttracker.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "developers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"tasks", "user"})
@ToString(exclude = {"tasks", "user"})
public class Developer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Developer name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email")
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Size(max = 500, message = "Skills cannot exceed 500 characters")
    @Column(length = 500)
    private String skills;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"developer", "password", "roles"})
    private User user;

    @OneToMany(mappedBy = "developer", cascade = CascadeType.ALL)
    @JsonIgnoreProperties({"developer", "hibernateLazyInitializer", "handler"})
    @Builder.Default
    private List<Task> tasks = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "developer_roles",
        joinColumns = @JoinColumn(name = "developer_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    public Developer(String name, String email, String skills) {
        this.name = name;
        this.email = email;
        this.skills = skills;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addTask(Task task) {
        if (tasks == null) {
            tasks = new ArrayList<>();
        }
        tasks.add(task);
        task.setDeveloper(this);
    }

    public void removeTask(Task task) {
        if (tasks != null) {
            tasks.remove(task);
            task.setDeveloper(null);
        }
    }

    public Long getUserId() {
        return user != null ? user.getId() : null;
    }

    public boolean hasUser() {
        return user != null;
    }

    public Object getUsername() {
        return user != null ? user.getUsername() : null;
    }
}