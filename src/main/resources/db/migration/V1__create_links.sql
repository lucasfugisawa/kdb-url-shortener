CREATE TABLE IF NOT EXISTS links (
  id BIGSERIAL PRIMARY KEY,
  slug VARCHAR(32) NOT NULL,
  target_url TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  expires_at TIMESTAMPTZ NULL
);

-- Ensure a uniquely named index for slug as requested
CREATE UNIQUE INDEX IF NOT EXISTS links_slug_uindex ON links(slug);
