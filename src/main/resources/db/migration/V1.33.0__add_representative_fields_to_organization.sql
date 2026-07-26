ALTER TABLE organization_profiles 
ADD COLUMN representative_phone VARCHAR(20),
ADD COLUMN representative_email VARCHAR(255),
ADD COLUMN representative_id_type VARCHAR(20),
ADD COLUMN representative_id_front_url VARCHAR(1000),
ADD COLUMN representative_avatar_url VARCHAR(1000),
ADD COLUMN representative_social_url VARCHAR(1000),
ADD COLUMN rep_last_verified_at DATETIME(6),
MODIFY COLUMN representative_id_number VARCHAR(50);
