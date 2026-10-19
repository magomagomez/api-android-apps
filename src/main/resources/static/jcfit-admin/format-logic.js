// Formato de números y fechas en castellano para el backoffice de JCFit. Sin DOM ni red.

const LOCALE = 'es-ES';
const TIME_ZONE = 'Europe/Madrid';
const MINUS = '−';
const MONTHS = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
const WEEKDAYS = ['dom', 'lun', 'mar', 'mié', 'jue', 'vie', 'sáb'];
const ISO_DATE = /^(\d{4})-(\d{2})-(\d{2})$/;

const numberFormats = new Map();
const madridParts = new Intl.DateTimeFormat(LOCALE, {
    timeZone: TIME_ZONE,
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
});

function numberFormat(decimals) {
    if (!numberFormats.has(decimals)) {
        numberFormats.set(decimals, new Intl.NumberFormat(LOCALE, { maximumFractionDigits: decimals, useGrouping: false }));
    }
    return numberFormats.get(decimals);
}

function isNumber(value) {
    return typeof value === 'number' && Number.isFinite(value);
}

/** `78.4` → `'78,4'`, con `decimals` decimales como máximo. Null o no numérico → `''`. */
export function formatNumber(value, decimals) {
    if (!isNumber(value)) {
        return '';
    }
    const text = numberFormat(decimals).format(value);
    return text === '-0' ? '0' : text;
}

/** Diferencia con signo: `'+1,5'`, `'−4,2'` (signo menos tipográfico) o `'0'`. */
export function formatSigned(value, decimals) {
    const text = formatNumber(value, decimals);
    if (text === '' || text === '0') {
        return text;
    }
    return text.startsWith('-') ? MINUS + text.substring(1) : '+' + text;
}

/**
 * `'2026-10-15'` → `'15 oct'`; con `year` → `'15 oct 2026'`; con `weekday` → `'jue 15 oct'`.
 * Se lee como fecha de calendario, sin zona horaria, así que no cambia de día en ningún navegador.
 */
export function formatShortDate(iso, { year = false, weekday = false } = {}) {
    if (typeof iso !== 'string') {
        return '';
    }
    const match = ISO_DATE.exec(iso);
    if (!match) {
        return iso;
    }
    const [y, m, d] = match.slice(1).map(Number);
    let text = `${d} ${MONTHS[m - 1]}`;
    if (year) {
        text += ` ${y}`;
    }
    if (weekday) {
        text = `${WEEKDAYS[new Date(Date.UTC(y, m - 1, d)).getUTCDay()]} ${text}`;
    }
    return text;
}

/** Un instante ISO-8601 como fecha y hora de Madrid: `'19 oct 2026, 10:00'`. Inválido → `''`. */
export function formatDateTime(instant) {
    if (typeof instant !== 'string' || instant.trim() === '') {
        return '';
    }
    const date = new Date(instant);
    if (Number.isNaN(date.getTime())) {
        return '';
    }
    const parts = Object.fromEntries(madridParts.formatToParts(date).map((p) => [p.type, p.value]));
    return `${Number(parts.day)} ${MONTHS[Number(parts.month) - 1]} ${parts.year}, ${parts.hour}:${parts.minute}`;
}
