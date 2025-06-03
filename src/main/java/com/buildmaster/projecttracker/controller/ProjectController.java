package com.buildmaster.projecttracker.controller;

import com.buildmaster.projecttracker.entity.Project;
import com.buildmaster.projecttracker.entity.ProjectStatus;
import com.buildmaster.projecttracker.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<Page<Project>> getAllProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Project> projects = projectService.findAll(pageable);

        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProjectById(@PathVariable Long id) {
        return projectService.findById(id)
                .map(project -> ResponseEntity.ok(project))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Project> createProject(@Valid @RequestBody Project project) {
        Project savedProject = projectService.save(project);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedProject);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Project> updateProject(@PathVariable Long id,
                                                 @Valid @RequestBody Project project) {
        return projectService.findById(id)
                .map(existingProject -> {
                    project.setId(id);
                    Project updatedProject = projectService.save(project);
                    return ResponseEntity.ok(updatedProject);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        return projectService.findById(id)
                .map(project -> {
                    projectService.deleteById(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<Project>> getProjectsByStatus(
            @PathVariable ProjectStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Project> projects = projectService.findByStatus(status, pageable);

        return ResponseEntity.ok(projects);
    }

    @GetMapping("/without-tasks")
    public ResponseEntity<List<Project>> getProjectsWithoutTasks() {
        List<Project> projects = projectService.findProjectsWithoutTasks();
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<Project>> getOverdueProjects() {
        List<Project> projects = projectService.findOverdueProjects();
        return ResponseEntity.ok(projects);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Project> updateProjectStatus(@PathVariable Long id,
                                                       @RequestBody ProjectStatus status) {
        try {
            Project updatedProject = projectService.updateStatus(id, status);
            return ResponseEntity.ok(updatedProject);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
