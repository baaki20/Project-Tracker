package com.buildmaster.projecttracker.controller;

import com.buildmaster.projecttracker.entity.Task;
import com.buildmaster.projecttracker.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<Page<Task>> getAllTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dueDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Task> tasks = taskService.findAll(pageable);

        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<Page<Task>> getTasksByProject(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Task> tasks = taskService.findByProjectId(projectId, pageable);

        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/developer/{developerId}")
    public ResponseEntity<Page<Task>> getTasksByDeveloper(
            @PathVariable Long developerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Task> tasks = taskService.findByDeveloperId(developerId, pageable);

        return ResponseEntity.ok(tasks);
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@Valid @RequestBody Task task) {
        Task savedTask = taskService.save(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTask);
    }

    @PutMapping("/{id}/assign/{developerId}")
    public ResponseEntity<Task> assignTaskToDeveloper(@PathVariable Long id,
                                                      @PathVariable Long developerId) {
        try {
            Task updatedTask = taskService.assignTaskToDeveloper(id, developerId);
            return ResponseEntity.ok(updatedTask);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<Task>> getOverdueTasks() {
        List<Task> tasks = taskService.findOverdueTasks();
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/stats/status")
    public ResponseEntity<Map<String, Long>> getTaskCountsByStatus() {
        List<Object[]> results = taskService.getTaskCountsByStatus();
        Map<String, Long> stats = new HashMap<>();

        for (Object[] result : results) {
            stats.put(result[0].toString(), (Long) result[1]);
        }

        return ResponseEntity.ok(stats);
    }
}