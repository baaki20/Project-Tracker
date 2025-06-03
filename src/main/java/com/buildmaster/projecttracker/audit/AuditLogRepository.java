package com.buildmaster.projecttracker.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    Page<AuditLog> findByEntityType(String entityType, Pageable pageable);

    Page<AuditLog> findByActorName(String actorName, Pageable pageable);

    List<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId, Pageable pageable);
}