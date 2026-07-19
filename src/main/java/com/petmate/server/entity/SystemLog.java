package com.petmate.server.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String actionType; // e.g. "BROADCAST_NOTIFICATION", "INVALID_TOKEN"
    
    private String actor; // UID người thực hiện hoặc "SYSTEM"
    
    private String severity; // INFO | WARN | ERROR
    
    private String description;
    
    private LocalDateTime timestamp;
}
