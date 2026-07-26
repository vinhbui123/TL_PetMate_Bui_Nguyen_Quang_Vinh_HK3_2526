UPDATE pets SET weight = REPLACE(LOWER(weight), 'kg', '');
UPDATE pets SET weight = TRIM(weight);
UPDATE pets SET weight = NULL WHERE weight = '';
ALTER TABLE pets MODIFY COLUMN weight DOUBLE;
