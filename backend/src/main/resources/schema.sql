CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    account VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(20) DEFAULT NULL,
    email VARCHAR(128) DEFAULT NULL,
    nickname VARCHAR(64) DEFAULT NULL,
    avatar_url VARCHAR(255) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    last_login_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_account (account),
    UNIQUE KEY uk_sys_user_phone (phone),
    UNIQUE KEY uk_sys_user_email (email),
    KEY idx_sys_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    role_code VARCHAR(32) NOT NULL,
    role_name VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    role_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_sur_user_id (user_id),
    KEY idx_sur_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS jobseeker_profile (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    real_name VARCHAR(64) NOT NULL,
    gender VARCHAR(16) DEFAULT NULL,
    birth_year SMALLINT DEFAULT NULL,
    school_name VARCHAR(128) DEFAULT NULL,
    major VARCHAR(128) DEFAULT NULL,
    degree VARCHAR(32) DEFAULT NULL,
    graduation_year SMALLINT DEFAULT NULL,
    current_city VARCHAR(64) DEFAULT NULL,
    target_city VARCHAR(64) DEFAULT NULL,
    expected_job VARCHAR(128) DEFAULT NULL,
    expected_salary_min DECIMAL(10, 2) DEFAULT NULL,
    expected_salary_max DECIMAL(10, 2) DEFAULT NULL,
    work_mode_preference VARCHAR(32) DEFAULT NULL,
    intro TEXT DEFAULT NULL,
    profile_completion_rate TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_jobseeker_user_id (user_id),
    KEY idx_jobseeker_target_city (target_city),
    KEY idx_jobseeker_expected_job (expected_job)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS jobseeker_support_need (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    support_visibility VARCHAR(32) NOT NULL DEFAULT 'HIDDEN',
    text_communication_preferred TINYINT NOT NULL DEFAULT 0,
    subtitle_needed TINYINT NOT NULL DEFAULT 0,
    remote_interview_preferred TINYINT NOT NULL DEFAULT 0,
    keyboard_only_mode TINYINT NOT NULL DEFAULT 0,
    high_contrast_needed TINYINT NOT NULL DEFAULT 0,
    large_font_needed TINYINT NOT NULL DEFAULT 0,
    flexible_schedule_needed TINYINT NOT NULL DEFAULT 0,
    accessible_workspace_needed TINYINT NOT NULL DEFAULT 0,
    assistive_software_needed TINYINT NOT NULL DEFAULT 0,
    remark VARCHAR(500) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_support_need_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS jobseeker_sensitive_info (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    disability_type_ciphertext TEXT DEFAULT NULL,
    disability_level_ciphertext TEXT DEFAULT NULL,
    support_need_detail_ciphertext TEXT DEFAULT NULL,
    health_note_ciphertext TEXT DEFAULT NULL,
    emergency_contact_name_ciphertext TEXT DEFAULT NULL,
    emergency_contact_phone_ciphertext TEXT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_jobseeker_sensitive_info_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS jobseeker_service_authorization (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    jobseeker_user_id BIGINT UNSIGNED NOT NULL,
    service_org_user_id BIGINT UNSIGNED NOT NULL,
    profile_access_granted TINYINT NOT NULL DEFAULT 1,
    sensitive_access_granted TINYINT NOT NULL DEFAULT 0,
    granted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_jobseeker_service_authorization_pair (jobseeker_user_id, service_org_user_id),
    KEY idx_jobseeker_service_authorization_jobseeker (jobseeker_user_id),
    KEY idx_jobseeker_service_authorization_service_org (service_org_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @jobseeker_support_need_visibility_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'jobseeker_support_need'
      AND COLUMN_NAME = 'support_visibility'
);
SET @jobseeker_support_need_visibility_sql := IF(
    @jobseeker_support_need_visibility_exists = 0,
    'ALTER TABLE jobseeker_support_need ADD COLUMN support_visibility VARCHAR(32) NOT NULL DEFAULT ''HIDDEN'' AFTER user_id',
    'SELECT 1'
);
PREPARE stmt FROM @jobseeker_support_need_visibility_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS jobseeker_skill (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    skill_code VARCHAR(64) NOT NULL,
    skill_name VARCHAR(64) NOT NULL,
    skill_level TINYINT NOT NULL DEFAULT 3,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_jobseeker_skill (user_id, skill_code, is_deleted),
    KEY idx_js_skill_code (skill_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS jobseeker_project (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    project_name VARCHAR(128) NOT NULL,
    role_name VARCHAR(64) DEFAULT NULL,
    description TEXT DEFAULT NULL,
    start_date DATE DEFAULT NULL,
    end_date DATE DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_project_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS catalog_job (
    id VARCHAR(64) NOT NULL,
    title VARCHAR(128) NOT NULL,
    company_name VARCHAR(128) NOT NULL,
    city VARCHAR(64) NOT NULL,
    salary_range VARCHAR(64) NOT NULL,
    work_mode VARCHAR(32) NOT NULL,
    summary TEXT NOT NULL,
    stage VARCHAR(32) NOT NULL,
    match_score INT NOT NULL,
    dimension_scores JSON NOT NULL,
    reasons JSON NOT NULL,
    risks JSON NOT NULL,
    supports JSON NOT NULL,
    description_items JSON NOT NULL,
    requirement_items JSON NOT NULL,
    environment_items JSON NOT NULL,
    apply_hint VARCHAR(255) NOT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_catalog_job_sort_no (sort_no),
    KEY idx_catalog_job_city (city),
    KEY idx_catalog_job_work_mode (work_mode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS enterprise_job_posting (
    job_id VARCHAR(64) NOT NULL,
    department VARCHAR(128) NOT NULL,
    salary_min INT NOT NULL,
    salary_max INT NOT NULL,
    headcount INT NOT NULL,
    description_text TEXT NOT NULL,
    requirement_text TEXT NOT NULL,
    deadline DATE DEFAULT NULL,
    interview_mode VARCHAR(32) NOT NULL,
    publish_status VARCHAR(32) NOT NULL,
    onsite_required TINYINT DEFAULT NULL,
    remote_supported TINYINT DEFAULT NULL,
    high_frequency_voice_required TINYINT DEFAULT NULL,
    noisy_environment TINYINT DEFAULT NULL,
    long_standing_required TINYINT DEFAULT NULL,
    text_material_supported TINYINT DEFAULT NULL,
    online_interview_supported TINYINT DEFAULT NULL,
    text_interview_supported TINYINT DEFAULT NULL,
    flexible_schedule_supported TINYINT DEFAULT NULL,
    accessible_workspace TINYINT DEFAULT NULL,
    assistive_software_supported TINYINT DEFAULT NULL,
    created_by_user_id BIGINT UNSIGNED DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (job_id),
    KEY idx_enterprise_job_posting_status (publish_status),
    KEY idx_enterprise_job_posting_deadline (deadline),
    KEY idx_enterprise_job_posting_created_by (created_by_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS enterprise_verification_profile (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    company_name VARCHAR(128) NOT NULL,
    industry VARCHAR(64) DEFAULT NULL,
    city VARCHAR(64) DEFAULT NULL,
    unified_social_credit_code VARCHAR(64) DEFAULT NULL,
    contact_name VARCHAR(64) DEFAULT NULL,
    contact_phone VARCHAR(32) DEFAULT NULL,
    office_address VARCHAR(255) DEFAULT NULL,
    accessibility_commitment TEXT DEFAULT NULL,
    verification_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    review_note VARCHAR(500) DEFAULT NULL,
    submitted_at DATETIME DEFAULT NULL,
    reviewed_at DATETIME DEFAULT NULL,
    reviewed_by_user_id BIGINT UNSIGNED DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_enterprise_verification_profile_user_id (user_id),
    KEY idx_enterprise_verification_profile_status (verification_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS enterprise_verification_material (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    material_type VARCHAR(32) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    storage_path VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) DEFAULT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    note VARCHAR(255) DEFAULT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_enterprise_verification_material_user_id (user_id),
    KEY idx_enterprise_verification_material_sort_no (sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS enterprise_verification_audit_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    action VARCHAR(32) NOT NULL,
    status_after VARCHAR(32) NOT NULL,
    operator_user_id BIGINT UNSIGNED DEFAULT NULL,
    operator_name VARCHAR(64) NOT NULL,
    content VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_enterprise_verification_audit_log_user_id (user_id),
    KEY idx_enterprise_verification_audit_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS job_application (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    job_id VARCHAR(64) NOT NULL,
    job_title VARCHAR(128) NOT NULL,
    company_name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    consent_to_share_support_need TINYINT NOT NULL DEFAULT 0,
    match_score_snapshot INT NOT NULL,
    explanation_snapshot JSON NOT NULL,
    submitted_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_job_application_user_job (user_id, job_id, is_deleted),
    KEY idx_job_application_user_id (user_id),
    KEY idx_job_application_job_id (job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS interview_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    application_id BIGINT UNSIGNED NOT NULL,
    interview_time DATETIME DEFAULT NULL,
    interview_mode VARCHAR(32) DEFAULT NULL,
    interviewer_name VARCHAR(64) DEFAULT NULL,
    invite_note VARCHAR(1000) DEFAULT NULL,
    result_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    feedback_note VARCHAR(1000) DEFAULT NULL,
    reject_reason VARCHAR(500) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_interview_application_id (application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @interview_record_invite_note_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'interview_record'
      AND COLUMN_NAME = 'invite_note'
);
SET @interview_record_invite_note_sql := IF(
    @interview_record_invite_note_exists = 0,
    'ALTER TABLE interview_record ADD COLUMN invite_note VARCHAR(1000) DEFAULT NULL AFTER interviewer_name',
    'SELECT 1'
);
PREPARE stmt FROM @interview_record_invite_note_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @interview_record_reject_reason_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'interview_record'
      AND COLUMN_NAME = 'reject_reason'
);
SET @interview_record_reject_reason_sql := IF(
    @interview_record_reject_reason_exists = 0,
    'ALTER TABLE interview_record ADD COLUMN reject_reason VARCHAR(500) DEFAULT NULL AFTER feedback_note',
    'SELECT 1'
);
PREPARE stmt FROM @interview_record_reject_reason_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS interview_support_request (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    application_id BIGINT UNSIGNED NOT NULL,
    request_type VARCHAR(32) NOT NULL,
    request_content VARCHAR(500) NOT NULL,
    request_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_interview_support_request_application_id (application_id),
    KEY idx_interview_support_request_status (request_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS catalog_candidate (
    id VARCHAR(64) NOT NULL,
    job_id VARCHAR(64) NOT NULL,
    name VARCHAR(64) NOT NULL,
    city VARCHAR(64) NOT NULL,
    expected_job VARCHAR(128) NOT NULL,
    work_mode VARCHAR(64) NOT NULL,
    match_score INT NOT NULL,
    stage VARCHAR(32) NOT NULL,
    consent_granted TINYINT NOT NULL DEFAULT 0,
    skills JSON NOT NULL,
    summary TEXT NOT NULL,
    risks JSON NOT NULL,
    support_summary JSON NOT NULL,
    suggestions JSON NOT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_catalog_candidate_job_id (job_id),
    KEY idx_catalog_candidate_sort_no (sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS catalog_enterprise_profile (
    code VARCHAR(64) NOT NULL,
    company_name VARCHAR(128) NOT NULL,
    industry VARCHAR(64) NOT NULL,
    city VARCHAR(64) NOT NULL,
    verification_status VARCHAR(32) NOT NULL,
    accessibility_commitment TEXT NOT NULL,
    published_job_count INT NOT NULL,
    open_candidate_count INT NOT NULL,
    interview_count INT NOT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (code),
    KEY idx_catalog_enterprise_profile_sort_no (sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS catalog_service_case (
    id VARCHAR(64) NOT NULL,
    user_id BIGINT UNSIGNED DEFAULT NULL,
    name VARCHAR(64) NOT NULL,
    stage VARCHAR(64) NOT NULL,
    owner_name VARCHAR(64) NOT NULL,
    next_action VARCHAR(255) NOT NULL,
    alert_level VARCHAR(32) NOT NULL,
    intake_note VARCHAR(500) DEFAULT NULL,
    profile_authorized TINYINT NOT NULL DEFAULT 0,
    authorization_note VARCHAR(255) DEFAULT NULL,
    authorization_updated_by VARCHAR(64) DEFAULT NULL,
    authorization_updated_at DATETIME DEFAULT NULL,
    timeline JSON NOT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_catalog_service_case_user_id (user_id),
    KEY idx_catalog_service_case_sort_no (sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @catalog_service_case_user_id_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'catalog_service_case'
      AND COLUMN_NAME = 'user_id'
);
SET @catalog_service_case_user_id_sql := IF(
    @catalog_service_case_user_id_exists = 0,
    'ALTER TABLE catalog_service_case ADD COLUMN user_id BIGINT UNSIGNED DEFAULT NULL AFTER id',
    'SELECT 1'
);
PREPARE stmt FROM @catalog_service_case_user_id_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @catalog_service_case_user_id_index_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'catalog_service_case'
      AND INDEX_NAME = 'idx_catalog_service_case_user_id'
);
SET @catalog_service_case_user_id_index_sql := IF(
    @catalog_service_case_user_id_index_exists = 0,
    'ALTER TABLE catalog_service_case ADD INDEX idx_catalog_service_case_user_id (user_id)',
    'SELECT 1'
);
PREPARE stmt FROM @catalog_service_case_user_id_index_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @catalog_service_case_intake_note_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'catalog_service_case'
      AND COLUMN_NAME = 'intake_note'
);
SET @catalog_service_case_intake_note_sql := IF(
    @catalog_service_case_intake_note_exists = 0,
    'ALTER TABLE catalog_service_case ADD COLUMN intake_note VARCHAR(500) DEFAULT NULL AFTER alert_level',
    'SELECT 1'
);
PREPARE stmt FROM @catalog_service_case_intake_note_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @catalog_service_case_profile_authorized_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'catalog_service_case'
      AND COLUMN_NAME = 'profile_authorized'
);
SET @catalog_service_case_profile_authorized_sql := IF(
    @catalog_service_case_profile_authorized_exists = 0,
    'ALTER TABLE catalog_service_case ADD COLUMN profile_authorized TINYINT NOT NULL DEFAULT 0 AFTER intake_note',
    'SELECT 1'
);
PREPARE stmt FROM @catalog_service_case_profile_authorized_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @catalog_service_case_authorization_note_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'catalog_service_case'
      AND COLUMN_NAME = 'authorization_note'
);
SET @catalog_service_case_authorization_note_sql := IF(
    @catalog_service_case_authorization_note_exists = 0,
    'ALTER TABLE catalog_service_case ADD COLUMN authorization_note VARCHAR(255) DEFAULT NULL AFTER profile_authorized',
    'SELECT 1'
);
PREPARE stmt FROM @catalog_service_case_authorization_note_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @catalog_service_case_authorization_updated_by_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'catalog_service_case'
      AND COLUMN_NAME = 'authorization_updated_by'
);
SET @catalog_service_case_authorization_updated_by_sql := IF(
    @catalog_service_case_authorization_updated_by_exists = 0,
    'ALTER TABLE catalog_service_case ADD COLUMN authorization_updated_by VARCHAR(64) DEFAULT NULL AFTER authorization_note',
    'SELECT 1'
);
PREPARE stmt FROM @catalog_service_case_authorization_updated_by_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @catalog_service_case_authorization_updated_at_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'catalog_service_case'
      AND COLUMN_NAME = 'authorization_updated_at'
);
SET @catalog_service_case_authorization_updated_at_sql := IF(
    @catalog_service_case_authorization_updated_at_exists = 0,
    'ALTER TABLE catalog_service_case ADD COLUMN authorization_updated_at DATETIME DEFAULT NULL AFTER authorization_updated_by',
    'SELECT 1'
);
PREPARE stmt FROM @catalog_service_case_authorization_updated_at_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @catalog_service_case_intake_note_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'catalog_service_case'
      AND COLUMN_NAME = 'intake_note'
);
SET @catalog_service_case_intake_note_sql := IF(
    @catalog_service_case_intake_note_exists = 0,
    'ALTER TABLE catalog_service_case ADD COLUMN intake_note VARCHAR(500) DEFAULT NULL AFTER alert_level',
    'SELECT 1'
);
PREPARE stmt FROM @catalog_service_case_intake_note_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @catalog_service_case_profile_authorized_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'catalog_service_case'
      AND COLUMN_NAME = 'profile_authorized'
);
SET @catalog_service_case_profile_authorized_sql := IF(
    @catalog_service_case_profile_authorized_exists = 0,
    'ALTER TABLE catalog_service_case ADD COLUMN profile_authorized TINYINT NOT NULL DEFAULT 0 AFTER intake_note',
    'SELECT 1'
);
PREPARE stmt FROM @catalog_service_case_profile_authorized_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @catalog_service_case_authorization_note_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'catalog_service_case'
      AND COLUMN_NAME = 'authorization_note'
);
SET @catalog_service_case_authorization_note_sql := IF(
    @catalog_service_case_authorization_note_exists = 0,
    'ALTER TABLE catalog_service_case ADD COLUMN authorization_note VARCHAR(255) DEFAULT NULL AFTER profile_authorized',
    'SELECT 1'
);
PREPARE stmt FROM @catalog_service_case_authorization_note_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @catalog_service_case_authorization_updated_by_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'catalog_service_case'
      AND COLUMN_NAME = 'authorization_updated_by'
);
SET @catalog_service_case_authorization_updated_by_sql := IF(
    @catalog_service_case_authorization_updated_by_exists = 0,
    'ALTER TABLE catalog_service_case ADD COLUMN authorization_updated_by VARCHAR(64) DEFAULT NULL AFTER authorization_note',
    'SELECT 1'
);
PREPARE stmt FROM @catalog_service_case_authorization_updated_by_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @catalog_service_case_authorization_updated_at_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'catalog_service_case'
      AND COLUMN_NAME = 'authorization_updated_at'
);
SET @catalog_service_case_authorization_updated_at_sql := IF(
    @catalog_service_case_authorization_updated_at_exists = 0,
    'ALTER TABLE catalog_service_case ADD COLUMN authorization_updated_at DATETIME DEFAULT NULL AFTER authorization_updated_by',
    'SELECT 1'
);
PREPARE stmt FROM @catalog_service_case_authorization_updated_at_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS catalog_service_alert (
    alert_id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) DEFAULT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(64) NOT NULL,
    alert_type VARCHAR(64) NOT NULL,
    alert_level INT NOT NULL,
    trigger_reason TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    alert_status VARCHAR(32) NOT NULL,
    resolution_note VARCHAR(255) DEFAULT NULL,
    handled_by VARCHAR(64) DEFAULT NULL,
    handled_at DATETIME DEFAULT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (alert_id),
    KEY idx_catalog_service_alert_sort_no (sort_no),
    KEY idx_catalog_service_alert_level (alert_level),
    KEY idx_catalog_service_alert_status (alert_status),
    KEY idx_catalog_service_alert_case_id (case_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @catalog_service_alert_case_id_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'catalog_service_alert'
      AND COLUMN_NAME = 'case_id'
);
SET @catalog_service_alert_case_id_sql := IF(
    @catalog_service_alert_case_id_exists = 0,
    'ALTER TABLE catalog_service_alert ADD COLUMN case_id VARCHAR(64) DEFAULT NULL AFTER alert_id',
    'SELECT 1'
);
PREPARE stmt FROM @catalog_service_alert_case_id_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @catalog_service_alert_resolution_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'catalog_service_alert'
      AND COLUMN_NAME = 'resolution_note'
);
SET @catalog_service_alert_resolution_sql := IF(
    @catalog_service_alert_resolution_exists = 0,
    'ALTER TABLE catalog_service_alert ADD COLUMN resolution_note VARCHAR(255) DEFAULT NULL AFTER alert_status',
    'SELECT 1'
);
PREPARE stmt FROM @catalog_service_alert_resolution_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @catalog_service_alert_handled_by_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'catalog_service_alert'
      AND COLUMN_NAME = 'handled_by'
);
SET @catalog_service_alert_handled_by_sql := IF(
    @catalog_service_alert_handled_by_exists = 0,
    'ALTER TABLE catalog_service_alert ADD COLUMN handled_by VARCHAR(64) DEFAULT NULL AFTER resolution_note',
    'SELECT 1'
);
PREPARE stmt FROM @catalog_service_alert_handled_by_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @catalog_service_alert_handled_at_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'catalog_service_alert'
      AND COLUMN_NAME = 'handled_at'
);
SET @catalog_service_alert_handled_at_sql := IF(
    @catalog_service_alert_handled_at_exists = 0,
    'ALTER TABLE catalog_service_alert ADD COLUMN handled_at DATETIME DEFAULT NULL AFTER handled_by',
    'SELECT 1'
);
PREPARE stmt FROM @catalog_service_alert_handled_at_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @catalog_service_alert_case_id_index_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'catalog_service_alert'
      AND INDEX_NAME = 'idx_catalog_service_alert_case_id'
);
SET @catalog_service_alert_case_id_index_sql := IF(
    @catalog_service_alert_case_id_index_exists = 0,
    'ALTER TABLE catalog_service_alert ADD INDEX idx_catalog_service_alert_case_id (case_id)',
    'SELECT 1'
);
PREPARE stmt FROM @catalog_service_alert_case_id_index_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS service_case_intervention (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    case_id VARCHAR(64) NOT NULL,
    intervention_type VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    attachment_note VARCHAR(255) DEFAULT NULL,
    operator_name VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_service_case_intervention_case_type_time (case_id, intervention_type, created_at, is_deleted),
    KEY idx_service_case_intervention_case_id (case_id),
    KEY idx_service_case_intervention_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS service_followup_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    case_id VARCHAR(64) NOT NULL,
    job_id VARCHAR(64) DEFAULT NULL,
    followup_stage VARCHAR(32) NOT NULL,
    adaptation_score INT NOT NULL,
    environment_issue VARCHAR(255) DEFAULT NULL,
    communication_issue VARCHAR(255) DEFAULT NULL,
    support_implemented TINYINT NOT NULL DEFAULT 0,
    leave_risk TINYINT NOT NULL DEFAULT 0,
    need_help TINYINT NOT NULL DEFAULT 0,
    record_status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    operator_name VARCHAR(64) NOT NULL,
    due_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME DEFAULT NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_service_followup_record_case_stage_time (case_id, followup_stage, completed_at, is_deleted),
    KEY idx_service_followup_record_case_id (case_id),
    KEY idx_service_followup_record_stage (followup_stage),
    KEY idx_service_followup_record_due_at (due_at),
    KEY idx_service_followup_record_completed_at (completed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @service_followup_record_due_at_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'service_followup_record'
      AND COLUMN_NAME = 'due_at'
);
SET @service_followup_record_due_at_sql := IF(
    @service_followup_record_due_at_exists = 0,
    'ALTER TABLE service_followup_record ADD COLUMN due_at DATETIME DEFAULT NULL AFTER operator_name',
    'SELECT 1'
);
PREPARE stmt FROM @service_followup_record_due_at_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @service_followup_record_due_at_index_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'service_followup_record'
      AND INDEX_NAME = 'idx_service_followup_record_due_at'
);
SET @service_followup_record_due_at_index_sql := IF(
    @service_followup_record_due_at_index_exists = 0,
    'ALTER TABLE service_followup_record ADD INDEX idx_service_followup_record_due_at (due_at)',
    'SELECT 1'
);
PREPARE stmt FROM @service_followup_record_due_at_index_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE service_followup_record
    MODIFY COLUMN completed_at DATETIME DEFAULT NULL;

CREATE TABLE IF NOT EXISTS service_resource_referral (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    case_id VARCHAR(64) NOT NULL,
    referral_type VARCHAR(32) NOT NULL,
    resource_name VARCHAR(128) NOT NULL,
    provider_name VARCHAR(128) DEFAULT NULL,
    contact_name VARCHAR(64) DEFAULT NULL,
    contact_phone VARCHAR(32) DEFAULT NULL,
    scheduled_at DATETIME DEFAULT NULL,
    referral_status VARCHAR(32) NOT NULL DEFAULT 'PLANNED',
    status_note VARCHAR(255) DEFAULT NULL,
    operator_name VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_service_resource_referral_case_id (case_id),
    KEY idx_service_resource_referral_status (referral_status),
    KEY idx_service_resource_referral_scheduled_at (scheduled_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS employment_followup (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    job_id VARCHAR(64) NOT NULL,
    followup_stage VARCHAR(16) NOT NULL,
    adaptation_score TINYINT NOT NULL,
    environment_issue VARCHAR(500) DEFAULT NULL,
    communication_issue VARCHAR(500) DEFAULT NULL,
    support_implemented TINYINT NOT NULL DEFAULT 0,
    leave_risk TINYINT NOT NULL DEFAULT 0,
    need_help TINYINT NOT NULL DEFAULT 0,
    remark VARCHAR(1000) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_employment_followup_user_job_stage (user_id, job_id, followup_stage, is_deleted),
    KEY idx_employment_followup_user_job (user_id, job_id),
    KEY idx_employment_followup_stage (followup_stage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE employment_followup
    MODIFY COLUMN job_id VARCHAR(64) NOT NULL;

CREATE TABLE IF NOT EXISTS catalog_admin_dashboard (
    code VARCHAR(64) NOT NULL,
    jobseeker_count INT NOT NULL,
    enterprise_count INT NOT NULL,
    published_job_count INT NOT NULL,
    application_count INT NOT NULL,
    hired_count INT NOT NULL,
    open_alert_count INT NOT NULL,
    PRIMARY KEY (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS catalog_admin_metric (
    metric_key VARCHAR(64) NOT NULL,
    label VARCHAR(64) NOT NULL,
    metric_value VARCHAR(32) NOT NULL,
    hint VARCHAR(255) NOT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    PRIMARY KEY (metric_key),
    KEY idx_catalog_admin_metric_sort_no (sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS catalog_enterprise_review (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    company VARCHAR(128) NOT NULL,
    industry VARCHAR(64) NOT NULL,
    city VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    note VARCHAR(255) NOT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_catalog_enterprise_review_company (company),
    KEY idx_catalog_enterprise_review_sort_no (sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS catalog_audit_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    content VARCHAR(255) NOT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_catalog_audit_log_content (content),
    KEY idx_catalog_audit_log_sort_no (sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS catalog_knowledge_article (
    id VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(64) NOT NULL,
    summary TEXT NOT NULL,
    tags JSON NOT NULL,
    publish_date DATE NOT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_catalog_knowledge_article_sort_no (sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS catalog_notification (
    id VARCHAR(64) NOT NULL,
    type VARCHAR(32) NOT NULL,
    title VARCHAR(128) NOT NULL,
    content VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL,
    read_flag TINYINT NOT NULL DEFAULT 0,
    sort_no INT NOT NULL DEFAULT 0,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_catalog_notification_sort_no (sort_no),
    KEY idx_catalog_notification_read_flag (read_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS knowledge_article (
    id VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(64) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    tags JSON NOT NULL,
    publish_status VARCHAR(16) NOT NULL DEFAULT 'OFFLINE',
    published_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sort_no INT NOT NULL DEFAULT 0,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_knowledge_article_status (publish_status),
    KEY idx_knowledge_article_updated_at (updated_at),
    KEY idx_knowledge_article_sort_no (sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notification_message (
    id VARCHAR(64) NOT NULL,
    type VARCHAR(32) NOT NULL,
    title VARCHAR(128) NOT NULL,
    content VARCHAR(500) NOT NULL,
    target_role VARCHAR(32) NOT NULL DEFAULT 'ALL',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_user_id BIGINT UNSIGNED DEFAULT NULL,
    read_flag TINYINT NOT NULL DEFAULT 0,
    sort_no INT NOT NULL DEFAULT 0,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_notification_message_role (target_role),
    KEY idx_notification_message_created_at (created_at),
    KEY idx_notification_message_sort_no (sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @notification_message_id_column_type := (
    SELECT COLUMN_TYPE
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notification_message'
      AND COLUMN_NAME = 'id'
);
SET @notification_message_id_sql := IF(
    @notification_message_id_column_type IS NOT NULL
        AND @notification_message_id_column_type <> 'varchar(64)',
    'ALTER TABLE notification_message CHANGE COLUMN id id VARCHAR(64) NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @notification_message_id_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @notification_message_type_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notification_message'
      AND COLUMN_NAME = 'type'
);
SET @notification_message_legacy_type_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notification_message'
      AND COLUMN_NAME = 'message_type'
);
SET @notification_message_type_sql := IF(
    @notification_message_type_exists = 0
        AND @notification_message_legacy_type_exists = 1,
    'ALTER TABLE notification_message CHANGE COLUMN message_type type VARCHAR(32) NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @notification_message_type_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @notification_message_read_flag_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notification_message'
      AND COLUMN_NAME = 'read_flag'
);
SET @notification_message_legacy_read_flag_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notification_message'
      AND COLUMN_NAME = 'read_status'
);
SET @notification_message_read_flag_sql := IF(
    @notification_message_read_flag_exists = 0
        AND @notification_message_legacy_read_flag_exists = 1,
    'ALTER TABLE notification_message CHANGE COLUMN read_status read_flag TINYINT NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt FROM @notification_message_read_flag_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @notification_message_target_role_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notification_message'
      AND COLUMN_NAME = 'target_role'
);
SET @notification_message_target_role_sql := IF(
    @notification_message_target_role_exists = 0,
    'ALTER TABLE notification_message ADD COLUMN target_role VARCHAR(32) NOT NULL DEFAULT ''ALL'' AFTER content',
    'SELECT 1'
);
PREPARE stmt FROM @notification_message_target_role_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @notification_message_created_by_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notification_message'
      AND COLUMN_NAME = 'created_by_user_id'
);
SET @notification_message_created_by_sql := IF(
    @notification_message_created_by_exists = 0,
    'ALTER TABLE notification_message ADD COLUMN created_by_user_id BIGINT UNSIGNED DEFAULT NULL AFTER created_at',
    'SELECT 1'
);
PREPARE stmt FROM @notification_message_created_by_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @notification_message_sort_no_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notification_message'
      AND COLUMN_NAME = 'sort_no'
);
SET @notification_message_sort_no_sql := IF(
    @notification_message_sort_no_exists = 0,
    'ALTER TABLE notification_message ADD COLUMN sort_no INT NOT NULL DEFAULT 0 AFTER read_flag',
    'SELECT 1'
);
PREPARE stmt FROM @notification_message_sort_no_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @notification_message_title_column_type := (
    SELECT COLUMN_TYPE
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notification_message'
      AND COLUMN_NAME = 'title'
);
SET @notification_message_title_can_shrink := (
    SELECT CASE
        WHEN @notification_message_title_column_type IS NOT NULL
         AND @notification_message_title_column_type <> 'varchar(128)'
         AND COALESCE((SELECT MAX(CHAR_LENGTH(title)) FROM notification_message), 0) <= 128
        THEN 1 ELSE 0
    END
);
SET @notification_message_title_sql := IF(
    @notification_message_title_can_shrink = 1,
    'ALTER TABLE notification_message MODIFY COLUMN title VARCHAR(128) NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @notification_message_title_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @notification_message_content_column_type := (
    SELECT COLUMN_TYPE
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notification_message'
      AND COLUMN_NAME = 'content'
);
SET @notification_message_content_can_shrink := (
    SELECT CASE
        WHEN @notification_message_content_column_type IS NOT NULL
         AND @notification_message_content_column_type <> 'varchar(500)'
         AND COALESCE((SELECT MAX(CHAR_LENGTH(content)) FROM notification_message), 0) <= 500
        THEN 1 ELSE 0
    END
);
SET @notification_message_content_sql := IF(
    @notification_message_content_can_shrink = 1,
    'ALTER TABLE notification_message MODIFY COLUMN content VARCHAR(500) NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @notification_message_content_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @notification_message_receiver_user_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notification_message'
      AND COLUMN_NAME = 'receiver_user_id'
);
SET @notification_message_receiver_user_sql := IF(
    @notification_message_receiver_user_exists = 1,
    'ALTER TABLE notification_message MODIFY COLUMN receiver_user_id BIGINT UNSIGNED DEFAULT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @notification_message_receiver_user_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @notification_message_receiver_user_can_drop := 0;
SET @notification_message_receiver_user_index_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notification_message'
      AND INDEX_NAME = 'idx_notification_receiver_read'
);
SET @notification_message_receiver_user_drop_index_sql := IF(
    @notification_message_receiver_user_can_drop = 1
        AND @notification_message_receiver_user_index_exists = 1,
    'ALTER TABLE notification_message DROP INDEX idx_notification_receiver_read',
    'SELECT 1'
);
PREPARE stmt FROM @notification_message_receiver_user_drop_index_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @notification_message_receiver_user_drop_sql := IF(
    @notification_message_receiver_user_can_drop = 1,
    'ALTER TABLE notification_message DROP COLUMN receiver_user_id',
    'SELECT 1'
);
PREPARE stmt FROM @notification_message_receiver_user_drop_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @notification_message_receiver_read_index_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notification_message'
      AND INDEX_NAME = 'idx_notification_receiver_read'
);
SET @notification_message_receiver_read_index_sql := IF(
    @notification_message_receiver_read_index_exists = 1,
    'ALTER TABLE notification_message DROP INDEX idx_notification_receiver_read',
    'SELECT 1'
);
PREPARE stmt FROM @notification_message_receiver_read_index_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @notification_message_role_index_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notification_message'
      AND INDEX_NAME = 'idx_notification_message_role'
);
SET @notification_message_role_index_sql := IF(
    @notification_message_role_index_exists = 0,
    'ALTER TABLE notification_message ADD INDEX idx_notification_message_role (target_role)',
    'SELECT 1'
);
PREPARE stmt FROM @notification_message_role_index_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @notification_message_created_at_index_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notification_message'
      AND INDEX_NAME = 'idx_notification_message_created_at'
);
SET @notification_message_created_at_index_sql := IF(
    @notification_message_created_at_index_exists = 0,
    'ALTER TABLE notification_message ADD INDEX idx_notification_message_created_at (created_at)',
    'SELECT 1'
);
PREPARE stmt FROM @notification_message_created_at_index_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @notification_message_sort_no_index_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notification_message'
      AND INDEX_NAME = 'idx_notification_message_sort_no'
);
SET @notification_message_sort_no_index_sql := IF(
    @notification_message_sort_no_index_exists = 0,
    'ALTER TABLE notification_message ADD INDEX idx_notification_message_sort_no (sort_no)',
    'SELECT 1'
);
PREPARE stmt FROM @notification_message_sort_no_index_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS notification_read_log (
    notification_id VARCHAR(64) NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    read_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (notification_id, user_id),
    KEY idx_notification_read_log_user (user_id),
    CONSTRAINT fk_notification_read_log_message
        FOREIGN KEY (notification_id) REFERENCES notification_message (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tag_dictionary (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    tag_code VARCHAR(64) NOT NULL,
    tag_name VARCHAR(64) NOT NULL,
    tag_category VARCHAR(32) NOT NULL,
    tag_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    description VARCHAR(255) DEFAULT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tag_dictionary_code (tag_code),
    KEY idx_tag_dictionary_category (tag_category),
    KEY idx_tag_dictionary_status (tag_status),
    KEY idx_tag_dictionary_sort_no (sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admin_issue_ticket (
    id VARCHAR(64) NOT NULL,
    issue_type VARCHAR(32) NOT NULL,
    source_role VARCHAR(32) NOT NULL,
    source_user_id BIGINT UNSIGNED DEFAULT NULL,
    source_name VARCHAR(64) NOT NULL,
    title VARCHAR(128) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    related_type VARCHAR(32) DEFAULT NULL,
    related_id VARCHAR(64) DEFAULT NULL,
    severity_level INT NOT NULL DEFAULT 1,
    ticket_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    resolution_note VARCHAR(500) DEFAULT NULL,
    handled_by VARCHAR(64) DEFAULT NULL,
    handled_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    sort_no INT NOT NULL DEFAULT 0,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_admin_issue_ticket_type (issue_type),
    KEY idx_admin_issue_ticket_status (ticket_status),
    KEY idx_admin_issue_ticket_severity (severity_level),
    KEY idx_admin_issue_ticket_created_at (created_at),
    KEY idx_admin_issue_ticket_sort_no (sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS catalog_matching_status (
    code VARCHAR(64) NOT NULL,
    mode VARCHAR(32) NOT NULL,
    version VARCHAR(32) NOT NULL,
    dimensions JSON NOT NULL,
    rule_count INT NOT NULL,
    available_job_count INT NOT NULL,
    candidate_count INT NOT NULL,
    PRIMARY KEY (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admin_matching_config (
    config_code VARCHAR(64) NOT NULL,
    skill_weight DECIMAL(8, 2) NOT NULL,
    work_mode_weight DECIMAL(8, 2) NOT NULL,
    communication_weight DECIMAL(8, 2) NOT NULL,
    environment_weight DECIMAL(8, 2) NOT NULL,
    accommodation_weight DECIMAL(8, 2) NOT NULL,
    penalty_per_risk INT NOT NULL,
    penalty_per_blocking_risk INT NOT NULL,
    max_penalty INT NOT NULL,
    hard_filtered_max_score INT NOT NULL,
    match_score_weight DECIMAL(8, 2) NOT NULL,
    profile_completion_weight DECIMAL(8, 2) NOT NULL,
    priority_threshold INT NOT NULL,
    follow_up_threshold INT NOT NULL,
    updated_by_user_id BIGINT UNSIGNED DEFAULT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (config_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
