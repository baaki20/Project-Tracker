package com.buildmaster.projecttracker.config;

import com.buildmaster.projecttracker.entity.*;
import com.buildmaster.projecttracker.repository.DeveloperRepository;
import com.buildmaster.projecttracker.repository.ProjectRepository;
import com.buildmaster.projecttracker.repository.TaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j  // Add this annotation
public class DataInitializer implements CommandLineRunner {

    private final ProjectRepository projectRepository;
    private final DeveloperRepository developerRepository;
    private final TaskRepository taskRepository;

    public DataInitializer(ProjectRepository projectRepository,
                           DeveloperRepository developerRepository,
                           TaskRepository taskRepository) {
        this.projectRepository = projectRepository;
        this.developerRepository = developerRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    public void run(String... args) {
        try {
            log.info("Starting data initialization...");

            // Clear existing data (optional, for demo)
            taskRepository.deleteAll();
            projectRepository.deleteAll();
            developerRepository.deleteAll();

            // Create developers
            Developer devAlice = Developer.builder()
                    .name("Alice Johnson")
                    .email("alice@example.com")
                    .build();
            Developer devBob = Developer.builder()
                    .name("Bob Smith")
                    .email("bob@example.com")
                    .build();
            Developer devCarol = Developer.builder()
                    .name("Carol Lee")
                    .email("carol@example.com")
                    .build();

            List<Developer> savedDevs = developerRepository.saveAll(Arrays.asList(devAlice, devBob, devCarol));
            log.info("Created {} developers", savedDevs.size());

            // Create projects
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

            // Get the saved entities with IDs
            Project savedAlpha = savedProjects.get(0);
            Project savedBeta = savedProjects.get(1);
            Developer savedAlice = savedDevs.get(0);
            Developer savedBob = savedDevs.get(1);
            Developer savedCarol = savedDevs.get(2);

            // Create tasks
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
    }
}
