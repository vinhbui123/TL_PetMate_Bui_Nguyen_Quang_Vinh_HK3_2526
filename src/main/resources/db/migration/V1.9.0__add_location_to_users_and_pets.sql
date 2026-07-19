-- Thêm toạ độ GPS cho users (vị trí người dùng)
ALTER TABLE users ADD COLUMN latitude DOUBLE NULL;
ALTER TABLE users ADD COLUMN longitude DOUBLE NULL;

-- Thêm toạ độ GPS cho pets (vị trí bài đăng)
ALTER TABLE pets ADD COLUMN latitude DOUBLE NULL;
ALTER TABLE pets ADD COLUMN longitude DOUBLE NULL;
