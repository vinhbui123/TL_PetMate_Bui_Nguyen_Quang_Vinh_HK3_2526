package com.petmate.server.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_logs", indexes = {
    @Index(name = "idx_system_logs_action_type", columnList = "action_type"),
    @Index(name = "idx_system_logs_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action_type", length = 100)
    private String actionType; // e.g. "BROADCAST_NOTIFICATION", "INVALID_TOKEN"

    @Column(name = "actor", length = 100)
    private String actor; // UID ngÆ°á»i thá»±c hiá»‡n hoáº·c "SYSTEM"

    @Column(name = "severity", length = 10)
    private String severity; // INFO | WARN | ERROR

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Helper for backwards compatibility with SystemLogService.builder().timestamp(...)
    public static class SystemLogBuilder {
        public SystemLogBuilder timestamp(LocalDateTime timestamp) {
            this.createdAt = timestamp;
            return this;
        }
    }
}
