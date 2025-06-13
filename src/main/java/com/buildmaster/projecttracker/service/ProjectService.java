package com.buildmaster.projecttracker.service;

import com.buildmaster.projecttracker.audit.AuditLog;
import com.buildmaster.projecttracker.repository.AuditLogRepository;
import com.buildmaster.projecttracker.entity.Project;
import com.buildmaster.projecttracker.enums.ProjectStatus;
import com.buildmaster.projecttracker.repository.ProjectRepository;
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
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final AuditLogRepository auditLogRepository;

    @Cacheable(value = "projects", key = "#id")
    public Optional<Project> findById(Long id) {
        log.debug("Fetching project with id: {}", id);
        return projectRepository.findById(id);
    }

    public Page<Project> findAll(Pageable pageable) {
        return projectRepository.findAll(pageable);
    }

    public Page<Project> findByStatus(ProjectStatus status, Pageable pageable) {
        return projectRepository.findByStatus(status, pageable);
    }

    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    public Project save(Project project) {
        boolean isNew = project.getId() == null;
        Project savedProject = projectRepository.save(project);

        // Create audit log with String-based payload
        Map<String, String> payload = createProjectStringPayload(savedProject);
        String actionType = isNew ? "CREATE" : "UPDATE";
        auditLogRepository.save(new AuditLog(actionType, "Project",
                savedProject.getId().toString(), "system", payload));

        log.info("Project {} successfully: {}", actionType.toLowerCase(), savedProject.getName());
        return savedProject;
    }

    @Transactional
    @CacheEvict(value = "projects", key = "#id")
    public void deleteById(Long id) {
        Optional<Project> project = projectRepository.findById(id);
        if (project.isPresent()) {
            projectRepository.deleteById(id);

            // Create audit log with String-based payload
            Map<String, String> payload = createProjectStringPayload(project.get());
            auditLogRepository.save(new AuditLog("DELETE", "Project",
                    id.toString(), "system", payload));

            log.info("Project deleted successfully: {}", project.get().getName());
        }
    }

    public List<Project> findProjectsWithoutTasks() {
        return projectRepository.findProjectsWithoutTasks();
    }

    public List<Project> findOverdueProjects() {
        return projectRepository.findOverdueProjects(LocalDate.now());
    }

    @Transactional
    public Project updateStatus(Long id, ProjectStatus status) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        project.setStatus(status);
        return save(project);
    }

    /**
     * Creates a payload map for AuditLog, ensuring all values are strings.
     * This method is crucial for compatibility with AuditLog's Map<String, String> payload.
     * Converts various object types to String for consistent audit logging.
     * @param project The Project object from which to create the payload.
     * @return A Map<String, String> representing the project's data.
     */
    private Map<String, String> createProjectStringPayload(Project project) {
        Map<String, String> payload = new HashMap<>();
        payload.put("id", project.getId() != null ? project.getId().toString() : null);
        payload.put("name", project.getName());
        payload.put("description", project.getDescription());
        payload.put("deadline", project.getDeadline() != null ? project.getDeadline().toString() : null); // Convert LocalDate to String
        payload.put("status", project.getStatus() != null ? project.getStatus().toString() : null); // Convert Enum to String
        payload.put("taskCount", String.valueOf(project.getTasks().size())); // Convert int to String
        // Add other project properties as needed, converting to String
        return payload;
    }
}
