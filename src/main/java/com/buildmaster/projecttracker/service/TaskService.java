package com.buildmaster.projecttracker.service;

import com.buildmaster.projecttracker.audit.AuditLog;
import com.buildmaster.projecttracker.repository.AuditLogRepository;
import com.buildmaster.projecttracker.entity.Developer;
import com.buildmaster.projecttracker.entity.Task;
import com.buildmaster.projecttracker.repository.DeveloperRepository;
import com.buildmaster.projecttracker.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final DeveloperRepository developerRepository;
    private final AuditLogRepository auditLogRepository;

    @Cacheable(value = "tasks")
    public Optional<Task> findById(Long id) {
        log.debug("Fetching task with id: {}", id);
        return taskRepository.findById(id);
    }

    @Cacheable(value = "tasks")
    public Page<Task> findAll(Pageable pageable) {
        return taskRepository.findAll(pageable);
    }

    @Cacheable(value = "tasks")
    public Page<Task> findByProjectId(Long projectId, Pageable pageable) {
        return taskRepository.findByProjectId(projectId, pageable);
    }

    @Cacheable(value = "tasks")
    public Page<Task> findByDeveloperId(Long developerId, Pageable pageable) {
        return taskRepository.findByDeveloperId(developerId, pageable);
    }

    @Cacheable(value = "tasks")
    public List<Task> findOverdueTasks() {
        return taskRepository.findOverdueTasks(LocalDate.now());
    }

    @Transactional
    @CacheEvict(value = "tasks", allEntries = true)
    public Task assignTaskToDeveloper(Long taskId, Long developerId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        Developer developer = developerRepository.findById(developerId)
                .orElseThrow(() -> new RuntimeException("Developer not found"));

        task.setDeveloper(developer);
        Task savedTask = taskRepository.save(task);

        // Create audit log with String-based payload
        Map<String, String> payload = createTaskStringPayload(savedTask);
        payload.put("assignedDeveloper", developer.getName()); // Add assigned developer name to payload
        auditLogRepository.save(new AuditLog("UPDATE", "Task",
                taskId.toString(), "system", payload));

        log.info("Task {} assigned to developer {}", task.getTitle(), developer.getName());
        return savedTask;
    }

    @Transactional
    @CacheEvict(value = "tasks", allEntries = true)
    public Task save(Task task) {
        boolean isNew = task.getId() == null;
        Task savedTask = taskRepository.save(task);

        // Create audit log with String-based payload
        Map<String, String> payload = createTaskStringPayload(savedTask);
        String actionType = isNew ? "CREATE" : "UPDATE";
        auditLogRepository.save(new AuditLog(actionType, "Task",
                savedTask.getId().toString(), "system", payload));

        log.info("Task {} successfully: {}", actionType.toLowerCase(), savedTask.getTitle());
        return savedTask;
    }

    @Transactional
    @CacheEvict(value = "tasks", allEntries = true)
    public void deleteById(Long id) {
        Optional<Task> task = taskRepository.findById(id);
        if (task.isPresent()) {
            taskRepository.deleteById(id);

            // Create audit log with String-based payload
            Map<String, String> payload = createTaskStringPayload(task.get());
            auditLogRepository.save(new AuditLog("DELETE", "Task",
                    id.toString(), "system", payload));

            log.info("Task deleted successfully: {}", task.get().getTitle());
        }
    }

    @Cacheable(value = "tasks")
    public List<Object[]> getTaskCountsByStatus() {
        return taskRepository.countTasksByStatus();
    }

    /**
     * Creates a payload map for AuditLog, ensuring all values are strings.
     * This method is crucial for compatibility with AuditLog's Map<String, String> payload.
     * Converts various object types to String for consistent audit logging.
     * @param task The Task object from which to create the payload.
     * @return A Map<String, String> representing the task's data.
     */
    private Map<String, String> createTaskStringPayload(Task task) {
        Map<String, String> payload = new HashMap<>();
        payload.put("id", task.getId() != null ? task.getId().toString() : null);
        payload.put("title", task.getTitle());
        payload.put("description", task.getDescription());
        payload.put("status", task.getStatus() != null ? task.getStatus().toString() : null); // Convert Enum to String
        payload.put("dueDate", task.getDueDate() != null ? task.getDueDate().toString() : null); // Convert LocalDate to String
        payload.put("startDate", task.getStartDate() != null ? task.getStartDate().toString() : null); // Convert LocalDate to String
        payload.put("endDate", task.getEndDate() != null ? task.getEndDate().toString() : null); // Convert LocalDate to String

        if (task.getProject() != null) {
            payload.put("projectId", task.getProject().getId() != null ? task.getProject().getId().toString() : null);
            payload.put("projectName", task.getProject().getName());
        }
        if (task.getDeveloper() != null) {
            payload.put("developerId", task.getDeveloper().getId() != null ? task.getDeveloper().getId().toString() : null);
            payload.put("developerName", task.getDeveloper().getName());
        }
        return payload;
    }
}
