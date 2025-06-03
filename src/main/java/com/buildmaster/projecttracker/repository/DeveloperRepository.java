package com.buildmaster.projecttracker.repository;

import com.buildmaster.projecttracker.entity.Developer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeveloperRepository extends JpaRepository<Developer, Long> {

    Optional<Developer> findByEmail(String email);

    Page<Developer> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("SELECT d FROM Developer d ORDER BY SIZE(d.tasks) DESC")
    List<Developer> findTop5DevelopersByTaskCount(Pageable pageable);

    @Query("SELECT COUNT(t) FROM Developer d JOIN d.tasks t WHERE d.id = :developerId")
    Long countTasksByDeveloperId(Long developerId);
}
