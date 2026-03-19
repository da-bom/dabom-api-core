-- ============================================================
-- Flyway V3: Phase 3 테이블 (ERD v21.0 기반)
-- ============================================================

-- ============================================================
-- 12. REWARD_TEMPLATE (보상 템플릿)
-- ============================================================
CREATE TABLE `reward_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `category` ENUM('DATA', 'GIFTICON') NOT NULL,
    `thumbnail_url` VARCHAR(500) NULL,
    `price` INT NOT NULL,
    `is_system` BOOLEAN NOT NULL DEFAULT TRUE,
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at` DATETIME NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 13. REWARD (보상 인스턴스)
-- ============================================================
CREATE TABLE `reward` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `reward_template_id` BIGINT NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `category` ENUM('DATA', 'GIFTICON') NOT NULL,
    `thumbnail_url` VARCHAR(500) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_reward_template` (`reward_template_id`),
    CONSTRAINT `fk_reward_template` FOREIGN KEY (`reward_template_id`) REFERENCES `reward_template` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 14. MISSION_ITEM (미션 항목)
-- ============================================================
CREATE TABLE `mission_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `family_id` BIGINT NOT NULL,
    `created_by_id` BIGINT NOT NULL,
    `target_customer_id` BIGINT NOT NULL,
    `reward_id` BIGINT NOT NULL,
    `mission_text` TEXT NOT NULL,
    `status` ENUM('ACTIVE', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'ACTIVE',
    `completed_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_mission_family` (`family_id`, `status`, `created_at` DESC),
    INDEX `idx_mission_creator` (`created_by_id`),
    INDEX `idx_mission_target` (`target_customer_id`, `status`, `created_at` DESC),
    CONSTRAINT `fk_mission_family` FOREIGN KEY (`family_id`) REFERENCES `family` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_mission_creator` FOREIGN KEY (`created_by_id`) REFERENCES `customer` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_mission_target` FOREIGN KEY (`target_customer_id`) REFERENCES `customer` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_mission_reward` FOREIGN KEY (`reward_id`) REFERENCES `reward` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 15. MISSION_REQUEST (미션 보상 요청)
-- ============================================================
CREATE TABLE `mission_request` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `mission_item_id` BIGINT NOT NULL,
    `requester_id` BIGINT NOT NULL,
    `status` ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    `reject_reason` TEXT NULL,
    `resolved_by_id` BIGINT NULL,
    `resolved_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_mreq_mission` (`mission_item_id`, `created_at` DESC),
    INDEX `idx_mreq_requester` (`requester_id`, `created_at` DESC),
    CONSTRAINT `fk_mreq_mission` FOREIGN KEY (`mission_item_id`) REFERENCES `mission_item` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_mreq_requester` FOREIGN KEY (`requester_id`) REFERENCES `customer` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_mreq_resolver` FOREIGN KEY (`resolved_by_id`) REFERENCES `customer` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 16. MISSION_LOG (미션 이벤트 로그)
-- ============================================================
CREATE TABLE `mission_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `mission_item_id` BIGINT NOT NULL,
    `actor_id` BIGINT NULL,
    `action_type` ENUM('MISSION_CREATED', 'MISSION_REQUESTED', 'MISSION_APPROVED', 'MISSION_REJECTED', 'MISSION_COMPLETED') NOT NULL,
    `message` VARCHAR(500) NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_mission_log_item` (`mission_item_id`, `created_at`),
    INDEX `idx_mission_log_actor` (`actor_id`),
    CONSTRAINT `fk_mlog_mission` FOREIGN KEY (`mission_item_id`) REFERENCES `mission_item` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_mlog_actor` FOREIGN KEY (`actor_id`) REFERENCES `customer` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 17. REWARD_GRANT (보상 지급)
-- ============================================================
CREATE TABLE `reward_grant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `reward_id` BIGINT NOT NULL,
    `customer_id` BIGINT NOT NULL,
    `mission_item_id` BIGINT NOT NULL,
    `coupon_code` VARCHAR(100) NULL,
    `coupon_url` VARCHAR(255) NULL,
    `status` ENUM('ISSUED', 'USED', 'EXPIRED') NOT NULL DEFAULT 'ISSUED',
    `expired_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_reward_grant_customer` (`customer_id`),
    INDEX `idx_reward_grant_status` (`status`, `created_at`),
    INDEX `idx_reward_grant_expired` (`expired_at`),
    CONSTRAINT `fk_rg_reward` FOREIGN KEY (`reward_id`) REFERENCES `reward` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_rg_customer` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_rg_mission` FOREIGN KEY (`mission_item_id`) REFERENCES `mission_item` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 18. POLICY_APPEAL (정책 이의제기)
-- ============================================================
CREATE TABLE `policy_appeal` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `type` ENUM('NORMAL', 'EMERGENCY') NOT NULL DEFAULT 'NORMAL',
    `policy_assignment_id` BIGINT NULL,
    `requester_id` BIGINT NOT NULL,
    `request_reason` TEXT NOT NULL,
    `reject_reason` TEXT NULL,
    `desired_rules` JSON NULL,
    `status` ENUM('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    `resolved_by_id` BIGINT NULL,
    `resolved_at` DATETIME NULL,
    `cancelled_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_appeal_assignment` (`policy_assignment_id`),
    INDEX `idx_appeal_requester` (`requester_id`),
    INDEX `idx_appeal_emergency_monthly` (`requester_id`, `type`, `status`, `created_at`),
    CONSTRAINT `fk_appeal_assignment` FOREIGN KEY (`policy_assignment_id`) REFERENCES `policy_assignment` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_appeal_requester` FOREIGN KEY (`requester_id`) REFERENCES `customer` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_appeal_resolver` FOREIGN KEY (`resolved_by_id`) REFERENCES `customer` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 19. POLICY_APPEAL_COMMENT (이의제기 댓글)
-- ============================================================
CREATE TABLE `policy_appeal_comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `appeal_id` BIGINT NOT NULL,
    `author_id` BIGINT NOT NULL,
    `comment` TEXT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted_at` DATETIME NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_appeal_comment_appeal` (`appeal_id`, `created_at`),
    INDEX `idx_appeal_comment_author` (`author_id`),
    CONSTRAINT `fk_acomment_appeal` FOREIGN KEY (`appeal_id`) REFERENCES `policy_appeal` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_acomment_author` FOREIGN KEY (`author_id`) REFERENCES `customer` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 20. FAMILY_RECAP_WEEKLY (주간 가족 리캡)
-- ============================================================
CREATE TABLE `family_recap_weekly` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `family_id` BIGINT NOT NULL,
    `week_start_date` DATE NOT NULL,
    `total_used_bytes` BIGINT NOT NULL,
    `total_quota_bytes` BIGINT NOT NULL,
    `usage_rate_percent` DECIMAL(5,2) NOT NULL,
    `usage_by_weekday` JSON NOT NULL,
    `peak_usage` JSON NULL,
    `mission_created_count` INT NOT NULL DEFAULT 0,
    `mission_completed_count` INT NOT NULL DEFAULT 0,
    `mission_rejected_count` INT NOT NULL DEFAULT 0,
    `appeal_count` INT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_recap_weekly` (`family_id`, `week_start_date`),
    INDEX `idx_recap_weekly_family_week` (`family_id`, `week_start_date` DESC),
    CONSTRAINT `fk_recap_weekly_family` FOREIGN KEY (`family_id`) REFERENCES `family` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 21. FAMILY_RECAP_MONTHLY (월간 가족 리캡)
-- ============================================================
CREATE TABLE `family_recap_monthly` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `family_id` BIGINT NOT NULL,
    `report_month` DATE NOT NULL,
    `total_used_bytes` BIGINT NOT NULL,
    `total_quota_bytes` BIGINT NOT NULL,
    `usage_rate_percent` DECIMAL(5,2) NOT NULL,
    `usage_by_weekday` JSON NULL,
    `peak_usage` JSON NULL,
    `mission_summary_json` JSON NULL,
    `appeal_summary_json` JSON NULL,
    `appeal_highlights_json` JSON NULL,
    `communication_score` DECIMAL(5,2) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_recap_monthly` (`family_id`, `report_month`),
    INDEX `idx_recap_monthly_family_month` (`family_id`, `report_month` DESC),
    CONSTRAINT `fk_recap_monthly_family` FOREIGN KEY (`family_id`) REFERENCES `family` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
