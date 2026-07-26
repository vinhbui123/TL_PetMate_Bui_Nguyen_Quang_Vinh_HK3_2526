ALTER TABLE pets ADD COLUMN organization_id BIGINT;
ALTER TABLE pets ADD CONSTRAINT fk_pet_organization FOREIGN KEY (organization_id) REFERENCES organization_profiles(id) ON DELETE SET NULL;
CREATE INDEX idx_pets_organization_id ON pets(organization_id);
