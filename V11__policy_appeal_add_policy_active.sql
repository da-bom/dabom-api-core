-- Flyway V11: POLICY_APPEAL policy_active 컬럼 추가 (이의제기 승인 시 활성/비활성 반영)

ALTER TABLE `policy_appeal`
    ADD COLUMN `policy_active` BOOLEAN NULL COMMENT 'NORMAL 이의제기 시 요청한 정책 활성/비활성 의도';