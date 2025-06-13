package com.buildmaster.projecttracker.repository;

import com.buildmaster.projecttracker.entity.Manager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ManagerRepository extends JpaRepository<Manager, Long> {
    Manager findByUserId(Long userId);
}
