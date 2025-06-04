package com.buildmaster.projecttracker.config;

import com.buildmaster.projecttracker.entity.Developer;
import com.buildmaster.projecttracker.entity.Project;
import com.buildmaster.projecttracker.entity.Task;
import com.buildmaster.projecttracker.repository.DeveloperRepository;
import com.buildmaster.projecttracker.repository.ProjectRepository;
import com.buildmaster.projecttracker.repository.TaskRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;

@Component
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

        developerRepository.saveAll(Arrays.asList(devAlice, devBob, devCarol));

        // Create projects
        Project projAlpha = Project.builder()
                .name("Alpha Project")
                .description("First project")
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().plusDays(20))
                .deadline(LocalDate.now().plusDays(20))
                .build();
        Project projBeta = Project.builder()
                .name("Beta Project")
                .description("Second project")
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().plusDays(30))
                .deadline(LocalDate.now().plusDays(30))
                .build();

        projectRepository.saveAll(Arrays.asList(projAlpha, projBeta));

        // Create tasks
        Task task1 = Task.builder()
                .title("Design DB Schema")
                .description("Design the initial database schema")
                .status(com.buildmaster.projecttracker.entity.TaskStatus.IN_PROGRESS)
                .startDate(LocalDate.now().minusDays(8))
                .endDate(LocalDate.now().plusDays(2))
                .project(projAlpha)
                .developer(devAlice)
                .build();
        Task task2 = Task.builder()
                .title("Implement API")
                .description("Develop REST endpoints")
                .status(com.buildmaster.projecttracker.entity.TaskStatus.TODO)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .project(projAlpha)
                .developer(devBob)
                .build();
        Task task3 = Task.builder()
                .title("Frontend UI")
                .description("Build the frontend interface")
                .status(com.buildmaster.projecttracker.entity.TaskStatus.TODO)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(15))
                .project(projBeta)
                .developer(devCarol)
                .build();

        taskRepository.saveAll(Arrays.asList(task1, task2, task3));
    }
}
