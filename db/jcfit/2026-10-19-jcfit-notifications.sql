-- JCFit, migración 4: dispositivos de la app (tokens de Firebase Cloud Messaging) y avisos
-- enviados desde el backoffice (contrato: jcfit-android/tasks/contract.md, "Notificaciones").
-- Va después de 2026-10-19-jcfit-shared-users.sql. Idempotente. Solo crea tablas nuevas.
--
-- Ejecutar antes de desplegar el código de notificaciones:
--   heroku pg:psql -a api-android-app < db/jcfit/2026-10-19-jcfit-notifications.sql

BEGIN;

-- Un token por instalación de la app; borrar la cuenta borra sus dispositivos.
CREATE TABLE IF NOT EXISTS jcfit_devices (
    token      text        PRIMARY KEY CHECK (length(token) BETWEEN 1 AND 4096),
    user_id    integer     NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS jcfit_notifications (
    id        serial      PRIMARY KEY,
    title     text        NOT NULL CHECK (length(title) BETWEEN 1 AND 60),
    body      text        NOT NULL CHECK (length(body) BETWEEN 1 AND 300),
    sent_at   timestamptz NOT NULL DEFAULT now(),
    delivered integer     NOT NULL DEFAULT 0 CHECK (delivered >= 0),
    failed    integer     NOT NULL DEFAULT 0 CHECK (failed >= 0)
);
CREATE INDEX IF NOT EXISTS jcfit_notifications_sent_at ON jcfit_notifications (sent_at DESC);

COMMIT;
