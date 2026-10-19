// Pestaña Avisos del backoffice de JCFit: DOM y red. La lógica pura está en notifications-logic.js.
// Regla: nada de innerHTML; todo dato del servidor o del usuario entra con textContent o propiedades.

import {
    NOTIFICATION_LIMITS,
    validateNotification,
    buildNotificationBody,
    characterCount,
    sendResultMessage,
    deliveryLabel,
    notificationErrorMessage,
} from './notifications-logic.js';
import { formatDateTime } from './format-logic.js';

const HISTORY_LIMIT = 50;
const PREVIEW_TITLE = 'Título del aviso';
const PREVIEW_BODY = 'Aquí va el mensaje.';
const COUNTERS = { 'nt-title': 'nt-title-count', 'nt-body': 'nt-body-count' };

/** `ui`: las utilidades compartidas de admin.js ({ $, api, el, showAlert, whileBusy, … }). */
export function createNotificationsTab(ui) {
    const { $, api, el, showAlert, whileBusy, showFieldErrors, clearFieldErrors } = ui;
    let history = [];
    let historyRequest = 0;

    function formValues() {
        return { title: $('nt-title').value, body: $('nt-body').value };
    }

    // ---------- Contadores y vista previa ----------

    /** Los errores de campo pisan aria-describedby: el contador tiene que seguir enlazado. */
    function linkCounters() {
        for (const [inputId, counterId] of Object.entries(COUNTERS)) {
            const input = $(inputId);
            const ids = new Set((input.getAttribute('aria-describedby') ?? '').split(' ').filter(Boolean));
            ids.add(counterId);
            input.setAttribute('aria-describedby', [...ids].join(' '));
        }
    }

    function updateCounter(inputId, counterId, max) {
        const { text, over } = characterCount($(inputId).value, max);
        const counter = $(counterId);
        counter.textContent = text;
        counter.classList.toggle('is-over', over);
    }

    function updatePreview() {
        const { title, body } = buildNotificationBody(formValues());
        setPreviewText($('preview-title'), title, PREVIEW_TITLE);
        setPreviewText($('preview-body'), body, PREVIEW_BODY);
    }

    function setPreviewText(node, text, placeholder) {
        node.textContent = text || placeholder;
        node.classList.toggle('is-placeholder', !text);
    }

    function onInput() {
        updateCounter('nt-title', 'nt-title-count', NOTIFICATION_LIMITS.title);
        updateCounter('nt-body', 'nt-body-count', NOTIFICATION_LIMITS.body);
        updatePreview();
        // Lo confirmado era otro texto: hay que volver a confirmar.
        if (!$('notify-confirm').hidden) {
            hideConfirm(false);
        }
    }

    function fillForm({ title = '', body = '' } = {}) {
        $('nt-title').value = title;
        $('nt-body').value = body;
        clearFieldErrors($('notify-form'));
        linkCounters();
        onInput();
    }

    // ---------- Envío ----------

    function onSubmit(event) {
        event.preventDefault();
        const form = $('notify-form');
        const errors = validateNotification(formValues());
        showAlert($('notify-error'), '');
        showResult(null);
        if (errors.length > 0) {
            showFieldErrors(form, errors);
            linkCounters();
            return;
        }
        clearFieldErrors(form);
        linkCounters();
        $('notify-confirm').hidden = false;
        $('notify-send').setAttribute('aria-expanded', 'true');
        $('notify-confirm-no').focus();
    }

    function hideConfirm(returnFocus) {
        $('notify-confirm').hidden = true;
        $('notify-send').setAttribute('aria-expanded', 'false');
        if (returnFocus) {
            $('notify-send').focus();
        }
    }

    function buttons() {
        return [$('notify-confirm-yes'), $('notify-confirm-no'), $('notify-send'), $('notify-clear')];
    }

    async function onConfirm() {
        const body = buildNotificationBody(formValues());
        showAlert($('notify-error'), '');
        try {
            const sent = await whileBusy(buttons(), 'Enviando…', () => api('POST', '/admin/notifications', body));
            hideConfirm(false);
            showResult(sendResultMessage(sent));
            fillForm();
            if (sent) {
                history = [sent, ...history.filter((n) => n.id !== sent.id)];
                renderHistory();
            }
            $('nt-title').focus();
        } catch (err) {
            if (err.sessionExpired) {
                return;
            }
            hideConfirm(true);
            showAlert($('notify-error'), err.status ? notificationErrorMessage(err.status, err.body) : err.message);
        }
    }

    function showResult(result) {
        const node = $('notify-result');
        node.classList.toggle('is-ok', result?.kind === 'ok');
        node.classList.toggle('is-warn', Boolean(result) && result.kind !== 'ok');
        showAlert(node, result ? result.text : '');
    }

    function onClear() {
        fillForm();
        showAlert($('notify-error'), '');
        showResult(null);
        $('nt-title').focus();
    }

    // ---------- Historial ----------

    async function load() {
        const request = ++historyRequest;
        const status = $('notify-history-status');
        const list = $('notify-history');
        if (history.length === 0) {
            list.replaceChildren(...[0, 1].map(() => el('li', { className: 'skeleton', 'aria-hidden': 'true' })));
        }
        status.textContent = 'Cargando avisos…';
        try {
            const data = await api('GET', `/admin/notifications?limit=${HISTORY_LIMIT}`);
            if (request !== historyRequest) {
                return;
            }
            history = Array.isArray(data) ? data : [];
            renderHistory();
        } catch (err) {
            if (request !== historyRequest || err.sessionExpired) {
                return;
            }
            list.replaceChildren();
            status.textContent = err.message;
        }
    }

    function renderHistory() {
        $('notify-history-status').textContent = history.length === 0 ? 'Todavía no has enviado ningún aviso.' : '';
        $('notify-history').replaceChildren(...history.map((n) => el('li', {},
            el('article', { className: 'history-item' },
                el('div', { className: 'history-meta' },
                    el('time', { dateTime: n.sentAt ?? '', text: formatDateTime(n.sentAt) }),
                    el('span', { className: 'badge', text: deliveryLabel(n) })),
                el('p', { className: 'history-title', text: n.title ?? '' }),
                el('p', { className: 'history-body', text: n.body ?? '' }),
                el('button', {
                    type: 'button',
                    className: 'btn btn-ghost history-resend',
                    'aria-label': `Reenviar «${n.title ?? ''}»`,
                    text: 'Reenviar',
                    onClick: () => resend(n),
                })))));
    }

    /** Rellena el formulario con un aviso ya enviado; no envía nada hasta que se confirme. */
    function resend(notification) {
        fillForm({ title: notification.title ?? '', body: notification.body ?? '' });
        showAlert($('notify-error'), '');
        showResult(null);
        $('notify-form').scrollIntoView({ block: 'start', behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth' });
        $('nt-title').focus({ preventScroll: true });
    }

    function reset() {
        history = [];
        $('notify-history').replaceChildren();
        $('notify-history-status').textContent = '';
        fillForm();
        hideConfirm(false);
        showAlert($('notify-error'), '');
        showResult(null);
    }

    $('notify-form').addEventListener('submit', onSubmit);
    $('nt-title').addEventListener('input', onInput);
    $('nt-body').addEventListener('input', onInput);
    $('notify-clear').addEventListener('click', onClear);
    $('notify-send').setAttribute('aria-controls', 'notify-confirm');
    $('notify-send').setAttribute('aria-expanded', 'false');
    $('notify-confirm-no').addEventListener('click', () => hideConfirm(true));
    $('notify-confirm-yes').addEventListener('click', onConfirm);
    onInput();

    return { load, reset };
}
