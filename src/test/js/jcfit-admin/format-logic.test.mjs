import { test, describe } from 'node:test';
import assert from 'node:assert/strict';
import {
    formatNumber,
    formatSigned,
    formatShortDate,
    formatDateTime,
} from '../../../main/resources/static/jcfit-admin/format-logic.js';

describe('formatNumber (castellano)', () => {
    test('coma decimal y sin ceros de relleno', () => {
        assert.equal(formatNumber(78.4, 2), '78,4');
        assert.equal(formatNumber(62.5, 2), '62,5');
        assert.equal(formatNumber(101, 1), '101');
        assert.equal(formatNumber(60.25, 2), '60,25');
    });

    test('redondea a los decimales pedidos', () => {
        assert.equal(formatNumber(17.86, 1), '17,9');
        assert.equal(formatNumber(78.4 - 82.6, 2), '-4,2');
    });

    test('null o no numérico da cadena vacía', () => {
        assert.equal(formatNumber(null, 1), '');
        assert.equal(formatNumber(undefined, 1), '');
        assert.equal(formatNumber(Number.NaN, 1), '');
    });
});

describe('formatSigned', () => {
    test('negativo con el signo menos tipográfico', () => {
        assert.equal(formatSigned(-4.2, 2), '−4,2');
    });

    test('positivo con +', () => {
        assert.equal(formatSigned(1.5, 1), '+1,5');
    });

    test('cero (o lo que redondea a cero) sin signo', () => {
        assert.equal(formatSigned(0, 1), '0');
        assert.equal(formatSigned(-0.001, 1), '0');
        assert.equal(formatSigned(78.4 - 78.4, 2), '0');
    });
});

describe('formatShortDate', () => {
    test('día y mes abreviado', () => {
        assert.equal(formatShortDate('2026-10-15'), '15 oct');
        assert.equal(formatShortDate('2026-01-03'), '3 ene');
        assert.equal(formatShortDate('2026-09-01'), '1 sep');
    });

    test('con año y con día de la semana', () => {
        assert.equal(formatShortDate('2026-10-15', { year: true }), '15 oct 2026');
        assert.equal(formatShortDate('2026-10-15', { weekday: true }), 'jue 15 oct');
        assert.equal(formatShortDate('2026-10-19', { weekday: true, year: true }), 'lun 19 oct 2026');
    });

    test('no se mueve de día por la zona horaria del navegador', () => {
        assert.equal(formatShortDate('2026-03-29'), '29 mar');
    });

    test('lo que no es una fecha se devuelve tal cual', () => {
        assert.equal(formatShortDate('ayer'), 'ayer');
        assert.equal(formatShortDate(null), '');
    });
});

describe('formatDateTime (Europe/Madrid)', () => {
    test('horario de verano: UTC+2', () => {
        assert.equal(formatDateTime('2026-10-19T08:00:00Z'), '19 oct 2026, 10:00');
    });

    test('horario de invierno: UTC+1, tras el cambio del 25 de octubre', () => {
        assert.equal(formatDateTime('2026-10-25T01:30:00Z'), '25 oct 2026, 02:30');
        assert.equal(formatDateTime('2026-11-02T18:05:00Z'), '2 nov 2026, 19:05');
    });

    test('cambia de día y de año según Madrid, no según UTC', () => {
        assert.equal(formatDateTime('2026-12-31T23:30:00Z'), '1 ene 2027, 00:30');
    });

    test('acepta desfases explícitos', () => {
        assert.equal(formatDateTime('2026-10-19T10:00:00+02:00'), '19 oct 2026, 10:00');
    });

    test('un instante inválido devuelve cadena vacía', () => {
        assert.equal(formatDateTime('no es una fecha'), '');
        assert.equal(formatDateTime(null), '');
    });
});
