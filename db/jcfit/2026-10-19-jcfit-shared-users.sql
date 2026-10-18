-- JCFit, migración 2: el login usa las cuentas de la tabla users (las de Escapers Things)
-- en vez de jcfit_users; solo entra la cuenta con id = JCFIT_USER_ID (config var).
-- Va después de 2026-10-18-jcfit.sql. Idempotente: ejecutarla otra vez no cambia nada.
--
-- Datos que cambia:
-- - Borra las sesiones de jcfit_tokens (apuntaban a jcfit_users): hay que volver a entrar.
-- - jcfit_tokens.user_id pasa a apuntar a users (id); borrar una cuenta borra sus sesiones.
-- - Borra jcfit_users, que ya no se usa.
--
-- Ejecutar antes de desplegar el código que lee users:
--   heroku pg:psql -a api-android-app < db/jcfit/2026-10-19-jcfit-shared-users.sql

BEGIN;

DELETE FROM jcfit_tokens
 WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'jcfit_users');

ALTER TABLE jcfit_tokens DROP CONSTRAINT IF EXISTS jcfit_tokens_user_id_fkey;
ALTER TABLE jcfit_tokens
    ADD CONSTRAINT jcfit_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

DROP TABLE IF EXISTS jcfit_users;

COMMIT;
