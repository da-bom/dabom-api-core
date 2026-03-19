-- ============================================================
-- Flyway V7: MISSION_LOG action_type ENUM 재정의 (ERD v21.3)
-- 역할 분리 원칙: 요청 처리 결과는 mission_request.status가 담당,
-- 미션 상태 변화 타임라인은 mission_log.action_type이 담당.
-- MISSION_APPROVED·MISSION_REJECTED 제거, MISSION_CANCELLED 추가
-- ============================================================

ALTER TABLE `mission_log`
    MODIFY COLUMN `action_type` ENUM('MISSION_CREATED', 'MISSION_REQUESTED', 'MISSION_COMPLETED', 'MISSION_CANCELLED') NOT NULL;
