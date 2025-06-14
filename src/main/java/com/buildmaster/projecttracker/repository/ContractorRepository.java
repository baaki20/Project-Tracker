package com.buildmaster.projecttracker.repository;

import com.buildmaster.projecttracker.entity.Contractor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractorRepository extends JpaRepository<Contractor, Long> {
    Contractor findByUserId(Long userId);

    void deleteByUserId(Long id);
}
