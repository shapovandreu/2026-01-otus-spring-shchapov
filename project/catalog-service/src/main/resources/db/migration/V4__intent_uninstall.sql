ALTER TABLE intent DROP CONSTRAINT IF EXISTS intent_action_check;
ALTER TABLE intent ADD CONSTRAINT intent_action_check
    CHECK (action IN ('INSTALL', 'UPDATE', 'ROLLBACK', 'UNINSTALL'));

ALTER TABLE intent ALTER COLUMN target_release_id DROP NOT NULL;

ALTER TABLE intent ADD CONSTRAINT intent_target_release_required
    CHECK ((action = 'UNINSTALL' AND target_release_id IS NULL)
        OR (action <> 'UNINSTALL' AND target_release_id IS NOT NULL));
