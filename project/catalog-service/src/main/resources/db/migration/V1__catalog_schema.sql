CREATE TABLE product (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE release (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    version    TEXT NOT NULL,
    channel    TEXT NOT NULL DEFAULT 'STABLE' CHECK (channel IN ('STABLE', 'BETA')),
    published  BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (product_id, version)
);

CREATE TABLE image_ref (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    release_id UUID NOT NULL REFERENCES release(id) ON DELETE CASCADE,
    repository TEXT NOT NULL,
    digest     TEXT NOT NULL,
    CONSTRAINT digest_format CHECK (digest ~ '^sha256:[a-f0-9]{64}$')
);

CREATE INDEX idx_release_product   ON release(product_id, channel);
CREATE INDEX idx_image_ref_release ON image_ref(release_id);
