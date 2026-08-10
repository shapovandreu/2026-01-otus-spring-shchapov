ALTER TABLE operator_user
    ADD COLUMN org_id UUID REFERENCES organization(id) ON DELETE CASCADE;

CREATE INDEX idx_operator_user_org ON operator_user(org_id) WHERE org_id IS NOT NULL;
