import { test, describe } from 'node:test';
import assert from 'node:assert/strict';
import {
    MEASURES,
    measureByKey,
    currentValues,
    formatMeasure,
    formatMeasureDiff,
    chartSeries,
    chartGeometry,
    formatSets,
    sessionSummary,
} from '../../../main/resources/static/jcfit-admin/progress-logic.js';

// De más nuevo a más viejo, como GET /admin/progress/measurements.
const ITEMS = [
    { id: 3, measuredOn: '2026-10-13', weightKg: 78.4, bodyFatPct: null, chestCm: 101, waistCm: null, hipCm: 98, legCm: 58 },
    { id: 2, measuredOn: '2026-09-20', weightKg: null, bodyFatPct: 18.5, chestCm: 102, waistCm: 86, hipCm: 98, legCm: null },
    { id: 1, measuredOn: '2026-09-01', weightKg: 82.6, bodyFatPct: 20.1, chestCm: 103, waistCm: null, hipCm: 99, legCm: null },
];

describe('MEASURES', () => {
    test('las seis medidas en orden', () => {
        assert.deepEqual(MEASURES.map((m) => m.key), ['weightKg', 'bodyFatPct', 'chestCm', 'waistCm', 'hipCm', 'legCm']);
        assert.deepEqual(MEASURES.map((m) => m.label), ['Peso', '% grasa', 'Pecho', 'Cintura', 'Cadera', 'Pierna']);
        assert.equal(measureByKey('waistCm').unit, 'cm');
        assert.equal(measureByKey('nada'), null);
    });
});

describe('currentValues', () => {
    const byKey = (items) => Object.fromEntries(currentValues(items).map((v) => [v.key, v]));

    test('actual = último registro con valor; inicio = primero con valor', () => {
        const v = byKey(ITEMS);
        assert.equal(v.weightKg.current, 78.4);
        assert.equal(v.weightKg.currentOn, '2026-10-13');
        assert.equal(v.weightKg.initial, 82.6);
        assert.equal(v.weightKg.initialOn, '2026-09-01');
        assert.equal(v.weightKg.diff, -4.2);
    });

    test('si el último registro tiene null, usa el último con valor', () => {
        const v = byKey(ITEMS);
        assert.equal(v.bodyFatPct.current, 18.5);
        assert.equal(v.bodyFatPct.currentOn, '2026-09-20');
        assert.equal(v.bodyFatPct.diff, -1.6);
    });

    test('un solo valor: hay actual pero no diferencia', () => {
        const v = byKey(ITEMS);
        assert.equal(v.waistCm.current, 86);
        assert.equal(v.waistCm.diff, null);
        assert.equal(v.legCm.current, 58);
        assert.equal(v.legCm.diff, null);
    });

    test('igual que al inicio: diferencia 0', () => {
        const v = byKey([
            { measuredOn: '2026-10-13', hipCm: 98 },
            { measuredOn: '2026-09-01', hipCm: 98 },
        ]);
        assert.equal(v.hipCm.diff, 0);
    });

    test('sin valores: todo null', () => {
        const v = byKey([{ measuredOn: '2026-10-13', weightKg: 80 }]);
        assert.equal(v.chestCm.current, null);
        assert.equal(v.chestCm.diff, null);
    });

    test('no depende del orden de entrada', () => {
        const v = byKey([...ITEMS].reverse());
        assert.equal(v.weightKg.current, 78.4);
        assert.equal(v.weightKg.initial, 82.6);
    });

    test('sin registros', () => {
        assert.equal(currentValues([]).length, 6);
        assert.equal(currentValues(null).every((v) => v.current === null), true);
    });
});

describe('formatMeasure y formatMeasureDiff', () => {
    test('con su unidad y en castellano', () => {
        assert.equal(formatMeasure(78.4, measureByKey('weightKg')), '78,4 kg');
        assert.equal(formatMeasure(17.8, measureByKey('bodyFatPct')), '17,8 %');
        assert.equal(formatMeasure(101, measureByKey('chestCm')), '101 cm');
        assert.equal(formatMeasure(null, measureByKey('chestCm')), '—');
    });

    test('diferencias con signo', () => {
        assert.equal(formatMeasureDiff(-4.2, measureByKey('weightKg')), '−4,2 kg');
        assert.equal(formatMeasureDiff(1, measureByKey('chestCm')), '+1 cm');
        assert.equal(formatMeasureDiff(0, measureByKey('hipCm')), '0 cm');
        assert.equal(formatMeasureDiff(null, measureByKey('hipCm')), '');
    });
});

describe('chartSeries', () => {
    test('de más viejo a más nuevo y sin los null', () => {
        assert.deepEqual(chartSeries(ITEMS, 'weightKg'), [
            { date: '2026-09-01', value: 82.6 },
            { date: '2026-10-13', value: 78.4 },
        ]);
        assert.deepEqual(chartSeries(null, 'weightKg'), []);
    });
});

describe('chartGeometry', () => {
    const BOX = { width: 600, height: 200, padding: { top: 20, right: 20, bottom: 30, left: 50 } };
    const inside = (p) => p.x >= BOX.padding.left && p.x <= BOX.width - BOX.padding.right
        && p.y >= BOX.padding.top && p.y <= BOX.height - BOX.padding.bottom;

    test('sin puntos', () => {
        const g = chartGeometry([], BOX);
        assert.deepEqual(g.points, []);
        assert.equal(g.path, '');
    });

    test('el más viejo a la izquierda, el más nuevo a la derecha', () => {
        const g = chartGeometry([
            { date: '2026-09-01', value: 82.6 },
            { date: '2026-10-13', value: 78.4 },
        ], BOX);
        assert.equal(g.points[0].x, 50);
        assert.equal(g.points[1].x, 580);
        assert.ok(g.points[0].y < g.points[1].y, 'más peso = más arriba');
        assert.ok(g.points.every(inside));
    });

    test('el eje x es temporal: un punto a mitad de fechas queda en medio', () => {
        const g = chartGeometry([
            { date: '2026-09-01', value: 80 },
            { date: '2026-09-11', value: 81 },
            { date: '2026-09-21', value: 79 },
        ], BOX);
        assert.equal(g.points[1].x, 315);
    });

    test('los extremos no tocan el borde de arriba ni el de abajo, y quedan dentro', () => {
        const g = chartGeometry([
            { date: '2026-09-01', value: 0 },
            { date: '2026-09-02', value: 400 },
        ], BOX);
        assert.ok(g.points.every(inside));
        assert.ok(g.points[1].y > BOX.padding.top);
        assert.ok(g.points[0].y < BOX.height - BOX.padding.bottom);
        assert.ok(g.yMin <= 0 && g.yMax >= 400);
    });

    test('un solo punto o todos iguales: centrados y dentro', () => {
        const one = chartGeometry([{ date: '2026-09-01', value: 80 }], BOX);
        assert.equal(one.points[0].x, 315);
        assert.equal(one.points[0].y, 95);
        const flat = chartGeometry([
            { date: '2026-09-01', value: 80 },
            { date: '2026-09-08', value: 80 },
        ], BOX);
        assert.ok(flat.points.every((p) => p.y === 95));
        assert.ok(flat.yMin < 80 && flat.yMax > 80);
    });

    test('path de la línea y marcas del eje y', () => {
        const g = chartGeometry([
            { date: '2026-09-01', value: 82.6 },
            { date: '2026-10-13', value: 78.4 },
        ], BOX);
        assert.match(g.path, /^M50 [\d.]+ L580 [\d.]+$/);
        assert.equal(g.ticks.length, 3);
        assert.equal(g.ticks[0].y, BOX.padding.top);
        assert.equal(g.ticks[2].y, BOX.height - BOX.padding.bottom);
        assert.ok(g.ticks[0].value > g.ticks[2].value);
    });
});

describe('formatSets', () => {
    test('peso × reps separados por punto medio, por número de serie', () => {
        assert.equal(formatSets([
            { setNumber: 2, weightKg: 62.5, reps: 8 },
            { setNumber: 1, weightKg: 62.5, reps: 10 },
        ]), '62,5 × 10 · 62,5 × 8');
    });

    test('sin peso: solo las reps', () => {
        assert.equal(formatSets([{ setNumber: 1, weightKg: null, reps: 12 }]), '12 reps');
        assert.equal(formatSets([{ setNumber: 1, weightKg: 0, reps: 12 }]), '0 × 12');
    });

    test('sin series', () => {
        assert.equal(formatSets([]), '');
        assert.equal(formatSets(null), '');
    });
});

describe('sessionSummary', () => {
    test('cuenta ejercicios y series', () => {
        assert.equal(sessionSummary({ exercises: [
            { sets: [{}, {}, {}] },
            { sets: [{}] },
        ] }), '2 ejercicios · 4 series');
        assert.equal(sessionSummary({ exercises: [{ sets: [{}] }] }), '1 ejercicio · 1 serie');
        assert.equal(sessionSummary({ exercises: [] }), 'Sin series completadas');
    });
});
