CREATE TABLE intent (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id            UUID NOT NULL,
    installation_id   UUID NOT NULL,
    product_id        UUID NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    action            TEXT NOT NULL CHECK (action IN ('INSTALL', 'UPDATE', 'ROLLBACK')),
    target_release_id UUID NOT NULL REFERENCES release(id),
    status            TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'CONSUMED')),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_intent_org_status ON intent(org_id, status, created_at);
