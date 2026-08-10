CREATE TABLE entitlement (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id        UUID NOT NULL,
    product_id    UUID NOT NULL,
    channel       TEXT NOT NULL DEFAULT 'stable',
    max_instances INT  NOT NULL DEFAULT 1 CHECK (max_instances >= 0),
    valid_until   TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (org_id, product_id, channel)
);

CREATE TABLE license (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    installation_id  UUID NOT NULL UNIQUE,
    org_id           UUID NOT NULL,
    product_id       UUID NOT NULL,
    valid_until      TIMESTAMPTZ NOT NULL,
    grace_until      TIMESTAMPTZ NOT NULL,
    on_expiry_policy TEXT NOT NULL DEFAULT 'BLOCK',
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT grace_after_valid CHECK (grace_until >= valid_until)
);

CREATE INDEX idx_entitlement_org ON entitlement(org_id);
CREATE INDEX idx_license_org     ON license(org_id);
