package com.buildmaster.projecttracker.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    private String id;

    private String actionType;
    private String entityType;
    private String entityId;
    private String userId;
    private String username;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime timestamp;
    private Boolean success = true;
    private String errorMessage;
    private Map<String, String> payload;

    public AuditLog(String actionType, String entityType, String entityId, String userId, Map<String, String> payload) {
        this.actionType = actionType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.userId = userId;
        this.payload = payload;
        this.timestamp = LocalDateTime.now();
    }

    public AuditLog(String actionType, String entityType, String entityId, String userId, String username,
                    String ipAddress, String userAgent, Boolean success, String errorMessage) {
        this.actionType = actionType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.userId = userId;
        this.username = username;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.success = success;
        this.errorMessage = errorMessage;
        this.timestamp = LocalDateTime.now();
    }
}
