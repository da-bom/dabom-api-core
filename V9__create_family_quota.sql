-- Flyway V9: FAMILY_QUOTA 테이블 추가 (family 월별 총량 분리)

CREATE TABLE `family_quota` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `family_id` BIGINT NOT NULL,
    `current_month` DATE NOT NULL,
    `total_quota_bytes` BIGINT NOT NULL,
    `used_bytes` BIGINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at` DATETIME NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_family_quota` (`family_id`, `current_month`, `deleted_at`),
    INDEX `idx_fquota_family_month` (`family_id`, `current_month`),
    CONSTRAINT `fk_fquota_family` FOREIGN KEY (`family_id`) REFERENCES `family` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
