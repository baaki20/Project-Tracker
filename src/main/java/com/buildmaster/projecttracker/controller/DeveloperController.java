package com.buildmaster.projecttracker.controller;

import com.buildmaster.projecttracker.entity.Developer;
import com.buildmaster.projecttracker.service.DeveloperService;
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
@RequestMapping("/api/v1/developers")
@RequiredArgsConstructor
public class DeveloperController {

    private final DeveloperService developerService;

    @GetMapping
    public ResponseEntity<Page<Developer>> getAllDevelopers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Developer> developers = developerService.findAll(pageable);

        return ResponseEntity.ok(developers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Developer> getDeveloperById(@PathVariable Long id) {
        return developerService.findById(id)
                .map(developer -> ResponseEntity.ok(developer))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<Developer> getDeveloperByEmail(@PathVariable String email) {
        return developerService.findByEmail(email)
                .map(developer -> ResponseEntity.ok(developer))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<Page<Developer>> searchDevelopersByName(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Developer> developers = developerService.searchByName(name, pageable);

        return ResponseEntity.ok(developers);
    }

    @PostMapping
    public ResponseEntity<?> createDeveloper(@Valid @RequestBody Developer developer) {
        try {
            Developer savedDeveloper = developerService.save(developer);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedDeveloper);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDeveloper(@PathVariable Long id,
                                             @Valid @RequestBody Developer developer) {
        return developerService.findById(id)
                .map(existingDeveloper -> {
                    try {
                        developer.setId(id);
                        Developer updatedDeveloper = developerService.save(developer);
                        return ResponseEntity.ok(updatedDeveloper);
                    } catch (RuntimeException e) {
                        Map<String, String> error = new HashMap<>();
                        error.put("error", e.getMessage());
                        return ResponseEntity.badRequest().body(error);
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDeveloper(@PathVariable Long id) {
        return developerService.findById(id)
                .map(developer -> {
                    try {
                        developerService.deleteById(id);
                        return ResponseEntity.noContent().build();
                    } catch (RuntimeException e) {
                        Map<String, String> error = new HashMap<>();
                        error.put("error", e.getMessage());
                        return ResponseEntity.badRequest().body(error);
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/top-performers")
    public ResponseEntity<List<Developer>> getTopDevelopers(
            @RequestParam(defaultValue = "5") int limit) {
        List<Developer> topDevelopers = developerService.findTopDevelopersByTaskCount(limit);
        return ResponseEntity.ok(topDevelopers);
    }

    @GetMapping("/{id}/task-count")
    public ResponseEntity<Map<String, Object>> getDeveloperTaskCount(@PathVariable Long id) {
        return developerService.findById(id)
                .map(developer -> {
                    Long taskCount = developerService.getTaskCountForDeveloper(id);
                    Map<String, Object> response = new HashMap<>();
                    response.put("developerId", id);
                    response.put("developerName", developer.getName());
                    response.put("taskCount", taskCount);
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/skills")
    public ResponseEntity<?> updateDeveloperSkills(@PathVariable Long id,
                                                   @RequestBody Map<String, String> request) {
        try {
            String skills = request.get("skills");
            if (skills == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Skills field is required");
                return ResponseEntity.badRequest().body(error);
            }

            Developer updatedDeveloper = developerService.updateSkills(id, skills);
            return ResponseEntity.ok(updatedDeveloper);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Boolean>> checkEmailExists(@RequestParam String email) {
        boolean exists = developerService.existsByEmail(email);
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }
}