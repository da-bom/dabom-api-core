-- ============================================================
-- Flyway V13: MISSION_REQUEST active_request_mission_id 동기화
-- ERD v23.3 기준으로 active_request_mission_id 컬럼과
-- uk_mission_request_active_request_mission UNIQUE 제약을 보정한다.
-- 기존 환경에 V5가 이미 반영된 경우에도 안전하게 통과하도록 구성한다.
-- ============================================================

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'mission_request'
              AND COLUMN_NAME = 'active_request_mission_id'
        ),
        'SELECT 1',
        'ALTER TABLE `mission_request` ADD COLUMN `active_request_mission_id` BIGINT NULL AFTER `id`'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `mission_request`
SET `active_request_mission_id` = `mission_item_id`
WHERE `status` = 'PENDING'
  AND `active_request_mission_id` IS NULL;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'mission_request'
              AND INDEX_NAME = 'uk_mission_request_active_request_mission'
        ),
        'SELECT 1',
        'ALTER TABLE `mission_request` ADD CONSTRAINT `uk_mission_request_active_request_mission` UNIQUE (`active_request_mission_id`)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
