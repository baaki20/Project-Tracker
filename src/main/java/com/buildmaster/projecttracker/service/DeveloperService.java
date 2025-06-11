package com.buildmaster.projecttracker.service;

    import com.buildmaster.projecttracker.audit.AuditLog;
    import com.buildmaster.projecttracker.repository.AuditLogRepository;
    import com.buildmaster.projecttracker.entity.Developer;
    import com.buildmaster.projecttracker.repository.DeveloperRepository;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.cache.annotation.CacheEvict;
    import org.springframework.cache.annotation.Cacheable;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.PageRequest;
    import org.springframework.data.domain.Pageable;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;
    import java.util.Optional;

    @Service
    @RequiredArgsConstructor
    @Slf4j
    public class DeveloperService {

        private final DeveloperRepository developerRepository;
        private final AuditLogRepository auditLogRepository;

        @Cacheable(value = "developers", key = "#id")
        public Optional<Developer> findById(Long id) {
            log.debug("Fetching developer with id: {}", id);
            return developerRepository.findById(id);
        }

        public Page<Developer> findAll(Pageable pageable) {
            return developerRepository.findAll(pageable);
        }

        public Optional<Developer> findByEmail(String email) {
            return developerRepository.findByEmail(email);
        }

        public Page<Developer> searchByName(String name, Pageable pageable) {
            return developerRepository.findByNameContainingIgnoreCase(name, pageable);
        }

        @Transactional
        @CacheEvict(value = "developers", allEntries = true)
        public Developer save(Developer developer) {
            boolean isNew = developer.getId() == null;

            if (isNew && findByEmail(developer.getEmail()).isPresent()) {
                throw new RuntimeException("Developer with email " + developer.getEmail() + " already exists");
            }

            Developer savedDeveloper = developerRepository.save(developer);

            Map<String, String> payload = createDeveloperPayload(savedDeveloper);
            String actionType = isNew ? "CREATE" : "UPDATE";
            auditLogRepository.save(new AuditLog(actionType, "Developer",
                    savedDeveloper.getId().toString(), "system", payload));

            log.info("Developer {} successfully: {}", actionType.toLowerCase(), savedDeveloper.getName());
            return savedDeveloper;
        }

        @Transactional
        @CacheEvict(value = "developers", key = "#id")
        public void deleteById(Long id) {
            Optional<Developer> developer = developerRepository.findById(id);
            if (developer.isPresent()) {
                if (!developer.get().getTasks().isEmpty()) {
                    throw new RuntimeException("Cannot delete developer with assigned tasks. Please reassign tasks first.");
                }

                developerRepository.deleteById(id);

                Map<String, String> payload = createDeveloperPayload(developer.get());
                auditLogRepository.save(new AuditLog("DELETE", "Developer",
                        id.toString(), "system", payload));

                log.info("Developer deleted successfully: {}", developer.get().getName());
            }
        }

        public List<Developer> findTopDevelopersByTaskCount(int limit) {
            Pageable pageable = PageRequest.of(0, limit);
            return developerRepository.findTop5DevelopersByTaskCount(pageable);
        }

        public Long getTaskCountForDeveloper(Long developerId) {
            return developerRepository.countTasksByDeveloperId(developerId);
        }

        public boolean existsByEmail(String email) {
            return developerRepository.findByEmail(email).isPresent();
        }

        @Transactional
        public Developer updateSkills(Long id, String skills) {
            Developer developer = developerRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Developer not found"));

            developer.setSkills(skills);
            return save(developer);
        }

        private Map<String, String> createDeveloperPayload(Developer developer) {
            Map<String, String> payload = new HashMap<>();
            payload.put("id", developer.getId().toString());
            payload.put("name", developer.getName());
            payload.put("email", developer.getEmail());
            payload.put("skills", developer.getSkills());
            payload.put("taskCount", String.valueOf(developer.getTasks().size()));
            payload.put("createdAt", developer.getCreatedAt().toString());
            payload.put("updatedAt", developer.getUpdatedAt().toString());
            return payload;
        }
    }