-- ============================================================
-- Flyway V4: BaseEntity 일관성 확보 (ERD v21.1)
-- 전체 21개 엔티티에 created_at/updated_at/deleted_at 통일
-- 이력성/불변 테이블은 deleted_at 컬럼 존재하되 운영상 미사용 (항상 NULL)
-- ============================================================

-- ============================================================
-- V1 테이블 보강
-- ============================================================

-- usage_record: updated_at 추가
ALTER TABLE `usage_record`
    ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `created_at`;

-- notification_log: created_at, updated_at 추가
ALTER TABLE `notification_log`
    ADD COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER `sent_at`,
    ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `created_at`;

-- ============================================================
-- V2 테이블 보강
-- ============================================================

-- audit_log: updated_at 추가
ALTER TABLE `audit_log`
    ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `created_at`;

-- invite: updated_at 추가
ALTER TABLE `invite`
    ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `created_at`;

-- ============================================================
-- V3 테이블 보강
-- ============================================================

-- reward: deleted_at 추가 (이력성 테이블 - 운영상 미사용)
ALTER TABLE `reward`
    ADD COLUMN `deleted_at` DATETIME NULL AFTER `updated_at`;

-- mission_item: deleted_at 추가 (이력성 테이블 - 운영상 미사용)
ALTER TABLE `mission_item`
    ADD COLUMN `deleted_at` DATETIME NULL AFTER `updated_at`;

-- mission_request: updated_at, deleted_at 추가 (이력성 테이블 - deleted_at 운영상 미사용)
ALTER TABLE `mission_request`
    ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `created_at`,
    ADD COLUMN `deleted_at` DATETIME NULL AFTER `updated_at`;

-- mission_log: updated_at, deleted_at 추가 (이력성 테이블 - deleted_at 운영상 미사용)
ALTER TABLE `mission_log`
    ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `created_at`,
    ADD COLUMN `deleted_at` DATETIME NULL AFTER `updated_at`;

-- reward_grant: updated_at, deleted_at 추가 (이력성 테이블 - deleted_at 운영상 미사용)
ALTER TABLE `reward_grant`
    ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `created_at`,
    ADD COLUMN `deleted_at` DATETIME NULL AFTER `updated_at`;

-- policy_appeal: deleted_at 추가 (이력성 테이블 - 운영상 미사용)
ALTER TABLE `policy_appeal`
    ADD COLUMN `deleted_at` DATETIME NULL AFTER `updated_at`;

-- policy_appeal_comment: updated_at 추가
ALTER TABLE `policy_appeal_comment`
    ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `created_at`;

-- family_recap_weekly: deleted_at 추가 (이력성 테이블 - 운영상 미사용)
ALTER TABLE `family_recap_weekly`
    ADD COLUMN `deleted_at` DATETIME NULL AFTER `updated_at`;

-- family_recap_monthly: deleted_at 추가 (이력성 테이블 - 운영상 미사용)
ALTER TABLE `family_recap_monthly`
    ADD COLUMN `deleted_at` DATETIME NULL AFTER `updated_at`;
