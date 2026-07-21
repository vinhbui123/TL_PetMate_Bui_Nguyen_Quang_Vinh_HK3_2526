ALTER TABLE reports DROP FOREIGN KEY fk_report_comment;
ALTER TABLE reports DROP COLUMN reported_comment_id;
DROP TABLE pet_comments;
RENAME TABLE pet_likes TO saved_pets;
