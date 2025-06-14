package com.buildmaster.projecttracker.controller;

import com.buildmaster.projecttracker.dto.AssignTaskRequest;
import com.buildmaster.projecttracker.entity.Task;
import com.buildmaster.projecttracker.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<Page<Task>> getAllTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ?
                    Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Task> tasks = taskService.findAll(pageable);

            log.info("Retrieved {} tasks", tasks.getTotalElements());
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            log.error("Error retrieving tasks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable Long id) {
        try {
            Optional<Task> task = taskService.findById(id);
            return task.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error retrieving task with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<Page<Task>> getTasksByProject(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Task> tasks = taskService.findByProjectId(projectId, pageable);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            log.error("Error retrieving tasks for project: {}", projectId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/developer/{developerId}")
    public ResponseEntity<Page<Task>> getTasksByDeveloper(
            @PathVariable Long developerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Task> tasks = taskService.findByDeveloperId(developerId, pageable);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            log.error("Error retrieving tasks for developer: {}", developerId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER', 'DEVELOPER')")
    @PostMapping
    public ResponseEntity<?> createTask(@Valid @RequestBody Task task) {
        try {
            Task savedTask = taskService.save(task);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedTask);
        } catch (Exception e) {
            log.error("Error creating task", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER', 'DEVELOPER')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTask(@PathVariable Long id, @Valid @RequestBody Task task) {
        try {
            Optional<Task> existingTask = taskService.findById(id);
            if (existingTask.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            task.setId(id);
            Task updatedTask = taskService.save(task);
            return ResponseEntity.ok(updatedTask);
        } catch (Exception e) {
            log.error("Error updating task with id: {}", id, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id) {
        try {
            Optional<Task> task = taskService.findById(id);
            if (task.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            taskService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting task with id: {}", id, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/assign")
    public ResponseEntity<?> assignTaskToDeveloper(@RequestBody AssignTaskRequest request) {
        try {
            Task updatedTask = taskService.assignTaskToDeveloper(request.getTaskId(), request.getDeveloperId());
            return ResponseEntity.ok(updatedTask);
        } catch (RuntimeException e) {
            log.error("Error assigning task {} to developer {}", request.getTaskId(), request.getDeveloperId(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<Task>> getOverdueTasks() {
        try {
            List<Task> tasks = taskService.findOverdueTasks();
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            log.error("Error retrieving overdue tasks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/stats/status")
    public ResponseEntity<Map<String, Long>> getTaskCountsByStatus() {
        try {
            List<Object[]> results = taskService.getTaskCountsByStatus();
            Map<String, Long> stats = new HashMap<>();

            for (Object[] result : results) {
                stats.put(result[0].toString(), (Long) result[1]);
            }

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error retrieving task statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}