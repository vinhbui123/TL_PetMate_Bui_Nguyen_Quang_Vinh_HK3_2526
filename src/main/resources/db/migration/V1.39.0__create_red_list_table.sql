CREATE TABLE red_list_species (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(50),
    breed_keyword VARCHAR(200) NOT NULL,
    synonyms TEXT,
    protection_level VARCHAR(20) NOT NULL DEFAULT 'RESTRICTED',
    description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_red_list_cat_breed (category, breed_keyword)
);

ALTER TABLE pets ADD COLUMN red_list_note VARCHAR(500) NULL;
