ALTER TABLE user_ratings ADD COLUMN pet_id BIGINT;
ALTER TABLE user_ratings ADD CONSTRAINT fk_user_ratings_pet FOREIGN KEY (pet_id) REFERENCES pets(id) ON DELETE SET NULL;
