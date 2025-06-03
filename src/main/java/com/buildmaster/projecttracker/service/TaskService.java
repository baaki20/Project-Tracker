package com.buildmaster.projecttracker.service;

import com.buildmaster.projecttracker.audit.AuditLog;
import com.buildmaster.projecttracker.audit.AuditLogRepository;
import com.buildmaster.projecttracker.entity.Developer;
import com.buildmaster.projecttracker.entity.Task;
import com.buildmaster.projecttracker.entity.TaskStatus;
import com.buildmaster.projecttracker.repository.DeveloperRepository;
import com.buildmaster.projecttracker.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public Page<Task> findAll(Pageable pageable) {
        return taskRepository.findAll(pageable);
    }

    public Page<Task> findByProjectId(Long projectId, Pageable pageable) {
        return taskRepository.findByProjectId(projectId, pageable);
    }

    public Page<Task> findByDeveloperId(Long developerId, Pageable pageable) {
        return taskRepository.findByDeveloperId(developerId, pageable);
    }

    public List<Task> findOverdueTasks() {
        return taskRepository.findOverdueTasks(LocalDate.now());
    }

    @Transactional
    public Task assignTaskToDeveloper(Long taskId, Long developerId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        Developer developer = developerRepository.findById(developerId)
                .orElseThrow(() -> new RuntimeException("Developer not found"));

        task.setDeveloper(developer);
        Task savedTask = taskRepository.save(task);

        // Create audit log
        Map<String, Object> payload = createTaskPayload(savedTask);
        payload.put("assignedDeveloper", developer.getName());
        auditLogRepository.save(new AuditLog("UPDATE", "Task",
                taskId.toString(), "system", payload));

        log.info("Task {} assigned to developer {}", task.getTitle(), developer.getName());
        return savedTask;
    }

    @Transactional
    public Task save(Task task) {
        boolean isNew = task.getId() == null;
        Task savedTask = taskRepository.save(task);

        // Create audit log
        Map<String, Object> payload = createTaskPayload(savedTask);
        String actionType = isNew ? "CREATE" : "UPDATE";
        auditLogRepository.save(new AuditLog(actionType, "Task",
                savedTask.getId().toString(), "system", payload));

        return savedTask;
    }

    public List<Object[]> getTaskCountsByStatus() {
        return taskRepository.countTasksByStatus();
    }

    private Map<String, Object> createTaskPayload(Task task) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", task.getId());
        payload.put("title", task.getTitle());
        payload.put("description", task.getDescription());
        payload.put("status", task.getStatus());
        payload.put("dueDate", task.getDueDate());
        payload.put("projectId", task.getProject().getId());
        if (task.getDeveloper() != null) {
            payload.put("developerId", task.getDeveloper().getId());
        }
        return payload;
    }
}