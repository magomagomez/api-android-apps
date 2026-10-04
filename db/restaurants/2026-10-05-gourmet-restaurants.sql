-- JC Gourmet: restaurants to try and tried, with one shared visit (date, 1-10 rating in half
-- points, comment). Run BEFORE deploying the code that reads gourmet_restaurants.
-- heroku pg:psql -a api-android-app < db/restaurants/2026-10-05-gourmet-restaurants.sql

BEGIN;
CREATE TABLE IF NOT EXISTS gourmet_restaurants (
    id               serial PRIMARY KEY,
    name             varchar(120) NOT NULL,
    city             varchar(80)  NOT NULL,
    cuisine          varchar(80),
    price_level      smallint CHECK (price_level BETWEEN 1 AND 4),
    website          varchar(500),
    location         varchar(500),
    drive_folder_url varchar(500),
    why_go           text,
    visited_on       date,
    rating           numeric(3,1) CHECK (rating BETWEEN 1 AND 10 AND rating * 2 = trunc(rating * 2)),
    comment          text,
    created_at       timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT gourmet_visit_complete CHECK ((visited_on IS NULL) = (rating IS NULL))
);
COMMIT;

-- Rollback (only after reverting the code that reads the table):
-- DROP TABLE gourmet_restaurants;
