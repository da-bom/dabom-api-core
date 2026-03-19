ALTER TABLE `mission_request`
    ADD COLUMN `active_request_mission_id` BIGINT NULL;

UPDATE `mission_request`
SET `active_request_mission_id` = `mission_item_id`
WHERE `status` = 'PENDING';

ALTER TABLE `mission_request`
    ADD CONSTRAINT `uk_mission_request_active_request_mission`
        UNIQUE (`active_request_mission_id`);
