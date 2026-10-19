// Lógica pura de la pestaña Avisos del backoffice de JCFit: sin DOM ni red.

import { errorMessageFrom } from './admin-logic.js';

export const NOTIFICATION_LIMITS = { title: 60, body: 300 };

// Textos del contrato por si el servidor no manda `message`.
const FALLBACK_ERRORS = {
    503: 'Las notificaciones todavía no están configuradas',
    502: 'No se ha podido enviar el aviso; inténtalo más tarde',
};

export function validateNotification(form) {
    const errors = [];
    const title = trimmed(form?.title);
    if (title === '') {
        errors.push({ field: 'title', message: 'Escribe un título' });
    } else if (title.length > NOTIFICATION_LIMITS.title) {
        errors.push({ field: 'title', message: `El título admite como mucho ${NOTIFICATION_LIMITS.title} caracteres` });
    }
    const body = trimmed(form?.body);
    if (body === '') {
        errors.push({ field: 'body', message: 'Escribe el mensaje' });
    } else if (body.length > NOTIFICATION_LIMITS.body) {
        errors.push({ field: 'body', message: `El mensaje admite como mucho ${NOTIFICATION_LIMITS.body} caracteres` });
    }
    return errors;
}

export function buildNotificationBody(form) {
    return { title: trimmed(form.title), body: trimmed(form.body) };
}

/** Contador `'4 / 60'` de lo que se enviará (ya recortado) y si se pasa del máximo. */
export function characterCount(text, max) {
    const length = trimmed(text).length;
    return { text: `${length} / ${max}`, over: length > max };
}

/**
 * Qué decir tras enviar, a partir del `Notification` devuelto. `kind`: `ok` (todo entregado),
 * `partial`, `none` (no hay dispositivos registrados) o `failed` (no llegó a ninguno).
 */
export function sendResultMessage(notification) {
    const delivered = count(notification?.delivered);
    const failed = count(notification?.failed);
    const total = delivered + failed;
    if (total === 0) {
        return { kind: 'none', text: 'Cris todavía no tiene la app instalada con avisos' };
    }
    if (failed === 0) {
        return { kind: 'ok', text: 'Enviado' };
    }
    if (delivered === 0) {
        const detail = total > 1 ? ` (0 de ${total})` : '';
        return { kind: 'failed', text: `No ha llegado a ningún dispositivo${detail}` };
    }
    return { kind: 'partial', text: `Enviado a ${delivered} de ${total} dispositivos` };
}

/** `'2 entregados · 1 fallido'` para el historial. */
export function deliveryLabel(notification) {
    const delivered = count(notification?.delivered);
    const failed = count(notification?.failed);
    if (delivered + failed === 0) {
        return 'Sin dispositivos';
    }
    return `${delivered} ${delivered === 1 ? 'entregado' : 'entregados'} · ${failed} ${failed === 1 ? 'fallido' : 'fallidos'}`;
}

export function notificationErrorMessage(status, body) {
    const message = body && typeof body.message === 'string' ? body.message.trim() : '';
    if (message === '' && FALLBACK_ERRORS[status]) {
        return FALLBACK_ERRORS[status];
    }
    return errorMessageFrom(status, body);
}

function count(value) {
    return Number.isInteger(value) && value > 0 ? value : 0;
}

function trimmed(value) {
    return value === null || value === undefined ? '' : String(value).trim();
}
