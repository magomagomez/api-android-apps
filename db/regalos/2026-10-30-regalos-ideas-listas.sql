-- Correo de Regalos, migración 2: ideas privadas y listas para alguien de fuera
-- (contrato: regalos-android/tasks/contract.md, sección v0.4). Idempotente.
--
-- Ejecutar ANTES de desplegar el código que la usa, con un backup previo:
--   heroku pg:backups:capture -a api-android-app

BEGIN;

-- Ideas: tabla aparte, siempre filtrada por su autor (regla R7). Nunca se cruza con las
-- consultas de la carta del dueño ni con las de los demás.
CREATE TABLE IF NOT EXISTS regalos_ideas (
    id             bigserial   PRIMARY KEY,
    author_user_id integer     NOT NULL REFERENCES regalos_users (id) ON DELETE CASCADE,
    for_user_id    integer     NOT NULL REFERENCES regalos_users (id) ON DELETE CASCADE,
    title          text        NOT NULL CHECK (length(trim(title)) BETWEEN 1 AND 120),
    url            text        CHECK (length(url) <= 2048),
    store          text        CHECK (length(store) <= 80),
    price_cents    integer     CHECK (price_cents BETWEEN 0 AND 100000000),
    notes          text        CHECK (length(notes) <= 500),
    bought         boolean     NOT NULL DEFAULT false,
    created_at     timestamptz NOT NULL DEFAULT now(),
    updated_at     timestamptz NOT NULL DEFAULT now(),
    CHECK (author_user_id <> for_user_id)
);
CREATE INDEX IF NOT EXISTS regalos_ideas_author_idx ON regalos_ideas (author_user_id, for_user_id);

-- Listas para alguien que no usa la app, y quién está invitado (quien la crea, también).
CREATE TABLE IF NOT EXISTS regalos_lists (
    id                 bigserial   PRIMARY KEY,
    name               text        NOT NULL CHECK (length(trim(name)) BETWEEN 1 AND 60),
    birthday_day       smallint    CHECK (birthday_day BETWEEN 1 AND 31),
    birthday_month     smallint    CHECK (birthday_month BETWEEN 1 AND 12),
    created_by_user_id integer     NOT NULL REFERENCES regalos_users (id) ON DELETE CASCADE,
    created_at         timestamptz NOT NULL DEFAULT now(),
    CHECK ((birthday_day IS NULL) = (birthday_month IS NULL))
);

CREATE TABLE IF NOT EXISTS regalos_list_members (
    list_id bigint  NOT NULL REFERENCES regalos_lists (id) ON DELETE CASCADE,
    user_id integer NOT NULL REFERENCES regalos_users (id) ON DELETE CASCADE,
    PRIMARY KEY (list_id, user_id)
);
CREATE INDEX IF NOT EXISTS regalos_list_members_user_idx ON regalos_list_members (user_id);

-- Los deseos de una lista son regalos_items sin dueño: así reutilizan las marcas
-- (regalos_claims) y sus reglas. Un deseo es de una persona o de una lista, nunca de las dos.
ALTER TABLE regalos_items ALTER COLUMN owner_user_id DROP NOT NULL;
ALTER TABLE regalos_items ADD COLUMN IF NOT EXISTS list_id bigint REFERENCES regalos_lists (id) ON DELETE CASCADE;
ALTER TABLE regalos_items ADD COLUMN IF NOT EXISTS added_by_user_id integer REFERENCES regalos_users (id) ON DELETE SET NULL;
ALTER TABLE regalos_items DROP CONSTRAINT IF EXISTS regalos_items_owner_or_list;
ALTER TABLE regalos_items ADD CONSTRAINT regalos_items_owner_or_list CHECK ((owner_user_id IS NULL) <> (list_id IS NULL));
CREATE INDEX IF NOT EXISTS regalos_items_list_idx ON regalos_items (list_id, status);

COMMIT;
