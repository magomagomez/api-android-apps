import { test, describe } from 'node:test';
import assert from 'node:assert/strict';
import {
    NOTIFICATION_LIMITS,
    validateNotification,
    buildNotificationBody,
    characterCount,
    sendResultMessage,
    deliveryLabel,
    notificationErrorMessage,
} from '../../../main/resources/static/jcfit-admin/notifications-logic.js';

describe('validateNotification', () => {
    test('título y mensaje correctos: sin errores', () => {
        assert.deepEqual(validateNotification({ title: '¡A por la semana!', body: 'Hoy toca pierna' }), []);
    });

    test('vacíos o solo espacios son obligatorios', () => {
        const errors = validateNotification({ title: '   ', body: '' });
        assert.deepEqual(errors.map((e) => e.field), ['title', 'body']);
        assert.equal(errors[0].message, 'Escribe un título');
        assert.equal(errors[1].message, 'Escribe el mensaje');
    });

    test('límites: 60 en el título y 300 en el mensaje, contando tras recortar', () => {
        assert.equal(NOTIFICATION_LIMITS.title, 60);
        assert.equal(NOTIFICATION_LIMITS.body, 300);
        assert.deepEqual(validateNotification({ title: ' ' + 'a'.repeat(60) + ' ', body: 'b'.repeat(300) }), []);
        const errors = validateNotification({ title: 'a'.repeat(61), body: 'b'.repeat(301) });
        assert.deepEqual(errors.map((e) => e.field), ['title', 'body']);
        assert.equal(errors[0].message, 'El título admite como mucho 60 caracteres');
        assert.equal(errors[1].message, 'El mensaje admite como mucho 300 caracteres');
    });

    test('null o undefined cuentan como vacío', () => {
        assert.equal(validateNotification({}).length, 2);
    });
});

describe('buildNotificationBody', () => {
    test('recorta título y mensaje', () => {
        assert.deepEqual(buildNotificationBody({ title: '  Hola ', body: '\n¿Qué tal?  ' }), { title: 'Hola', body: '¿Qué tal?' });
    });
});

describe('characterCount', () => {
    test('texto del contador y si se pasa', () => {
        assert.deepEqual(characterCount('Hola', 60), { text: '4 / 60', over: false });
        assert.deepEqual(characterCount('a'.repeat(61), 60), { text: '61 / 60', over: true });
        assert.deepEqual(characterCount(null, 300), { text: '0 / 300', over: false });
    });

    test('cuenta lo que se enviará (recortado)', () => {
        assert.equal(characterCount('  Hola  ', 60).text, '4 / 60');
    });
});

describe('sendResultMessage', () => {
    test('todo entregado', () => {
        assert.deepEqual(sendResultMessage({ delivered: 1, failed: 0 }), { kind: 'ok', text: 'Enviado' });
        assert.deepEqual(sendResultMessage({ delivered: 2, failed: 0 }), { kind: 'ok', text: 'Enviado' });
    });

    test('entregado solo en parte', () => {
        assert.deepEqual(sendResultMessage({ delivered: 1, failed: 1 }), { kind: 'partial', text: 'Enviado a 1 de 2 dispositivos' });
        assert.deepEqual(sendResultMessage({ delivered: 2, failed: 1 }), { kind: 'partial', text: 'Enviado a 2 de 3 dispositivos' });
    });

    test('sin dispositivos registrados', () => {
        assert.deepEqual(sendResultMessage({ delivered: 0, failed: 0 }),
            { kind: 'none', text: 'Cris todavía no tiene la app instalada con avisos' });
    });

    test('no llegó a ninguno', () => {
        assert.deepEqual(sendResultMessage({ delivered: 0, failed: 1 }), { kind: 'failed', text: 'No ha llegado a ningún dispositivo' });
        assert.deepEqual(sendResultMessage({ delivered: 0, failed: 2 }), { kind: 'failed', text: 'No ha llegado a ningún dispositivo (0 de 2)' });
    });

    test('valores ausentes cuentan como 0', () => {
        assert.equal(sendResultMessage({}).kind, 'none');
        assert.equal(sendResultMessage(null).kind, 'none');
    });
});

describe('deliveryLabel', () => {
    test('singular y plural', () => {
        assert.equal(deliveryLabel({ delivered: 1, failed: 0 }), '1 entregado · 0 fallidos');
        assert.equal(deliveryLabel({ delivered: 2, failed: 1 }), '2 entregados · 1 fallido');
        assert.equal(deliveryLabel({ delivered: 0, failed: 0 }), 'Sin dispositivos');
    });
});

describe('notificationErrorMessage', () => {
    test('usa el message del servidor', () => {
        assert.equal(notificationErrorMessage(503, { error: 'notifications_unavailable', message: 'Las notificaciones todavía no están configuradas' }),
            'Las notificaciones todavía no están configuradas');
    });

    test('sin cuerpo, 503 y 502 dan los textos del contrato', () => {
        assert.equal(notificationErrorMessage(503, null), 'Las notificaciones todavía no están configuradas');
        assert.equal(notificationErrorMessage(502, {}), 'No se ha podido enviar el aviso; inténtalo más tarde');
        assert.equal(notificationErrorMessage(500, null), 'El servidor respondió con un error (500)');
    });
});
