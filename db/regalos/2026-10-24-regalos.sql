-- Correo de Regalos, migración inicial (contrato: regalos-android/tasks/contract.md).
-- Crea las tablas de regalos-api/v1, todas con el prefijo regalos_. No toca nada de otros
-- módulos (sus usuarios son propios, no los de la tabla users). Idempotente: ejecutarla otra
-- vez no cambia nada.
--
-- Ejecutar ANTES de desplegar el código de regalos-api (con un backup previo):
--   heroku pg:backups:capture -a api-android-app
--   heroku pg:psql -a api-android-app < db/regalos/2026-10-24-regalos.sql

BEGIN;

-- Usuarios y sesiones -----------------------------------------------------------------

-- username en minúsculas (lo garantiza la app y el CHECK). password_hash: formato de
-- RegalosPasswordHasher (pbkdf2$<iter>$<salt>$<hash>). Cumple opcional, sin año.
CREATE TABLE IF NOT EXISTS regalos_users (
    id             serial      PRIMARY KEY,
    username       text        NOT NULL UNIQUE CHECK (username ~ '^[a-z0-9._-]{3,40}$'),
    display_name   text        NOT NULL CHECK (length(trim(display_name)) BETWEEN 1 AND 40),
    password_hash  text        NOT NULL,
    birthday_day   smallint    CHECK (birthday_day BETWEEN 1 AND 31),
    birthday_month smallint    CHECK (birthday_month BETWEEN 1 AND 12),
    created_at     timestamptz NOT NULL DEFAULT now(),
    CHECK ((birthday_day IS NULL) = (birthday_month IS NULL))
);

-- Sesiones por el SHA-256 del token (nunca el token).
CREATE TABLE IF NOT EXISTS regalos_tokens (
    token_hash   text        PRIMARY KEY,
    user_id      integer     NOT NULL REFERENCES regalos_users (id) ON DELETE CASCADE,
    created_at   timestamptz NOT NULL DEFAULT now(),
    last_used_at timestamptz NOT NULL DEFAULT now()
);

-- Deseos y marcas ---------------------------------------------------------------------

-- Borrado lógico (DELETED) solo cuando el deseo tiene marca, para avisar a quien regala.
CREATE TABLE IF NOT EXISTS regalos_items (
    id            bigserial   PRIMARY KEY,
    owner_user_id integer     NOT NULL REFERENCES regalos_users (id) ON DELETE CASCADE,
    title         text        NOT NULL CHECK (length(trim(title)) BETWEEN 1 AND 120),
    url           text        CHECK (length(url) <= 2048),
    store         text        CHECK (length(store) <= 80),
    price_cents   integer     CHECK (price_cents BETWEEN 0 AND 100000000),
    image_url     text        CHECK (length(image_url) <= 2048),
    notes         text        CHECK (length(notes) <= 500),
    wanted_a_lot  boolean     NOT NULL DEFAULT false,
    status        text        NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'RECEIVED', 'DELETED')),
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS regalos_items_owner_idx ON regalos_items (owner_user_id, status);

-- Como mucho una marca por deseo: la clave primaria sobre item_id lo impide (regla R2).
-- Quien regala es alguien de la app (giver_user_id) o un nombre libre (giver_name).
CREATE TABLE IF NOT EXISTS regalos_claims (
    item_id           bigint      PRIMARY KEY REFERENCES regalos_items (id) ON DELETE CASCADE,
    marked_by_user_id integer     NOT NULL REFERENCES regalos_users (id) ON DELETE CASCADE,
    giver_user_id     integer     REFERENCES regalos_users (id) ON DELETE CASCADE,
    giver_name        text        CHECK (length(trim(giver_name)) BETWEEN 1 AND 60),
    state             text        NOT NULL CHECK (state IN ('RESERVED', 'BOUGHT')),
    item_edited_at    timestamptz,
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now(),
    CHECK ((giver_user_id IS NULL) <> (giver_name IS NULL))
);
CREATE INDEX IF NOT EXISTS regalos_claims_giver_idx ON regalos_claims (giver_user_id);
CREATE INDEX IF NOT EXISTS regalos_claims_marked_by_idx ON regalos_claims (marked_by_user_id);

COMMIT;
