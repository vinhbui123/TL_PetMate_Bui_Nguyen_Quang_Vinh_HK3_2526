ALTER TABLE reports ADD COLUMN reported_comment_id BIGINT;
ALTER TABLE reports ADD COLUMN reported_message_id BIGINT;

ALTER TABLE reports ADD CONSTRAINT fk_report_comment FOREIGN KEY (reported_comment_id) REFERENCES pet_comments(id);
ALTER TABLE reports ADD CONSTRAINT fk_report_message FOREIGN KEY (reported_message_id) REFERENCES chat_messages(id);
