-- ============================================================
-- Flyway V12: usage_event_outbox 테이블 추가 (ERD v24.0)
-- 변경 사유: usage 이벤트 후행 notification 발행 복구용 outbox를 도입하고 재시도 조회/멱등성 제약을 반영
-- ============================================================

CREATE TABLE `usage_event_outbox` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `event_id` VARCHAR(191) NOT NULL,
    `family_id` BIGINT NOT NULL,
    `customer_id` BIGINT NOT NULL,
    `status` ENUM('PREPARED', 'PUBLISH_PENDING', 'SKIPPED', 'FAILED', 'SENT') NOT NULL DEFAULT 'PREPARED',
    `payload_json` TEXT NULL,
    `retry_count` INT NOT NULL DEFAULT 0,
    `next_retry_at` DATETIME NULL,
    `last_error` VARCHAR(1000) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at` DATETIME NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_usage_event_outbox_event_id` (`event_id`),
    INDEX `idx_usage_outbox_status_retry` (`status`, `next_retry_at`),
    CONSTRAINT `fk_usage_outbox_family` FOREIGN KEY (`family_id`) REFERENCES `family` (`id`),
    CONSTRAINT `fk_usage_outbox_customer` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
