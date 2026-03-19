-- Flyway V10: FAMILY에서 월별 quota/usage 상태 컬럼 제거

ALTER TABLE `family`
    DROP COLUMN `total_quota_bytes`,
    DROP COLUMN `used_bytes`,
    DROP COLUMN `current_month`;
