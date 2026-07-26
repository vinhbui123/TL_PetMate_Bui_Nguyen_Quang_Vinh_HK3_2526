ALTER TABLE pets
    ADD COLUMN age_months INT NULL;

UPDATE pets
    SET age_months = CAST(age AS UNSIGNED)
    WHERE age REGEXP '^[0-9]+$';
