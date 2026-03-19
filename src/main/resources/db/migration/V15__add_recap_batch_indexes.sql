-- ============================================================
-- Flyway V15: recap batch 성능 개선용 인덱스 추가
-- weekly/monthly family recap 집계에서 사용하는 created/resolved/completed 범위 조회를 최적화한다.
-- ============================================================

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'mission_item'
              AND INDEX_NAME = 'idx_mission_recap_family_created'
        ),
        'SELECT 1',
        'CREATE INDEX idx_mission_recap_family_created ON mission_item (family_id, created_at, deleted_at)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'mission_item'
              AND INDEX_NAME = 'idx_mission_recap_family_completed'
        ),
        'SELECT 1',
        'CREATE INDEX idx_mission_recap_family_completed ON mission_item (family_id, status, completed_at, deleted_at)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'mission_request'
              AND INDEX_NAME = 'idx_mreq_recap_item_status_resolved'
        ),
        'SELECT 1',
        'CREATE INDEX idx_mreq_recap_item_status_resolved ON mission_request (mission_item_id, status, resolved_at, deleted_at)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'mission_log'
              AND INDEX_NAME = 'idx_mission_log_recap_item_action_created'
        ),
        'SELECT 1',
        'CREATE INDEX idx_mission_log_recap_item_action_created ON mission_log (mission_item_id, action_type, created_at, deleted_at)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'policy_appeal'
              AND INDEX_NAME = 'idx_appeal_recap_assignment_type_created'
        ),
        'SELECT 1',
        'CREATE INDEX idx_appeal_recap_assignment_type_created ON policy_appeal (policy_assignment_id, type, created_at, deleted_at)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'policy_appeal'
              AND INDEX_NAME = 'idx_appeal_recap_assignment_type_status_resolved'
        ),
        'SELECT 1',
        'CREATE INDEX idx_appeal_recap_assignment_type_status_resolved ON policy_appeal (policy_assignment_id, type, status, resolved_at, deleted_at)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
