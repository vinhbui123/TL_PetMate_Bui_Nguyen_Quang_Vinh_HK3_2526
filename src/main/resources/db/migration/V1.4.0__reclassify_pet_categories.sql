-- Phân loại lại các pet đang bị gán category = 'ALL' dựa trên tên (name)

-- Chó
UPDATE pets SET category = 'DOGS' WHERE category = 'ALL'
  AND (LOWER(name) LIKE '%chó%' OR LOWER(name) LIKE '%cún%' OR LOWER(name) LIKE '%cẩu%'
    OR LOWER(name) LIKE '%poodle%' OR LOWER(name) LIKE '%corgi%' OR LOWER(name) LIKE '%husky%'
    OR LOWER(name) LIKE '%golden%' OR LOWER(name) LIKE '%phốc%' OR LOWER(name) LIKE '%pomeranian%'
    OR LOWER(name) LIKE '%chihuahua%' OR LOWER(name) LIKE '%beagle%' OR LOWER(name) LIKE '%shiba%'
    OR LOWER(name) LIKE '%alaska%' OR LOWER(name) LIKE '%bull%' OR LOWER(name) LIKE '%pitbull%'
    OR LOWER(name) LIKE '%phú quốc%' OR LOWER(name) LIKE '%becgie%' OR LOWER(name) LIKE '%rottweiler%'
    OR LOWER(name) LIKE '%labrador%' OR LOWER(name) LIKE '%samoyed%' OR LOWER(name) LIKE '%pug%'
    OR LOWER(name) LIKE '%puppy%' OR LOWER(name) LIKE '%dog%'
    OR LOWER(description) LIKE '%chó%' OR LOWER(description) LIKE '%cún%'
    OR LOWER(description) LIKE '%puppy%');

-- Mèo
UPDATE pets SET category = 'CATS' WHERE category = 'ALL'
  AND (LOWER(name) LIKE '%mèo%' OR LOWER(name) LIKE '%cat%' OR LOWER(name) LIKE '%kitten%'
    OR LOWER(name) LIKE '%aln%' OR LOWER(name) LIKE '%anh lông ngắn%'
    OR LOWER(name) LIKE '%ba tư%' OR LOWER(name) LIKE '%persian%'
    OR LOWER(name) LIKE '%scottish%' OR LOWER(name) LIKE '%munchkin%'
    OR LOWER(name) LIKE '%ragdoll%' OR LOWER(name) LIKE '%bengal%'
    OR LOWER(name) LIKE '%british%' OR LOWER(name) LIKE '%tai cụp%'
    OR LOWER(description) LIKE '%mèo%' OR LOWER(description) LIKE '%cat%');

-- Chim
UPDATE pets SET category = 'PARROT' WHERE category = 'ALL'
  AND (LOWER(name) LIKE '%chim%' OR LOWER(name) LIKE '%vẹt%' OR LOWER(name) LIKE '%yến%'
    OR LOWER(name) LIKE '%chào mào%' OR LOWER(name) LIKE '%sáo%'
    OR LOWER(name) LIKE '%bồ câu%' OR LOWER(name) LIKE '%cu gáy%'
    OR LOWER(name) LIKE '%gà%' OR LOWER(name) LIKE '%parrot%'
    OR LOWER(description) LIKE '%chim%' OR LOWER(description) LIKE '%vẹt%');

-- Thú cưng khác
UPDATE pets SET category = 'RABBIT' WHERE category = 'ALL'
  AND (LOWER(name) LIKE '%thỏ%' OR LOWER(name) LIKE '%hamster%' OR LOWER(name) LIKE '%chuột%'
    OR LOWER(name) LIKE '%rùa%' OR LOWER(name) LIKE '%rắn%' OR LOWER(name) LIKE '%rồng%'
    OR LOWER(name) LIKE '%sóc%' OR LOWER(name) LIKE '%nhím%'
    OR LOWER(name) LIKE '%cá%' OR LOWER(name) LIKE '%lồng%'
    OR LOWER(description) LIKE '%thỏ%' OR LOWER(description) LIKE '%hamster%');

-- Phần còn lại mặc định về DOGS
UPDATE pets SET category = 'DOGS' WHERE category = 'ALL';
