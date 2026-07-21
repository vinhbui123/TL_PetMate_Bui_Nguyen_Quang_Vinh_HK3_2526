ALTER TABLE users ADD COLUMN average_rating DOUBLE DEFAULT 0.0;
ALTER TABLE users ADD COLUMN rating_count INT DEFAULT 0;

CREATE TABLE user_ratings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rater_id BIGINT NOT NULL,
    rated_user_id BIGINT NOT NULL,
    score DOUBLE NOT NULL,
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_rater_rated (rater_id, rated_user_id),
    FOREIGN KEY (rater_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (rated_user_id) REFERENCES users(id) ON DELETE CASCADE
);
