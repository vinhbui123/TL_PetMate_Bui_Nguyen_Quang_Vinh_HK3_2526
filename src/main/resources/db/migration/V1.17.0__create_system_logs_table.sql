CREATE TABLE IF NOT EXISTS system_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    action_type VARCHAR(255) NOT NULL,
    actor VARCHAR(255),
    severity VARCHAR(20) NOT NULL DEFAULT 'INFO',
    description TEXT,
    timestamp DATETIME NOT NULL
);
