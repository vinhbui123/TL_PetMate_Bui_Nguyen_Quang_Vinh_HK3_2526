-- Indexes to improve query performance for listing and filtering pets
CREATE INDEX idx_pets_status ON pets(status);
CREATE INDEX idx_pets_category_status ON pets(category, status);
CREATE INDEX idx_pets_user_id ON pets(user_id);
