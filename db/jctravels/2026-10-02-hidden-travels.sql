-- JC Travels: trips can be hidden from the apps (the 9999 "USA" template).
-- Run BEFORE deploying the code that reads travels.hidden; the old code ignores the column.
-- heroku pg:psql -a api-android-app < db/jctravels/2026-10-02-hidden-travels.sql

BEGIN;
ALTER TABLE travels ADD COLUMN IF NOT EXISTS hidden boolean NOT NULL DEFAULT false;
UPDATE travels SET hidden = true WHERE id = 9999;
COMMIT;

-- Rollback (only after reverting the code that reads the column):
-- ALTER TABLE travels DROP COLUMN hidden;
