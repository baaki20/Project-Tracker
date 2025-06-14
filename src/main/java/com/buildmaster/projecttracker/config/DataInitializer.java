package com.buildmaster.projecttracker.config;

import com.buildmaster.projecttracker.entity.*;
import com.buildmaster.projecttracker.enums.ProjectStatus;
import com.buildmaster.projecttracker.enums.TaskStatus;
import com.buildmaster.projecttracker.repository.DeveloperRepository;
import com.buildmaster.projecttracker.repository.ProjectRepository;
import com.buildmaster.projecttracker.repository.TaskRepository;
import com.buildmaster.projecttracker.repository.RoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
@Transactional
public class DataInitializer implements CommandLineRunner {

    private final ProjectRepository projectRepository;
    private final DeveloperRepository developerRepository;
    private final TaskRepository taskRepository;
    private final RoleRepository roleRepository;

    public DataInitializer(ProjectRepository projectRepository,
                           DeveloperRepository developerRepository,
                           TaskRepository taskRepository,
                           RoleRepository roleRepository) {
        this.projectRepository = projectRepository;
        this.developerRepository = developerRepository;
        this.taskRepository = taskRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        try {
            log.info("Starting data initialization...");

            taskRepository.deleteAll();
            projectRepository.deleteAll();
            developerRepository.deleteAll();

            List<String> requiredRoles = Arrays.asList(
                    "ROLE_ADMIN",
                    "ROLE_MANAGER",
                    "ROLE_DEVELOPER",
                    "ROLE_CONTRACTOR"
            );
            List<Role> savedRoles = new ArrayList<>();
            for (String roleName : requiredRoles) {
                Role role = roleRepository.findByName(roleName)
                        .orElse(null);
                if (role == null) {
                    role = new Role();
                    role.setName(roleName);
                    role = roleRepository.save(role);
                    log.info("Created new role: {}", roleName);
                }
                savedRoles.add(role);
            }
            log.info("Verified roles exist: {}", savedRoles.stream()
                .map(Role::getName)
                .collect(Collectors.toList()));

            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN should exist"));

            Developer adminUser = Developer.builder()
                    .name("System Admin")
                    .email("admin@system.com")
                    .roles(Set.of(adminRole))
                    .build();

            developerRepository.save(adminUser);
            log.info("Created admin user");

            String defaultRoleName = "ROLE_DEVELOPER";
            Role developerRole = roleRepository.findByName(defaultRoleName)
                    .orElseThrow(() -> new IllegalStateException("ROLE_DEVELOPER should exist"));

            Developer devAlice = Developer.builder()
                    .name("Alice Johnson")
                    .email("alice@example.com")
                    .roles(Set.of(developerRole))
                    .build();
            Developer devBob = Developer.builder()
                    .name("Bob Smith")
                    .email("bob@example.com")
                    .roles(Set.of(developerRole))
                    .build();
            Developer devCarol = Developer.builder()
                    .name("Carol Lee")
                    .email("carol@example.com")
                    .roles(Set.of(developerRole))
                    .build();

            List<Developer> savedDevs = developerRepository.saveAll(Arrays.asList(devAlice, devBob, devCarol));
            log.info("Created {} developers", savedDevs.size());

            Project projAlpha = Project.builder()
                    .name("Alpha Project")
                    .description("First project")
                    .startDate(LocalDate.now().minusDays(10))
                    .endDate(LocalDate.now().plusDays(20))
                    .deadline(LocalDate.now().plusDays(20))
                    .status(ProjectStatus.PLANNING)
                    .build();
            Project projBeta = Project.builder()
                    .name("Beta Project")
                    .description("Second project")
                    .startDate(LocalDate.now().minusDays(5))
                    .endDate(LocalDate.now().plusDays(30))
                    .deadline(LocalDate.now().plusDays(30))
                    .status(ProjectStatus.IN_PROGRESS)
                    .build();

            List<Project> savedProjects = projectRepository.saveAll(Arrays.asList(projAlpha, projBeta));
            log.info("Created {} projects", savedProjects.size());

            Project savedAlpha = savedProjects.get(0);
            Project savedBeta = savedProjects.get(1);
            Developer savedAlice = savedDevs.get(0);
            Developer savedBob = savedDevs.get(1);
            Developer savedCarol = savedDevs.get(2);

            Task task1 = Task.builder()
                    .title("Design DB Schema")
                    .description("Design the initial database schema")
                    .status(TaskStatus.IN_PROGRESS)
                    .startDate(LocalDate.now().minusDays(8))
                    .endDate(LocalDate.now().plusDays(2))
                    .project(savedAlpha)
                    .developer(savedAlice)
                    .build();
            Task task2 = Task.builder()
                    .title("Implement API")
                    .description("Develop REST endpoints")
                    .status(TaskStatus.TODO)
                    .startDate(LocalDate.now())
                    .endDate(LocalDate.now().plusDays(10))
                    .project(savedAlpha)
                    .developer(savedBob)
                    .build();
            Task task3 = Task.builder()
                    .title("Frontend UI")
                    .description("Build the frontend interface")
                    .status(TaskStatus.TODO)
                    .startDate(LocalDate.now())
                    .endDate(LocalDate.now().plusDays(15))
                    .project(savedBeta)
                    .developer(savedCarol)
                    .build();

            List<Task> savedTasks = taskRepository.saveAll(Arrays.asList(task1, task2, task3));
            log.info("Created {} tasks", savedTasks.size());

            log.info("Data initialization completed successfully!");

        } catch (Exception e) {
            log.error("Error during data initialization: ", e);
            throw new RuntimeException("Failed to initialize data", e);
        }

        roleRepository.findAll().forEach(role -> {
            log.info("Verifying role in database: {}", role.getName());
        });
    }
}