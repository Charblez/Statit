ALTER TABLE categories ADD COLUMN IF NOT EXISTS lower_limit double precision DEFAULT 0.0;
UPDATE categories SET lower_limit = 0.0 WHERE lower_limit IS NULL;
ALTER TABLE categories ALTER COLUMN lower_limit SET DEFAULT 0.0;
ALTER TABLE categories ALTER COLUMN lower_limit SET NOT NULL;

ALTER TABLE categories ADD COLUMN IF NOT EXISTS upper_limit double precision DEFAULT 100.0;
UPDATE categories SET upper_limit = 100.0 WHERE upper_limit IS NULL;
ALTER TABLE categories ALTER COLUMN upper_limit SET DEFAULT 100.0;
ALTER TABLE categories ALTER COLUMN upper_limit SET NOT NULL;

ALTER TABLE categories ADD COLUMN IF NOT EXISTS image_data text;
