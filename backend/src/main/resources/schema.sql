ALTER TABLE categories ADD COLUMN IF NOT EXISTS lower_limit double precision DEFAULT 0.0;
UPDATE categories SET lower_limit = 0.0 WHERE lower_limit IS NULL;
ALTER TABLE categories ALTER COLUMN lower_limit SET DEFAULT 0.0;
ALTER TABLE categories ALTER COLUMN lower_limit SET NOT NULL;

ALTER TABLE categories ADD COLUMN IF NOT EXISTS upper_limit double precision DEFAULT 100.0;
UPDATE categories SET upper_limit = 100.0 WHERE upper_limit IS NULL;
ALTER TABLE categories ALTER COLUMN upper_limit SET DEFAULT 100.0;
ALTER TABLE categories ALTER COLUMN upper_limit SET NOT NULL;

ALTER TABLE categories ADD COLUMN IF NOT EXISTS image_data text;

ALTER TABLE categories ADD COLUMN IF NOT EXISTS global_source_key varchar(255);

ALTER TABLE categories ADD COLUMN IF NOT EXISTS category_scope varchar(16) DEFAULT 'LOCAL';
UPDATE categories SET category_scope = 'LOCAL' WHERE category_scope IS NULL;
UPDATE categories SET category_scope = 'LOCAL' WHERE global_source_key IS NULL;
ALTER TABLE categories ALTER COLUMN category_scope SET DEFAULT 'LOCAL';
ALTER TABLE categories ALTER COLUMN category_scope SET NOT NULL;

UPDATE categories SET category_scope = 'LOCAL' WHERE global_source_key IS NULL;
ALTER TABLE categories ALTER COLUMN founding_user_id DROP NOT NULL;

CREATE TABLE IF NOT EXISTS global_dataset_points (
    point_id uuid PRIMARY KEY,
    category_id uuid NOT NULL REFERENCES categories(category_id) ON DELETE CASCADE,
    source_name varchar(255) NOT NULL,
    source_participant_id varchar(64) NOT NULL,
    value double precision NOT NULL,
    demographics jsonb DEFAULT '{}'::jsonb,
    created_at timestamp DEFAULT current_timestamp,
    CONSTRAINT uq_global_dataset_point UNIQUE (category_id, source_name, source_participant_id)
);

CREATE INDEX IF NOT EXISTS idx_global_dataset_points_category
    ON global_dataset_points(category_id);

CREATE INDEX IF NOT EXISTS idx_global_dataset_points_demographics
    ON global_dataset_points USING gin(demographics);
