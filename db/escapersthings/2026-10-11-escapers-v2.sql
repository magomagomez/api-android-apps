-- Escapers Things v2, migración A (contrato: escapersThings-android/tasks/contract.md).
-- Añade lo que necesita escapers-api/v2 sin quitar nada que use v1: password_hash,
-- tokens, jugadores por sala y fotos; limpia datos de prueba y notas huérfanas, normaliza
-- categorías y ciudades, rellena jugadores a partir de las notas y de done/javi_done/cris_done
-- y pone claves ajenas a las notas. Idempotente: ejecutarla otra vez no cambia nada.
--
-- Ensayo (BEGIN, migración, recuentos, ROLLBACK): node db/escapersthings/dry-run.js
-- Prueba local sin Postgres: db/escapersthings/test/pglite-test.js
-- Ejecutar (después de heroku pg:backups:capture y ANTES de desplegar el código de v2):
--   heroku pg:psql -a api-android-app < db/escapersthings/2026-10-11-escapers-v2.sql
--
-- Datos que cambia:
-- - Borra las salas de prueba 168, 217 y 218 y sus notas.
-- - Borra las notas de salas inexistentes (0, 82, 83, 84, 200) o de usuarios inexistentes.
-- - escape_rooms.type: recorta espacios; Cinefilo -> Cinéfilo, Aventuras -> Aventura,
--   Battle Royal -> Battle Royale. Si queda alguna fuera de la lista cerrada, aborta.
-- - escape_rooms.ubication: recorta espacios ('' -> null) y corrige:
--     Barceona -> Barcelona, Hospitalet -> L'Hospitalet, Cornella -> Cornellà,
--     Rubi -> Rubí, Esparraguera -> Esparreguera.
-- - escape_room_players: quien tiene nota; done = 1 -> Magomez (1) y Cris (2);
--   javi_done = 1 -> 1; cris_done = 1 -> 2; Misterio de Kemet y Olimpo -> 1-5;
--   Exorcismo e In -> 1, 2, 3 y 5 (Sara, 4, no fue). played_on queda null.

BEGIN;

-- 1. Limpieza ---------------------------------------------------------------------------

DELETE FROM user_categoria_nota WHERE escape_room_id IN (168, 217, 218);
DELETE FROM escape_rooms WHERE id IN (168, 217, 218);

DELETE FROM user_categoria_nota n
 WHERE NOT EXISTS (SELECT 1 FROM escape_rooms e WHERE e.id = n.escape_room_id)
    OR NOT EXISTS (SELECT 1 FROM users u WHERE u.id = n.user_id);

-- 2. Categorías a la lista cerrada ------------------------------------------------------

UPDATE escape_rooms SET type = trim(type) WHERE type <> trim(type);

UPDATE escape_rooms e
   SET type = m.nuevo
  FROM (VALUES ('Cinefilo',     'Cinéfilo'),
               ('Aventuras',    'Aventura'),
               ('Battle Royal', 'Battle Royale')) AS m (viejo, nuevo)
 WHERE e.type = m.viejo;

DO $$
DECLARE
    fuera text;
BEGIN
    SELECT string_agg(DISTINCT type, ', ') INTO fuera
      FROM escape_rooms
     WHERE type NOT IN ('Aventura', 'Terror', 'Cinéfilo', 'Tensión', 'Histórico',
                        'Hall Escape', 'Battle Royale');
    IF fuera IS NOT NULL THEN
        RAISE EXCEPTION 'Categorías fuera de la lista cerrada: %', fuera;
    END IF;
END $$;

-- 3. Ciudades ---------------------------------------------------------------------------

UPDATE escape_rooms
   SET ubication = nullif(trim(ubication), '')
 WHERE ubication IS DISTINCT FROM nullif(trim(ubication), '');

UPDATE escape_rooms e
   SET ubication = m.nueva
  FROM (VALUES ('Barceona',     'Barcelona'),
               ('Hospitalet',   'L''Hospitalet'),
               ('Cornella',     'Cornellà'),
               ('Rubi',         'Rubí'),
               ('Esparraguera', 'Esparreguera')) AS m (vieja, nueva)
 WHERE e.ubication = m.vieja;

-- 4. Esquema nuevo ----------------------------------------------------------------------

ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash varchar(200);

CREATE TABLE IF NOT EXISTS escapers_tokens (
    token_hash   char(64)    PRIMARY KEY,                 -- SHA-256 en hex del token
    user_id      integer     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at   timestamptz NOT NULL DEFAULT now(),
    last_used_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS escape_room_players (
    escape_room_id integer NOT NULL REFERENCES escape_rooms(id) ON DELETE CASCADE,
    user_id        integer NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    played_on      date,
    PRIMARY KEY (escape_room_id, user_id)
);

CREATE TABLE IF NOT EXISTS escape_room_photos (
    escape_room_id integer     NOT NULL REFERENCES escape_rooms(id) ON DELETE CASCADE,
    kind           varchar(10) NOT NULL CHECK (kind IN ('poster', 'group')),
    content_type   varchar(30) NOT NULL,                  -- image/jpeg | image/png | image/webp
    data           bytea       NOT NULL,
    etag           char(64)    NOT NULL,                  -- SHA-256 en hex de data
    updated_at     timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (escape_room_id, kind)
);

-- Las notas ya no tienen huérfanas: se les pueden poner claves ajenas.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                    WHERE conname = 'user_categoria_nota_escape_room_fk') THEN
        ALTER TABLE user_categoria_nota
            ADD CONSTRAINT user_categoria_nota_escape_room_fk
            FOREIGN KEY (escape_room_id) REFERENCES escape_rooms(id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                    WHERE conname = 'user_categoria_nota_user_fk') THEN
        ALTER TABLE user_categoria_nota
            ADD CONSTRAINT user_categoria_nota_user_fk
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;
END $$;

-- 5. Jugadores --------------------------------------------------------------------------

INSERT INTO escape_room_players (escape_room_id, user_id)
SELECT DISTINCT escape_room_id, user_id FROM user_categoria_nota
ON CONFLICT DO NOTHING;

INSERT INTO escape_room_players (escape_room_id, user_id)
SELECT e.id, u.id
  FROM escape_rooms e
  JOIN users u ON (e.done = 1      AND u.id IN (1, 2))
               OR (e.javi_done = 1 AND u.id = 1)
               OR (e.cris_done = 1 AND u.id = 2)
ON CONFLICT DO NOTHING;

INSERT INTO escape_room_players (escape_room_id, user_id)
SELECT e.id, u.id
  FROM escape_rooms e
  JOIN (VALUES ('misterio de kemet', ARRAY[1, 2, 3, 4, 5]),
               ('olimpo',            ARRAY[1, 2, 3, 4, 5]),
               ('exorcismo',         ARRAY[1, 2, 3, 5]),
               ('in',                ARRAY[1, 2, 3, 5])) AS s (sala, jugadores)
    ON lower(trim(e.name)) = s.sala
  JOIN users u ON u.id = ANY (s.jugadores)
ON CONFLICT DO NOTHING;

COMMIT;

-- Rollback del esquema (solo antes de desplegar v2 o tras revertirla). Los borrados y las
-- normalizaciones de datos no se deshacen: para eso, restaurar el backup previo.
-- BEGIN;
-- ALTER TABLE user_categoria_nota DROP CONSTRAINT IF EXISTS user_categoria_nota_escape_room_fk;
-- ALTER TABLE user_categoria_nota DROP CONSTRAINT IF EXISTS user_categoria_nota_user_fk;
-- DROP TABLE IF EXISTS escape_room_photos;
-- DROP TABLE IF EXISTS escape_room_players;
-- DROP TABLE IF EXISTS escapers_tokens;
-- ALTER TABLE users DROP COLUMN IF EXISTS password_hash;
-- COMMIT;
