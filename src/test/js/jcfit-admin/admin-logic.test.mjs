import { test, describe } from 'node:test';
import assert from 'node:assert/strict';
import {
    youtubeVideoId,
    youtubeThumbnailUrl,
    validateExercise,
    buildExerciseBody,
    validateProgram,
    buildProgramBody,
    validateDay,
    buildDayBody,
    dayFormFromProgramDay,
    moveItem,
    removeItem,
    weekDays,
    DAY_NAMES,
    errorMessageFrom,
    authFailureNotice,
} from '../../../main/resources/static/jcfit-admin/admin-logic.js';

const ID = 'dQw4w9WgXcQ';

describe('youtubeVideoId (mismas formas que YouTubeUrls.java)', () => {
    for (const url of [
        'https://www.youtube.com/watch?v=' + ID,
        'https://youtube.com/watch?v=' + ID + '&t=42s',
        'https://m.youtube.com/watch?feature=share&v=' + ID,
        'http://www.youtube.com/watch?v=' + ID,
        'https://youtu.be/' + ID,
        'https://youtu.be/' + ID + '?si=abc',
        'https://www.youtube.com/shorts/' + ID,
        'https://www.youtube.com/embed/' + ID,
        'https://www.youtube.com/shorts/' + ID + '/',
        'HTTPS://WWW.YOUTUBE.COM/watch?v=' + ID,
        '  https://youtu.be/' + ID + '  ',
    ]) {
        test('acepta ' + JSON.stringify(url), () => {
            assert.equal(youtubeVideoId(url), ID);
        });
    }

    for (const url of [
        null,
        undefined,
        '',
        '   ',
        'not a url',
        'https://vimeo.com/123456789',
        'https://www.youtube.com/watch?list=PL123',
        'https://www.youtube.com/watch?v=short',
        'https://www.youtube.com/watch?v=' + ID + 'X',
        'https://www.youtube.com/channel/UC1234567890',
        'https://evil.com/watch?v=' + ID,
        'https://youtube.com.evil.com/watch?v=' + ID,
        'javascript:alert(1)',
        'ftp://youtu.be/' + ID,
        'https://youtu.be/' + ID + ' extra',
        'https://www.youtube.com/watch/?v=' + ID,
    ]) {
        test('rechaza ' + JSON.stringify(url), () => {
            assert.equal(youtubeVideoId(url), null);
        });
    }
});

test('youtubeThumbnailUrl construye la miniatura mqdefault', () => {
    assert.equal(youtubeThumbnailUrl(ID), `https://img.youtube.com/vi/${ID}/mqdefault.jpg`);
});

describe('ejercicios', () => {
    const valid = { name: ' Press banca ', youtubeUrl: '', muscleGroup: 'Pecho', instructions: '' };

    test('un ejercicio con nombre y sin enlace es válido', () => {
        assert.deepEqual(validateExercise(valid), []);
    });

    test('el nombre es obligatorio y como mucho 80 caracteres', () => {
        assert.deepEqual(fields(validateExercise({ ...valid, name: '   ' })), ['name']);
        assert.deepEqual(fields(validateExercise({ ...valid, name: 'a'.repeat(81) })), ['name']);
        assert.deepEqual(validateExercise({ ...valid, name: 'a'.repeat(80) }), []);
    });

    test('un enlace que no es de YouTube no es válido', () => {
        const errors = validateExercise({ ...valid, youtubeUrl: 'https://vimeo.com/1' });
        assert.deepEqual(fields(errors), ['youtubeUrl']);
        assert.equal(errors[0].message, 'El enlace de YouTube no es válido');
    });

    test('grupo muscular ≤ 60 e indicaciones ≤ 1000', () => {
        const errors = validateExercise({ ...valid, muscleGroup: 'x'.repeat(61), instructions: 'y'.repeat(1001) });
        assert.deepEqual(fields(errors), ['muscleGroup', 'instructions']);
    });

    test('el cuerpo recorta espacios y manda null en los opcionales vacíos', () => {
        assert.deepEqual(buildExerciseBody(valid), {
            name: 'Press banca', youtubeUrl: null, muscleGroup: 'Pecho', instructions: null,
        });
        assert.deepEqual(buildExerciseBody({ ...valid, youtubeUrl: ' https://youtu.be/' + ID + ' ', instructions: ' Codos a 45º ' }), {
            name: 'Press banca', youtubeUrl: 'https://youtu.be/' + ID, muscleGroup: 'Pecho', instructions: 'Codos a 45º',
        });
    });
});

describe('programa', () => {
    test('nombre obligatorio y fecha yyyy-MM-dd real', () => {
        assert.deepEqual(validateProgram({ name: 'Recomposición', startDate: '2026-09-01' }), []);
        assert.deepEqual(fields(validateProgram({ name: ' ', startDate: '' })), ['name', 'startDate']);
        assert.deepEqual(fields(validateProgram({ name: 'X', startDate: '2026-02-30' })), ['startDate']);
        assert.deepEqual(fields(validateProgram({ name: 'X', startDate: '01/09/2026' })), ['startDate']);
    });

    test('el cuerpo recorta el nombre', () => {
        assert.deepEqual(buildProgramBody({ name: ' Recomposición ', startDate: '2026-09-01' }),
            { name: 'Recomposición', startDate: '2026-09-01' });
    });

    test('weekDays devuelve siempre los 7 días con su entreno o null (descanso)', () => {
        const program = { days: [{ id: 3, dayOfWeek: 1, name: 'Empuje' }, { id: 5, dayOfWeek: 4, name: 'Pierna' }] };
        const week = weekDays(program);
        assert.equal(week.length, 7);
        assert.deepEqual(week.map((d) => d.dayOfWeek), [1, 2, 3, 4, 5, 6, 7]);
        assert.deepEqual(week.map((d) => d.name), DAY_NAMES);
        assert.equal(week[0].day.name, 'Empuje');
        assert.equal(week[1].day, null);
        assert.equal(week[3].day.name, 'Pierna');
        assert.equal(weekDays(null).every((d) => d.day === null), true);
    });
});

describe('día del programa', () => {
    const row = (over = {}) => ({ exerciseId: '4', sets: '4', repsMin: '6', repsMax: '8', restSeconds: '120', ...over });
    const form = (over = {}) => ({ name: 'Empuje', muscles: 'Pecho · Hombro', exercises: [row()], ...over });
    const active = new Set([4, 7]);

    test('un día bien rellenado es válido', () => {
        assert.deepEqual(validateDay(form(), active), []);
    });

    test('nombre 1–60, músculos ≤ 120 y al menos un ejercicio', () => {
        assert.deepEqual(fields(validateDay(form({ name: '' }), active)), ['name']);
        assert.deepEqual(fields(validateDay(form({ name: 'a'.repeat(61) }), active)), ['name']);
        assert.deepEqual(fields(validateDay(form({ muscles: 'm'.repeat(121) }), active)), ['muscles']);
        assert.deepEqual(fields(validateDay(form({ exercises: [] }), active)), ['exercises']);
    });

    test('series 1–20 y enteras', () => {
        assert.deepEqual(fields(validateDay(form({ exercises: [row({ sets: '0' })] }), active)), ['exercises.0.sets']);
        assert.deepEqual(fields(validateDay(form({ exercises: [row({ sets: '21' })] }), active)), ['exercises.0.sets']);
        assert.deepEqual(fields(validateDay(form({ exercises: [row({ sets: '2.5' })] }), active)), ['exercises.0.sets']);
        assert.deepEqual(fields(validateDay(form({ exercises: [row({ sets: '' })] }), active)), ['exercises.0.sets']);
    });

    test('reps mín 1–100 y máx entre mín y 100', () => {
        assert.deepEqual(fields(validateDay(form({ exercises: [row({ repsMin: '0' })] }), active)), ['exercises.0.repsMin']);
        assert.deepEqual(fields(validateDay(form({ exercises: [row({ repsMax: '101' })] }), active)), ['exercises.0.repsMax']);
        assert.deepEqual(fields(validateDay(form({ exercises: [row({ repsMin: '10', repsMax: '8' })] }), active)), ['exercises.0.repsMax']);
        assert.deepEqual(validateDay(form({ exercises: [row({ repsMin: '8', repsMax: '8' })] }), active), []);
    });

    test('descanso 0–900 o vacío', () => {
        assert.deepEqual(validateDay(form({ exercises: [row({ restSeconds: '' })] }), active), []);
        assert.deepEqual(validateDay(form({ exercises: [row({ restSeconds: '0' })] }), active), []);
        assert.deepEqual(fields(validateDay(form({ exercises: [row({ restSeconds: '901' })] }), active)), ['exercises.0.restSeconds']);
        assert.deepEqual(fields(validateDay(form({ exercises: [row({ restSeconds: '-1' })] }), active)), ['exercises.0.restSeconds']);
    });

    test('hay que elegir un ejercicio y que no esté archivado', () => {
        assert.deepEqual(fields(validateDay(form({ exercises: [row({ exerciseId: '' })] }), active)), ['exercises.0.exerciseId']);
        const errors = validateDay(form({ exercises: [row(), row({ exerciseId: '9' })] }), active);
        assert.deepEqual(fields(errors), ['exercises.1.exerciseId']);
    });

    test('el cuerpo convierte a números, recorta textos y manda null en descanso y músculos vacíos', () => {
        const body = buildDayBody(form({ name: ' Empuje ', muscles: ' ', exercises: [row(), row({ exerciseId: '7', restSeconds: ' ' })] }));
        assert.deepEqual(body, {
            name: 'Empuje',
            muscles: null,
            exercises: [
                { exerciseId: 4, sets: 4, repsMin: 6, repsMax: 8, restSeconds: 120 },
                { exerciseId: 7, sets: 4, repsMin: 6, repsMax: 8, restSeconds: null },
            ],
        });
    });

    test('dayFormFromProgramDay pasa un día del servidor al formulario, en orden de position', () => {
        const day = {
            id: 3, dayOfWeek: 1, name: 'Empuje', muscles: null,
            exercises: [
                { id: 12, exerciseId: 7, exerciseName: 'Fondos', archived: true, position: 2, sets: 3, repsMin: 8, repsMax: 12, restSeconds: null },
                { id: 11, exerciseId: 4, exerciseName: 'Press banca', archived: false, position: 1, sets: 4, repsMin: 6, repsMax: 8, restSeconds: 120 },
            ],
        };
        assert.deepEqual(dayFormFromProgramDay(day), {
            name: 'Empuje',
            muscles: '',
            exercises: [
                { exerciseId: '4', exerciseName: 'Press banca', archived: false, sets: '4', repsMin: '6', repsMax: '8', restSeconds: '120' },
                { exerciseId: '7', exerciseName: 'Fondos', archived: true, sets: '3', repsMin: '8', repsMax: '12', restSeconds: '' },
            ],
        });
        assert.deepEqual(dayFormFromProgramDay(null), { name: '', muscles: '', exercises: [] });
    });
});

describe('reordenar', () => {
    test('moveItem sube y baja sin mutar el original', () => {
        const list = ['a', 'b', 'c'];
        assert.deepEqual(moveItem(list, 1, -1), ['b', 'a', 'c']);
        assert.deepEqual(moveItem(list, 1, 1), ['a', 'c', 'b']);
        assert.deepEqual(list, ['a', 'b', 'c']);
    });

    test('moveItem no se sale de los bordes', () => {
        const list = ['a', 'b', 'c'];
        assert.deepEqual(moveItem(list, 0, -1), ['a', 'b', 'c']);
        assert.deepEqual(moveItem(list, 2, 1), ['a', 'b', 'c']);
        assert.notEqual(moveItem(list, 0, -1), list);
    });

    test('removeItem quita uno sin mutar el original', () => {
        const list = ['a', 'b', 'c'];
        assert.deepEqual(removeItem(list, 1), ['a', 'c']);
        assert.deepEqual(list, ['a', 'b', 'c']);
    });
});

describe('errorMessageFrom', () => {
    test('usa el message del servidor tal cual', () => {
        assert.equal(errorMessageFrom(400, { error: 'bad_request', message: 'El enlace de YouTube no es válido' }),
            'El enlace de YouTube no es válido');
    });

    test('sin cuerpo útil da un mensaje genérico con el código', () => {
        assert.equal(errorMessageFrom(502, null), 'El servidor respondió con un error (502)');
        assert.equal(errorMessageFrom(500, { message: '   ' }), 'El servidor respondió con un error (500)');
    });
});

describe('authFailureNotice', () => {
    test('401: el message del servidor o el aviso de sesión caducada', () => {
        assert.equal(authFailureNotice(401, { message: 'Falta la sesión o ha caducado; vuelve a entrar' }),
            'Falta la sesión o ha caducado; vuelve a entrar');
        assert.equal(authFailureNotice(401, null), 'Falta la sesión o ha caducado; vuelve a entrar');
    });

    test('403: la cuenta no es la del backoffice', () => {
        assert.equal(authFailureNotice(403, { error: 'forbidden', message: 'Esta cuenta no tiene acceso aquí' }),
            'Esta cuenta no tiene acceso al backoffice');
    });

    test('otros códigos no echan de la sesión', () => {
        assert.equal(authFailureNotice(400, null), null);
        assert.equal(authFailureNotice(503, null), null);
    });
});

function fields(errors) {
    return errors.map((e) => e.field);
}
