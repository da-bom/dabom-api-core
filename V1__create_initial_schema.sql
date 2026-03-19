-- ============================================================
-- Flyway V1: 초기 스키마 생성 (ERD v10.4 기반, 1차 개발 9개 테이블)
-- ============================================================

-- ============================================================
-- 1. CUSTOMER (사용자)
-- ============================================================
CREATE TABLE `customer` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `phone_number` VARCHAR(11) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `email` VARCHAR(255) NULL,
    `is_onboarded` BOOLEAN NOT NULL DEFAULT FALSE,
    `terms_agreed_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at` DATETIME NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_customer_phone` (`phone_number`, `deleted_at`),
    INDEX `idx_customer_phone` (`phone_number`),
    INDEX `idx_customer_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 2. ADMIN (백오피스 운영자)
-- ============================================================
CREATE TABLE `admin` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `email` VARCHAR(255) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at` DATETIME NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_admin_email` (`email`, `deleted_at`),
    INDEX `idx_admin_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 3. POLICY (정책)
-- ============================================================
CREATE TABLE `policy` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `description` VARCHAR(255) NULL,
    `require_role` ENUM('MEMBER', 'OWNER') NOT NULL DEFAULT 'MEMBER',
    `type` ENUM('MONTHLY_LIMIT', 'TIME_BLOCK', 'APP_BLOCK', 'MANUAL_BLOCK') NOT NULL,
    `default_rules` JSON NOT NULL,
    `is_system` BOOLEAN NOT NULL DEFAULT FALSE,
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at` DATETIME NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 4. FAMILY (가족)
-- ============================================================
CREATE TABLE `family` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `created_by_id` BIGINT NOT NULL,
    `total_quota_bytes` BIGINT NOT NULL DEFAULT 107374182400,
    `used_bytes` BIGINT NOT NULL DEFAULT 0,
    `current_month` DATE NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at` DATETIME NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_family_created_by` (`created_by_id`),
    CONSTRAINT `fk_family_created_by` FOREIGN KEY (`created_by_id`) REFERENCES `customer` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 5. FAMILY_MEMBER (가족 구성원)
-- ============================================================
CREATE TABLE `family_member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `family_id` BIGINT NOT NULL,
    `customer_id` BIGINT NOT NULL,
    `role` ENUM('MEMBER', 'OWNER') NOT NULL DEFAULT 'MEMBER',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at` DATETIME NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_family_member` (`family_id`, `customer_id`, `deleted_at`),
    INDEX `idx_member_family` (`family_id`),
    INDEX `idx_member_customer` (`customer_id`),
    CONSTRAINT `fk_member_family` FOREIGN KEY (`family_id`) REFERENCES `family` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_member_customer` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 6. CUSTOMER_QUOTA (구성원 월별 할당량)
-- ============================================================
CREATE TABLE `customer_quota` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `customer_id` BIGINT NOT NULL,
    `family_id` BIGINT NOT NULL,
    `monthly_limit_bytes` BIGINT NULL,
    `monthly_used_bytes` BIGINT NOT NULL DEFAULT 0,
    `current_month` DATE NOT NULL,
    `is_blocked` BOOLEAN NOT NULL DEFAULT FALSE,
    `block_reason` VARCHAR(50) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at` DATETIME NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cquota` (`customer_id`, `family_id`, `current_month`, `deleted_at`),
    INDEX `idx_cquota_customer_month` (`customer_id`, `current_month`),
    INDEX `idx_cquota_family` (`family_id`),
    CONSTRAINT `fk_cquota_customer` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_cquota_family` FOREIGN KEY (`family_id`) REFERENCES `family` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 7. USAGE_RECORD (데이터 사용 이력)
-- ============================================================
CREATE TABLE `usage_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `event_id` VARCHAR(50) NOT NULL,
    `customer_id` BIGINT NOT NULL,
    `family_id` BIGINT NOT NULL,
    `bytes_used` BIGINT NOT NULL,
    `app_id` VARCHAR(100) NULL,
    `event_time` DATETIME NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted_at` DATETIME NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_usage_event` (`event_id`),
    INDEX `idx_usage_family_time` (`family_id`, `event_time`),
    INDEX `idx_usage_customer_time` (`customer_id`, `event_time`),
    INDEX `idx_usage_event_id` (`event_id`),
    CONSTRAINT `fk_usage_customer` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_usage_family` FOREIGN KEY (`family_id`) REFERENCES `family` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 8. POLICY_ASSIGNMENT (정책 적용)
-- ============================================================
CREATE TABLE `policy_assignment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `policy_id` BIGINT NOT NULL,
    `family_id` BIGINT NOT NULL,
    `target_customer_id` BIGINT NULL,
    `rules` JSON NOT NULL,
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
    `applied_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `applied_by_id` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at` DATETIME NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_pa_family` (`family_id`),
    INDEX `idx_pa_target` (`target_customer_id`),
    CONSTRAINT `fk_pa_policy` FOREIGN KEY (`policy_id`) REFERENCES `policy` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_pa_family` FOREIGN KEY (`family_id`) REFERENCES `family` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_pa_target` FOREIGN KEY (`target_customer_id`) REFERENCES `customer` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_pa_applied_by` FOREIGN KEY (`applied_by_id`) REFERENCES `customer` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 9. NOTIFICATION_LOG (알림 로그)
-- ============================================================
CREATE TABLE `notification_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `customer_id` BIGINT NOT NULL,
    `family_id` BIGINT NOT NULL,
    `type` ENUM('THRESHOLD_ALERT', 'BLOCKED', 'UNBLOCKED', 'POLICY_CHANGED', 'MISSION_CREATED', 'REWARD_REQUESTED', 'REWARD_APPROVED', 'REWARD_REJECTED', 'APPEAL_CREATED', 'APPEAL_APPROVED', 'APPEAL_REJECTED', 'EMERGENCY_APPROVED') NOT NULL,
    `message` TEXT NOT NULL,
    `payload` JSON NULL,
    `is_read` BOOLEAN NOT NULL DEFAULT FALSE,
    `sent_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted_at` DATETIME NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_notif_customer` (`customer_id`, `sent_at` DESC),
    INDEX `idx_notif_customer_type` (`customer_id`, `type`, `sent_at` DESC),
    INDEX `idx_notif_family` (`family_id`, `sent_at` DESC),
    CONSTRAINT `fk_notif_customer` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_notif_family` FOREIGN KEY (`family_id`) REFERENCES `family` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
