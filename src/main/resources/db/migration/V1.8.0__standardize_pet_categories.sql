-- Chuẩn hóa danh mục thú cưng
-- Gom PARROT + BIRD -> BIRDS
UPDATE pets SET category = 'BIRDS' WHERE category IN ('PARROT', 'BIRD');

-- RABBIT -> RABBITS (số nhiều cho nhất quán)
UPDATE pets SET category = 'RABBITS' WHERE category = 'RABBIT';

-- Giữ nguyên DOGS, CATS, FISH, POULTRY
-- Thêm danh mục OTHER cho các loài chưa phân loại (nếu có)
