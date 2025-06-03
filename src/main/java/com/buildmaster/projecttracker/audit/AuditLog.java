package com.buildmaster.projecttracker.audit;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "audit_logs")
@Data
@NoArgsConstructor
public class AuditLog {

    @Id
    private String id;

    private String actionType; // CREATE, UPDATE, DELETE
    private String entityType; // Project, Task, Developer
    private String entityId;
    private LocalDateTime timestamp;
    private String actorName;
    private Map<String, Object> payload; // JSON representation of the entity

    public AuditLog(String actionType, String entityType, String entityId, String actorName, Map<String, Object> payload) {
        this.actionType = actionType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.actorName = actorName;
        this.payload = payload;
        this.timestamp = LocalDateTime.now();
    }
}