// Lógica pura de la pestaña "Progreso de Cris" del backoffice de JCFit: sin DOM ni red.

import { formatNumber, formatSigned } from './format-logic.js';

const DAY_MS = 86_400_000;
const Y_MARGIN = 0.1;       // aire por encima y por debajo de los extremos de la gráfica
const FLAT_MARGIN = 1;      // rango cuando todos los valores son iguales
const TICK_COUNT = 3;

/** Las medidas de `Measurement`, en el orden en que se enseñan. Kg con 2 decimales; cm y %, 1. */
export const MEASURES = [
    { key: 'weightKg', label: 'Peso', unit: 'kg', decimals: 2 },
    { key: 'bodyFatPct', label: '% grasa', unit: '%', decimals: 1 },
    { key: 'chestCm', label: 'Pecho', unit: 'cm', decimals: 1 },
    { key: 'waistCm', label: 'Cintura', unit: 'cm', decimals: 1 },
    { key: 'hipCm', label: 'Cadera', unit: 'cm', decimals: 1 },
    { key: 'legCm', label: 'Pierna', unit: 'cm', decimals: 1 },
];

export function measureByKey(key) {
    return MEASURES.find((m) => m.key === key) ?? null;
}

/**
 * Para cada medida: el valor actual (el del registro más nuevo que lo tenga), el del inicio
 * (el del más viejo que lo tenga) y la diferencia, o null si no hay al menos dos valores.
 */
export function currentValues(items) {
    const sorted = oldestFirst(items);
    return MEASURES.map((measure) => {
        const withValue = sorted.filter((item) => isNumber(item[measure.key]));
        const first = withValue[0] ?? null;
        const last = withValue[withValue.length - 1] ?? null;
        const diff = withValue.length >= 2 ? round(last[measure.key] - first[measure.key], measure.decimals) : null;
        return {
            ...measure,
            current: last ? last[measure.key] : null,
            currentOn: last ? last.measuredOn : null,
            initial: first ? first[measure.key] : null,
            initialOn: first ? first.measuredOn : null,
            diff,
        };
    });
}

/** `'78,4 kg'`, `'17,8 %'`; sin valor, `'—'`. */
export function formatMeasure(value, measure) {
    return isNumber(value) ? `${formatNumber(value, measure.decimals)} ${measure.unit}` : '—';
}

/** `'−4,2 kg'`, `'+1 cm'`, `'0 cm'`; sin diferencia, `''`. */
export function formatMeasureDiff(diff, measure) {
    return isNumber(diff) ? `${formatSigned(diff, measure.decimals)} ${measure.unit}` : '';
}

/** Los valores de una medida para la gráfica, del más viejo al más nuevo y sin huecos. */
export function chartSeries(items, key) {
    return oldestFirst(items)
        .filter((item) => isNumber(item[key]) && dayNumber(item.measuredOn) !== null)
        .map((item) => ({ date: item.measuredOn, value: item[key] }));
}

/**
 * Coordenadas de la gráfica dentro de un viewBox `width` × `height` con `padding` alrededor del
 * área de dibujo. El eje x es temporal (fechas); el y va de `yMin` (abajo) a `yMax` (arriba) con
 * algo de aire, así que ningún punto toca ni se sale del área.
 */
export function chartGeometry(series, { width, height, padding }) {
    const left = padding.left;
    const top = padding.top;
    const innerWidth = width - padding.left - padding.right;
    const innerHeight = height - padding.top - padding.bottom;
    if (!Array.isArray(series) || series.length === 0) {
        return { points: [], path: '', yMin: null, yMax: null, ticks: [] };
    }
    const values = series.map((p) => p.value);
    const min = Math.min(...values);
    const max = Math.max(...values);
    const margin = max === min ? FLAT_MARGIN : (max - min) * Y_MARGIN;
    const yMin = min - margin;
    const yMax = max + margin;
    const days = series.map((p) => dayNumber(p.date));
    const firstDay = Math.min(...days);
    const span = Math.max(...days) - firstDay;

    const xFor = (day) => (span === 0 ? left + innerWidth / 2 : left + ((day - firstDay) / span) * innerWidth);
    const yFor = (value) => top + ((yMax - value) / (yMax - yMin)) * innerHeight;

    const points = series.map((p, i) => ({ x: round(xFor(days[i]), 1), y: round(yFor(p.value), 1), date: p.date, value: p.value }));
    const path = points.map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x} ${p.y}`).join(' ');
    const ticks = Array.from({ length: TICK_COUNT }, (_, i) => {
        const fraction = i / (TICK_COUNT - 1);
        return { value: yMax - fraction * (yMax - yMin), y: round(top + fraction * innerHeight, 1) };
    });
    return { points, path, yMin, yMax, ticks };
}

/** Las series de un ejercicio: `'62,5 × 10 · 62,5 × 8'`; sin peso, `'12 reps'`. */
export function formatSets(sets) {
    if (!Array.isArray(sets)) {
        return '';
    }
    return [...sets]
        .sort((a, b) => a.setNumber - b.setNumber)
        .map((set) => {
            const reps = isNumber(set.reps) ? String(set.reps) : '—';
            return isNumber(set.weightKg) ? `${formatNumber(set.weightKg, 2)} × ${reps}` : `${reps} reps`;
        })
        .join(' · ');
}

/** `'2 ejercicios · 4 series'` para la cabecera plegable de una sesión. */
export function sessionSummary(session) {
    const exercises = Array.isArray(session?.exercises) ? session.exercises : [];
    const sets = exercises.reduce((total, e) => total + (Array.isArray(e.sets) ? e.sets.length : 0), 0);
    if (sets === 0) {
        return 'Sin series completadas';
    }
    const n = exercises.length;
    return `${n} ${n === 1 ? 'ejercicio' : 'ejercicios'} · ${sets} ${sets === 1 ? 'serie' : 'series'}`;
}

function oldestFirst(items) {
    if (!Array.isArray(items)) {
        return [];
    }
    return [...items].sort((a, b) => String(a.measuredOn).localeCompare(String(b.measuredOn)));
}

function dayNumber(iso) {
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso ?? '');
    return match ? Date.UTC(Number(match[1]), Number(match[2]) - 1, Number(match[3])) / DAY_MS : null;
}

function isNumber(value) {
    return typeof value === 'number' && Number.isFinite(value);
}

function round(value, decimals) {
    const factor = 10 ** decimals;
    const result = Math.round(value * factor) / factor;
    return result === 0 ? 0 : result;   // sin -0
}
