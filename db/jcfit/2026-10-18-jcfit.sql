-- JCFit, migración inicial (contrato: jcfit-android/tasks/contract.md).
-- Crea las tablas de jcfit-api/v1, todas con el prefijo jcfit_. No toca nada de otros
-- módulos. Idempotente: ejecutarla otra vez no cambia nada.
--
-- Ejecutar ANTES de desplegar el código de jcfit-api:
--   heroku pg:psql -a api-android-app < db/jcfit/2026-10-18-jcfit.sql
-- El usuario no se crea aquí (ver jcfit_users).

BEGIN;

-- Login -------------------------------------------------------------------------------

-- Un único usuario. password_hash: formato de PasswordHasher (pbkdf2$<iter>$<salt>$<hash>).
CREATE TABLE IF NOT EXISTS jcfit_users (
    id            serial      PRIMARY KEY,
    username      text        NOT NULL UNIQUE,
    password_hash text        NOT NULL,
    created_at    timestamptz NOT NULL DEFAULT now()
);

-- Sesiones por el SHA-256 del token (nunca el token).
CREATE TABLE IF NOT EXISTS jcfit_tokens (
    token_hash   text        PRIMARY KEY,
    user_id      integer     NOT NULL REFERENCES jcfit_users (id) ON DELETE CASCADE,
    created_at   timestamptz NOT NULL DEFAULT now(),
    last_used_at timestamptz NOT NULL DEFAULT now()
);

-- Catálogo y programa ------------------------------------------------------------------

-- Los ejercicios no se borran (las series apuntan a ellos): se archivan.
CREATE TABLE IF NOT EXISTS jcfit_exercises (
    id           serial      PRIMARY KEY,
    name         text        NOT NULL CHECK (length(trim(name)) BETWEEN 1 AND 80),
    youtube_url  text,
    instructions text        CHECK (length(instructions) <= 1000),
    muscle_group text        CHECK (length(muscle_group) <= 60),
    archived     boolean     NOT NULL DEFAULT false,
    created_at   timestamptz NOT NULL DEFAULT now()
);

-- Plantilla semanal que se repite, sin fecha de fin. Solo hay uno activo.
CREATE TABLE IF NOT EXISTS jcfit_programs (
    id         serial      PRIMARY KEY,
    name       text        NOT NULL CHECK (length(trim(name)) BETWEEN 1 AND 80),
    start_date date        NOT NULL,
    active     boolean     NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS jcfit_programs_one_active ON jcfit_programs (active) WHERE active;

-- Un entreno por día de la semana (1 = lunes ... 7 = domingo). Sin fila = descanso.
CREATE TABLE IF NOT EXISTS jcfit_program_days (
    id          serial   PRIMARY KEY,
    program_id  integer  NOT NULL REFERENCES jcfit_programs (id) ON DELETE CASCADE,
    day_of_week smallint NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    name        text     NOT NULL CHECK (length(trim(name)) BETWEEN 1 AND 60),
    muscles     text     CHECK (length(muscles) <= 120),
    UNIQUE (program_id, day_of_week)
);

CREATE TABLE IF NOT EXISTS jcfit_program_exercises (
    id             serial   PRIMARY KEY,
    program_day_id integer  NOT NULL REFERENCES jcfit_program_days (id) ON DELETE CASCADE,
    exercise_id    integer  NOT NULL REFERENCES jcfit_exercises (id),
    position       smallint NOT NULL CHECK (position >= 1),
    sets           smallint NOT NULL CHECK (sets BETWEEN 1 AND 20),
    reps_min       smallint NOT NULL CHECK (reps_min BETWEEN 1 AND 100),
    reps_max       smallint NOT NULL CHECK (reps_max BETWEEN reps_min AND 100),
    rest_seconds   smallint CHECK (rest_seconds BETWEEN 0 AND 900),
    UNIQUE (program_day_id, position)
);

-- Registro de entrenos -----------------------------------------------------------------

-- day_name guarda el nombre del entreno, para que el historial sobreviva si se cambia
-- o se quita el día del programa (program_day_id pasa a null).
CREATE TABLE IF NOT EXISTS jcfit_workout_sessions (
    id             serial      PRIMARY KEY,
    program_day_id integer     REFERENCES jcfit_program_days (id) ON DELETE SET NULL,
    day_name       text        NOT NULL,
    session_date   date        NOT NULL,
    started_at     timestamptz NOT NULL DEFAULT now(),
    finished_at    timestamptz
);
CREATE UNIQUE INDEX IF NOT EXISTS jcfit_workout_sessions_day_date
    ON jcfit_workout_sessions (program_day_id, session_date);
CREATE INDEX IF NOT EXISTS jcfit_workout_sessions_date ON jcfit_workout_sessions (session_date);

CREATE TABLE IF NOT EXISTS jcfit_set_logs (
    id          serial       PRIMARY KEY,
    session_id  integer      NOT NULL REFERENCES jcfit_workout_sessions (id) ON DELETE CASCADE,
    exercise_id integer      NOT NULL REFERENCES jcfit_exercises (id),
    set_number  smallint     NOT NULL CHECK (set_number BETWEEN 1 AND 20),
    weight_kg   numeric(6,2) CHECK (weight_kg BETWEEN 0 AND 999.99),
    reps        smallint     CHECK (reps BETWEEN 0 AND 999),
    completed   boolean      NOT NULL DEFAULT false,
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    UNIQUE (session_id, exercise_id, set_number)
);
CREATE INDEX IF NOT EXISTS jcfit_set_logs_exercise ON jcfit_set_logs (exercise_id);

-- Progreso físico ----------------------------------------------------------------------

-- Un registro por día. Todos los valores son opcionales, pero al menos uno va relleno
-- (lo valida la API).
CREATE TABLE IF NOT EXISTS jcfit_measurements (
    id           serial       PRIMARY KEY,
    measured_on  date         NOT NULL UNIQUE,
    weight_kg    numeric(5,2) CHECK (weight_kg BETWEEN 20 AND 400),
    body_fat_pct numeric(4,1) CHECK (body_fat_pct BETWEEN 2 AND 70),
    chest_cm     numeric(5,1) CHECK (chest_cm BETWEEN 10 AND 300),
    waist_cm     numeric(5,1) CHECK (waist_cm BETWEEN 10 AND 300),
    hip_cm       numeric(5,1) CHECK (hip_cm BETWEEN 10 AND 300),
    leg_cm       numeric(5,1) CHECK (leg_cm BETWEEN 10 AND 300),
    notes        text         CHECK (length(notes) <= 1000),
    created_at   timestamptz  NOT NULL DEFAULT now()
);

-- Las fotos viven en Google Drive (fase 4); aquí solo su referencia.
CREATE TABLE IF NOT EXISTS jcfit_photos (
    id             serial      PRIMARY KEY,
    measurement_id integer     NOT NULL REFERENCES jcfit_measurements (id) ON DELETE CASCADE,
    pose           text        NOT NULL CHECK (pose IN ('FRONT', 'SIDE', 'BACK')),
    drive_file_id  text        NOT NULL,
    mime_type      text        NOT NULL,
    uploaded_at    timestamptz NOT NULL DEFAULT now(),
    UNIQUE (measurement_id, pose)
);

COMMIT;
