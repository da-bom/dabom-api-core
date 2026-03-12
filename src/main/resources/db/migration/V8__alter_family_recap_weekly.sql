ALTER TABLE `family_recap_weekly`
CHANGE COLUMN `appeal_count` `total_appeal_count` INT NOT NULL DEFAULT 0,
ADD COLUMN `approved_appeal_count` INT NOT NULL DEFAULT 0 AFTER `total_appeal_count`,
ADD COLUMN `rejected_appeal_count` INT NOT NULL DEFAULT 0 AFTER `approved_appeal_count`;