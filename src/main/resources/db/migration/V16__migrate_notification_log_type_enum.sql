-- ============================================================
-- Flyway V16: notification_log type ENUM 전환
-- 기존 BLOCKED/UNBLOCKED 값을 CUSTOMER_BLOCKED/CUSTOMER_UNBLOCKED로 치환하고
-- ADMIN_PUSH를 신규 ENUM 값으로 추가한다.
-- ============================================================

ALTER TABLE notification_log
    MODIFY COLUMN type ENUM(
        'THRESHOLD_ALERT',
        'BLOCKED',
        'UNBLOCKED',
        'CUSTOMER_BLOCKED',
        'CUSTOMER_UNBLOCKED',
        'POLICY_CHANGED',
        'MISSION_CREATED',
        'REWARD_REQUESTED',
        'REWARD_APPROVED',
        'REWARD_REJECTED',
        'APPEAL_CREATED',
        'APPEAL_APPROVED',
        'APPEAL_REJECTED',
        'EMERGENCY_APPROVED',
        'ADMIN_PUSH'
    ) NOT NULL;

UPDATE notification_log
SET type = 'CUSTOMER_BLOCKED'
WHERE type = 'BLOCKED';

UPDATE notification_log
SET type = 'CUSTOMER_UNBLOCKED'
WHERE type = 'UNBLOCKED';

ALTER TABLE notification_log
    MODIFY COLUMN type ENUM(
        'THRESHOLD_ALERT',
        'CUSTOMER_BLOCKED',
        'CUSTOMER_UNBLOCKED',
        'POLICY_CHANGED',
        'MISSION_CREATED',
        'REWARD_REQUESTED',
        'REWARD_APPROVED',
        'REWARD_REJECTED',
        'APPEAL_CREATED',
        'APPEAL_APPROVED',
        'APPEAL_REJECTED',
        'EMERGENCY_APPROVED',
        'ADMIN_PUSH'
    ) NOT NULL;
