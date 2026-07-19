CREATE TABLE adoption_applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pet_id BIGINT NOT NULL,
    applicant_id BIGINT NOT NULL,
    message TEXT,
    experience TEXT,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_adoption_pet FOREIGN KEY (pet_id) REFERENCES pets(id) ON DELETE CASCADE,
    CONSTRAINT fk_adoption_applicant FOREIGN KEY (applicant_id) REFERENCES users(id) ON DELETE CASCADE
);
