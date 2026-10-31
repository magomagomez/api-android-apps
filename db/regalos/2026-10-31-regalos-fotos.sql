-- regalos-api v0.5: one photo per wish (of a letter or a shared list) and per idea.
-- The photo itself lives in Google Drive; here only its file id and when it last changed,
-- which the app uses to refresh its image cache. Idempotent: safe to run twice.

ALTER TABLE regalos_items ADD COLUMN IF NOT EXISTS photo_file_id VARCHAR(100);
ALTER TABLE regalos_items ADD COLUMN IF NOT EXISTS photo_updated_at TIMESTAMPTZ;

ALTER TABLE regalos_ideas ADD COLUMN IF NOT EXISTS photo_file_id VARCHAR(100);
ALTER TABLE regalos_ideas ADD COLUMN IF NOT EXISTS photo_updated_at TIMESTAMPTZ;
