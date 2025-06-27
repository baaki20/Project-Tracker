package com.buildmaster.projecttracker.repository;

import com.buildmaster.projecttracker.audit.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    Page<AuditLog> findByActionType(String actionType, Pageable pageable);

    Page<AuditLog> findByEntityType(String entityType, Pageable pageable);

    Page<AuditLog> findByUserId(String userId, Pageable pageable);

    Page<AuditLog> findByUsername(String username, Pageable pageable);

    Page<AuditLog> findBySuccessFalse(Pageable pageable);

    Page<AuditLog> findByTimestampBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    List<AuditLog> findByActionTypeAndSuccessFalseAndTimestampAfter(String actionType, LocalDateTime since);

    List<AuditLog> findByIpAddressAndSuccessFalseAndTimestampAfter(String ipAddress, LocalDateTime since);
}