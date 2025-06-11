package com.buildmaster.projecttracker.audit;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", length = 50)
    private String entityId;

    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "success", nullable = false)
    private Boolean success = true;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /**
     * Stores additional data as key-value pairs in a separate table.
     * The `property_value` column is explicitly defined as TEXT to resolve
     * Hibernate's inability to determine the SQL type for Map values.
     */
    @ElementCollection
    @CollectionTable(name = "audit_log_payload", joinColumns = @JoinColumn(name = "audit_log_id"))
    @MapKeyColumn(name = "property_key", length = 255) // Added length for property_key for consistency
    @Column(name = "property_value", length = 1000, columnDefinition = "TEXT") // Explicitly define as TEXT
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

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
