-- ============================================================
-- Flyway V14: notification_log title 컬럼 추가
-- ERD v23.2 기준으로 PWA Push title/body 분리를 위해 title 컬럼을 추가한다.
-- ============================================================

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'notification_log'
              AND COLUMN_NAME = 'title'
        ),
        'SELECT 1',
        'ALTER TABLE `notification_log` ADD COLUMN `title` VARCHAR(100) NOT NULL AFTER `type`'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
