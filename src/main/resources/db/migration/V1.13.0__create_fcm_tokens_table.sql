CREATE TABLE fcm_tokens (
    token VARCHAR(255) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    updated_at DATETIME,
    CONSTRAINT fk_fcm_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
