-- ===== 1. Mở rộng bảng organization_profiles =====
ALTER TABLE organization_profiles
    ADD COLUMN org_type VARCHAR(50) NOT NULL DEFAULT 'PRIVATE_RESCUE'
        COMMENT 'PUBLIC_SHELTER | PRIVATE_RESCUE | VET_CLINIC | INDEPENDENT_FOSTER',
    ADD COLUMN verification_level VARCHAR(10) NOT NULL DEFAULT 'FULL'
        COMMENT 'FULL (tổ chức) | LITE (cá nhân)',
    ADD COLUMN founded_year INT,
    ADD COLUMN business_address VARCHAR(500) COMMENT 'Địa chỉ đăng ký kinh doanh (tổ chức)',
    ADD COLUMN tax_code VARCHAR(50) COMMENT 'Mã số thuế / mã số ĐKKD (tổ chức)',
    ADD COLUMN establishment_number VARCHAR(100) COMMENT 'Số QĐ thành lập (phi lợi nhuận)',
    ADD COLUMN website VARCHAR(500),
    ADD COLUMN fanpage VARCHAR(500),
    ADD COLUMN email VARCHAR(255),
    ADD COLUMN phone VARCHAR(20),
    ADD COLUMN representative_name VARCHAR(255) COMMENT 'Họ tên người đại diện',
    ADD COLUMN representative_id_number VARCHAR(20) COMMENT 'Số CMND/CCCD',
    ADD COLUMN representative_role VARCHAR(50) COMMENT 'OWNER | MANAGER | VOLUNTEER',
    ADD COLUMN sterilization_policy BOOLEAN DEFAULT FALSE
        COMMENT 'Cam kết triệt sản trước nhận nuôi',
    ADD COLUMN vaccination_policy BOOLEAN DEFAULT FALSE
        COMMENT 'Cam kết tiêm phòng trước nhận nuôi',
    ADD COLUMN policy_description TEXT COMMENT 'Mô tả chi tiết chính sách',
    ADD COLUMN admin_note TEXT COMMENT 'Ghi chú admin khi duyệt/từ chối',
    ADD COLUMN rejection_reason TEXT,
    ADD COLUMN verified_at TIMESTAMP NULL,
    ADD COLUMN verified_until TIMESTAMP NULL COMMENT 'Hạn verified badge',
    ADD COLUMN agreed_terms BOOLEAN DEFAULT FALSE,
    ADD COLUMN agreed_terms_at TIMESTAMP NULL;

-- Mở rộng status VARCHAR (PENDING, NEEDS_SUPPLEMENT, APPROVED, REJECTED)
ALTER TABLE organization_profiles MODIFY COLUMN status VARCHAR(30) DEFAULT 'PENDING';

-- ===== 2. Bảng documents upload =====
CREATE TABLE organization_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    org_id BIGINT NOT NULL,
    doc_type VARCHAR(50) NOT NULL
        COMMENT 'BUSINESS_LICENSE | ESTABLISHMENT_DECISION | VET_COOPERATION | FACILITY_PHOTO | ID_CARD | LIVING_SPACE_PHOTO | OTHER',
    file_url VARCHAR(1000) NOT NULL,
    file_name VARCHAR(255),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_orgdoc_org FOREIGN KEY (org_id)
        REFERENCES organization_profiles(id) ON DELETE CASCADE
);
CREATE INDEX idx_orgdoc_org_id ON organization_documents(org_id);

-- ===== 3. Bảng thành viên tổ chức =====
CREATE TABLE organization_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    org_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    member_role VARCHAR(30) NOT NULL DEFAULT 'COLLABORATOR'
        COMMENT 'OWNER | MANAGER | COLLABORATOR',
    invited_by BIGINT,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_orgmember_org FOREIGN KEY (org_id)
        REFERENCES organization_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_orgmember_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_org_user UNIQUE (org_id, user_id)
);
CREATE INDEX idx_orgmember_user ON organization_members(user_id);
CREATE INDEX idx_orgmember_org ON organization_members(org_id);

-- ===== 4. Bảng config nền tảng =====
CREATE TABLE platform_config (
    config_key VARCHAR(100) PRIMARY KEY,
    config_value VARCHAR(500) NOT NULL,
    description VARCHAR(500),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Seed SLA config
INSERT INTO platform_config (config_key, config_value, description) VALUES
    ('org.sla_acknowledgment_hours', '24', 'Mốc 1: Xác nhận tiếp nhận hồ sơ (giờ)'),
    ('org.sla_decision_hours', '72', 'Mốc 2: Quyết định duyệt/từ chối cuối cùng (giờ)'),
    ('org.verified_duration_days', '365', 'Thời hạn verified badge (ngày)'),
    ('org.renewal_reminder_days', '30', 'Nhắc gia hạn trước bao nhiêu ngày');

-- ===== 5. Thêm cột report tổ chức vào bảng reports =====
ALTER TABLE reports ADD COLUMN reported_org_id BIGINT;
ALTER TABLE reports ADD CONSTRAINT fk_report_org
    FOREIGN KEY (reported_org_id) REFERENCES organization_profiles(id) ON DELETE SET NULL;
