// Lógica pura del backoffice de JCFit: sin DOM ni red, para poder probarla con `node --test`.

export const DAY_NAMES = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];

const VIDEO_ID = /^[A-Za-z0-9_-]{11}$/;
const LONG_HOSTS = new Set(['youtube.com', 'www.youtube.com', 'm.youtube.com']);
const SHORT_HOST = 'youtu.be';
const ID_PATH_PREFIXES = ['/shorts/', '/embed/'];
const WATCH_PATH = '/watch';
const VIDEO_PARAM = 'v=';
const ABSOLUTE_HTTP = /^https?:\/\//i;

export const LIMITS = {
    exerciseName: 80,
    muscleGroup: 60,
    instructions: 1000,
    dayName: 60,
    muscles: 120,
    sets: [1, 20],
    reps: [1, 100],
    restSeconds: [0, 900],
};

/**
 * El id del vídeo de un enlace de YouTube, o null. Acepta las mismas formas que
 * YouTubeUrls.java: watch?v=, youtu.be/, shorts/ y embed/, por http o https, con o sin www o m.
 */
export function youtubeVideoId(url) {
    if (typeof url !== 'string') {
        return null;
    }
    const text = url.trim();
    // java.net.URI rechaza espacios y barras invertidas; URL de JS las "arregla", así que se filtran antes.
    if (!ABSOLUTE_HTTP.test(text) || /[\s\\]/.test(text)) {
        return null;
    }
    let parsed;
    try {
        parsed = new URL(text);
    } catch {
        return null;
    }
    const host = parsed.hostname.toLowerCase();
    const path = parsed.pathname;
    let candidate = null;
    if (host === SHORT_HOST) {
        candidate = path.startsWith('/') ? path.substring(1) : path;
    } else if (LONG_HOSTS.has(host)) {
        if (path === WATCH_PATH) {
            candidate = queryValue(parsed.search.replace(/^\?/, ''));
        } else {
            const prefix = ID_PATH_PREFIXES.find((p) => path.startsWith(p));
            candidate = prefix ? path.substring(prefix.length) : null;
        }
    }
    if (candidate === null) {
        return null;
    }
    const id = candidate.endsWith('/') ? candidate.slice(0, -1) : candidate;
    return VIDEO_ID.test(id) ? id : null;
}

function queryValue(query) {
    if (!query) {
        return null;
    }
    const pair = query.split('&').find((p) => p.startsWith(VIDEO_PARAM));
    return pair === undefined ? null : pair.substring(VIDEO_PARAM.length);
}

export function youtubeThumbnailUrl(videoId) {
    return `https://img.youtube.com/vi/${videoId}/mqdefault.jpg`;
}

export function youtubeWatchUrl(videoId) {
    return `https://www.youtube.com/watch?v=${videoId}`;
}

// ---------- Ejercicios ----------

export function validateExercise(form) {
    const errors = [];
    const name = trimmed(form.name);
    if (name === '') {
        errors.push(error('name', 'Ponle un nombre al ejercicio'));
    } else if (name.length > LIMITS.exerciseName) {
        errors.push(error('name', `El nombre admite como mucho ${LIMITS.exerciseName} caracteres`));
    }
    const url = trimmed(form.youtubeUrl);
    if (url !== '' && youtubeVideoId(url) === null) {
        errors.push(error('youtubeUrl', 'El enlace de YouTube no es válido'));
    }
    if (trimmed(form.muscleGroup).length > LIMITS.muscleGroup) {
        errors.push(error('muscleGroup', `El grupo muscular admite como mucho ${LIMITS.muscleGroup} caracteres`));
    }
    if (trimmed(form.instructions).length > LIMITS.instructions) {
        errors.push(error('instructions', `Las indicaciones admiten como mucho ${LIMITS.instructions} caracteres`));
    }
    return errors;
}

export function buildExerciseBody(form) {
    return {
        name: trimmed(form.name),
        youtubeUrl: orNull(form.youtubeUrl),
        muscleGroup: orNull(form.muscleGroup),
        instructions: orNull(form.instructions),
    };
}

// ---------- Programa ----------

export function validateProgram(form) {
    const errors = [];
    if (trimmed(form.name) === '') {
        errors.push(error('name', 'Ponle un nombre al programa'));
    }
    if (!isIsoDate(trimmed(form.startDate))) {
        errors.push(error('startDate', 'Elige una fecha de inicio válida'));
    }
    return errors;
}

export function buildProgramBody(form) {
    return { name: trimmed(form.name), startDate: trimmed(form.startDate) };
}

/** Los 7 días de la semana (1 = lunes), cada uno con su entreno o null si es descanso. */
export function weekDays(program) {
    const days = program && Array.isArray(program.days) ? program.days : [];
    return DAY_NAMES.map((name, index) => {
        const dayOfWeek = index + 1;
        return { dayOfWeek, name, day: days.find((d) => d.dayOfWeek === dayOfWeek) ?? null };
    });
}

function isIsoDate(text) {
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(text);
    if (!match) {
        return false;
    }
    const [year, month, day] = match.slice(1).map(Number);
    const date = new Date(Date.UTC(year, month - 1, day));
    return date.getUTCFullYear() === year && date.getUTCMonth() === month - 1 && date.getUTCDate() === day;
}

// ---------- Día del programa ----------

/**
 * Valida el formulario de un día. `activeExerciseIds` es el conjunto de ids de ejercicios no
 * archivados: el servidor no deja guardar un día con un ejercicio archivado.
 */
export function validateDay(form, activeExerciseIds) {
    const errors = [];
    const name = trimmed(form.name);
    if (name === '') {
        errors.push(error('name', 'Ponle un nombre al entreno'));
    } else if (name.length > LIMITS.dayName) {
        errors.push(error('name', `El nombre admite como mucho ${LIMITS.dayName} caracteres`));
    }
    if (trimmed(form.muscles).length > LIMITS.muscles) {
        errors.push(error('muscles', `Los músculos admiten como mucho ${LIMITS.muscles} caracteres`));
    }
    const rows = form.exercises ?? [];
    if (rows.length === 0) {
        errors.push(error('exercises', 'Añade al menos un ejercicio'));
    }
    rows.forEach((row, i) => errors.push(...validateDayRow(row, i, activeExerciseIds)));
    return errors;
}

function validateDayRow(row, index, activeExerciseIds) {
    const errors = [];
    const at = (field) => `exercises.${index}.${field}`;
    const exerciseId = toInt(row.exerciseId);
    if (exerciseId === null) {
        errors.push(error(at('exerciseId'), 'Elige un ejercicio'));
    } else if (activeExerciseIds && !activeExerciseIds.has(exerciseId)) {
        errors.push(error(at('exerciseId'), 'Ese ejercicio está archivado: cámbialo o quítalo'));
    }
    const [minSets, maxSets] = LIMITS.sets;
    if (!inRange(toInt(row.sets), minSets, maxSets)) {
        errors.push(error(at('sets'), `Series entre ${minSets} y ${maxSets}`));
    }
    const [minReps, maxReps] = LIMITS.reps;
    const repsMin = toInt(row.repsMin);
    const repsMax = toInt(row.repsMax);
    if (!inRange(repsMin, minReps, maxReps)) {
        errors.push(error(at('repsMin'), `Reps mínimas entre ${minReps} y ${maxReps}`));
    }
    if (!inRange(repsMax, minReps, maxReps)) {
        errors.push(error(at('repsMax'), `Reps máximas entre ${minReps} y ${maxReps}`));
    } else if (repsMin !== null && repsMax < repsMin) {
        errors.push(error(at('repsMax'), 'Las reps máximas no pueden ser menos que las mínimas'));
    }
    const [minRest, maxRest] = LIMITS.restSeconds;
    if (trimmed(row.restSeconds) !== '' && !inRange(toInt(row.restSeconds), minRest, maxRest)) {
        errors.push(error(at('restSeconds'), `Descanso entre ${minRest} y ${maxRest} s, o vacío`));
    }
    return errors;
}

export function buildDayBody(form) {
    return {
        name: trimmed(form.name),
        muscles: orNull(form.muscles),
        exercises: (form.exercises ?? []).map((row) => ({
            exerciseId: toInt(row.exerciseId),
            sets: toInt(row.sets),
            repsMin: toInt(row.repsMin),
            repsMax: toInt(row.repsMax),
            restSeconds: toInt(row.restSeconds),
        })),
    };
}

/** Un día del `Program` del servidor (o null para un descanso) como formulario editable. */
export function dayFormFromProgramDay(day) {
    if (!day) {
        return { name: '', muscles: '', exercises: [] };
    }
    const exercises = [...(day.exercises ?? [])]
        .sort((a, b) => a.position - b.position)
        .map((e) => ({
            exerciseId: String(e.exerciseId),
            exerciseName: e.exerciseName,
            archived: Boolean(e.archived),
            sets: String(e.sets),
            repsMin: String(e.repsMin),
            repsMax: String(e.repsMax),
            restSeconds: e.restSeconds === null || e.restSeconds === undefined ? '' : String(e.restSeconds),
        }));
    return { name: day.name ?? '', muscles: day.muscles ?? '', exercises };
}

// ---------- Listas ----------

/** Copia de la lista con el elemento `index` movido `delta` posiciones (−1 sube, +1 baja). */
export function moveItem(list, index, delta) {
    const copy = [...list];
    const target = index + delta;
    if (index < 0 || index >= copy.length || target < 0 || target >= copy.length) {
        return copy;
    }
    [copy[index], copy[target]] = [copy[target], copy[index]];
    return copy;
}

export function removeItem(list, index) {
    return list.filter((_, i) => i !== index);
}

// ---------- Errores ----------

export function errorMessageFrom(status, body) {
    const message = body && typeof body.message === 'string' ? body.message.trim() : '';
    return message !== '' ? body.message : `El servidor respondió con un error (${status})`;
}

// ---------- Utilidades ----------

function error(field, message) {
    return { field, message };
}

function trimmed(value) {
    return value === null || value === undefined ? '' : String(value).trim();
}

function orNull(value) {
    const text = trimmed(value);
    return text === '' ? null : text;
}

function toInt(value) {
    const text = trimmed(value);
    return /^\d+$/.test(text) ? Number(text) : null;
}

function inRange(value, min, max) {
    return value !== null && value >= min && value <= max;
}
