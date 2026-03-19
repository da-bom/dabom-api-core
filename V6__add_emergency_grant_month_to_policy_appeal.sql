ALTER TABLE `policy_appeal`
    ADD COLUMN `emergency_grant_month` DATE NULL;

ALTER TABLE `policy_appeal`
    ADD CONSTRAINT `uk_policy_appeal_emergency_month`
        UNIQUE (`requester_id`, `emergency_grant_month`);
