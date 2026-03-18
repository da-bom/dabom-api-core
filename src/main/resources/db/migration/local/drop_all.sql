-- 생성기 전용: 전체 테이블 DROP (Flyway에서는 사용하지 않음)
-- FK 제약 무시로 순서 무관하게 안전한 DROP 보장

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `family_recap_monthly`;
DROP TABLE IF EXISTS `family_recap_weekly`;
DROP TABLE IF EXISTS `policy_appeal_comment`;
DROP TABLE IF EXISTS `reward_grant`;
DROP TABLE IF EXISTS `mission_log`;
DROP TABLE IF EXISTS `mission_request`;
DROP TABLE IF EXISTS `policy_appeal`;
DROP TABLE IF EXISTS `notification_log`;
DROP TABLE IF EXISTS `policy_assignment`;
DROP TABLE IF EXISTS `mission_item`;
DROP TABLE IF EXISTS `reward`;
DROP TABLE IF EXISTS `reward_template`;
DROP TABLE IF EXISTS `usage_event_outbox`;
DROP TABLE IF EXISTS `usage_record`;
DROP TABLE IF EXISTS `customer_quota`;
DROP TABLE IF EXISTS `family_quota`;
DROP TABLE IF EXISTS `family_member`;
DROP TABLE IF EXISTS `family`;
DROP TABLE IF EXISTS `policy`;
DROP TABLE IF EXISTS `invite`;
DROP TABLE IF EXISTS `audit_log`;
DROP TABLE IF EXISTS `admin`;
DROP TABLE IF EXISTS `customer`;

-- Flyway 메타데이터 (생성기 재실행 시 클린 리셋)
DROP TABLE IF EXISTS `flyway_schema_history`;

SET FOREIGN_KEY_CHECKS = 1;
