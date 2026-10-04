CREATE TABLE areas (
    id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name   VARCHAR(100) NOT NULL,
    description TEXT,
    color  VARCHAR(7),
    icon   VARCHAR(50),
    position INT  NOT NULL DEFAULT 0,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_areas_color CHECK (color IS NULL OR color ~ '^#[0-9A-Fa-f]{6}$')
    );
    CREATE UNIQUE INDEX uq_areas_name_lower ON areas (lower(name));