-- ============================================================
-- Flyway V2: 2차 개발 테이블 (ERD v10.4 기반)
-- ============================================================

-- ============================================================
-- 10. AUDIT_LOG (감사 로그)
-- ============================================================
CREATE TABLE `audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `actor_id` BIGINT NULL,
    `action` VARCHAR(50) NOT NULL,
    `entity_type` VARCHAR(50) NOT NULL,
    `entity_id` BIGINT NOT NULL,
    `old_value` JSON NULL,
    `new_value` JSON NULL,
    `ip_address` VARCHAR(45) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted_at` DATETIME NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_audit_actor` (`actor_id`, `created_at` DESC),
    INDEX `idx_audit_entity` (`entity_type`, `entity_id`, `created_at` DESC),
    INDEX `idx_audit_action` (`action`, `created_at` DESC),
    CONSTRAINT `fk_audit_actor` FOREIGN KEY (`actor_id`) REFERENCES `customer` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 11. INVITE (가족 초대)
-- ============================================================
CREATE TABLE `invite` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `family_id` BIGINT NOT NULL,
    `phone_number` VARCHAR(11) NOT NULL,
    `role` ENUM('MEMBER', 'OWNER') NOT NULL DEFAULT 'MEMBER',
    `status` ENUM('PENDING', 'ACCEPTED', 'EXPIRED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    `expires_at` DATETIME NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted_at` DATETIME NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_invite_phone` (`phone_number`, `status`),
    INDEX `idx_invite_family` (`family_id`, `status`),
    CONSTRAINT `fk_invite_family` FOREIGN KEY (`family_id`) REFERENCES `family` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
