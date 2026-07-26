package com.petmate.server.service;

import com.petmate.server.entity.SystemLog;
import com.petmate.server.repository.SystemLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemLogService {

    private final SystemLogRepository systemLogRepository;

    public void info(String actionType, String actor, String description) {
        save(actionType, actor, description, "INFO");
    }

    public void warn(String actionType, String actor, String description) {
        save(actionType, actor, description, "WARN");
    }

    public void error(String actionType, String actor, String description) {
        save(actionType, actor, description, "ERROR");
    }

    private void save(String actionType, String actor, String description, String severity) {
        try {
            SystemLog entry = SystemLog.builder()
                    .actionType(actionType)
                    .actor(actor != null ? actor : "UNKNOWN")
                    .description(description)
                    .severity(severity)
                    .timestamp(LocalDateTime.now())
                    .build();
            systemLogRepository.save(entry);
        } catch (Exception e) {
            // TrÃ¡nh log lá»—i gÃ¢y vÃ²ng láº·p vÃ´ táº­n
            log.error("[SystemLogService] Failed to save log: {}", e.getMessage());
        }
    }

    public java.util.List<SystemLog> getRecentLogs(int limit) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        return systemLogRepository.findAll(pageable).getContent();
    }
}
