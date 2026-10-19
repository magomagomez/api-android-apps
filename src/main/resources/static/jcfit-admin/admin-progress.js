// Pestaña "Progreso de Cris" del backoffice de JCFit (solo lectura): DOM y red.
// La lógica pura está en progress-logic.js. Nada de innerHTML: todo dato entra con textContent.

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
} from './progress-logic.js';
import { formatNumber, formatShortDate } from './format-logic.js';

const SESSIONS_LIMIT = 20;
const DEFAULT_CHART_MEASURE = 'weightKg';
// El viewBox usa el ancho real del contenedor (1 unidad = 1 px) para que las etiquetas no escalen.
const CHART_PADDING = { top: 16, right: 20, bottom: 34, left: 56 };
const CHART_MIN_WIDTH = 280;
const CHART_HEIGHT = 240;
const RESIZE_DELAY_MS = 150;
const POINT_RADIUS = 4;
const SVG_NS = 'http://www.w3.org/2000/svg';

/** `ui`: las utilidades compartidas de admin.js ({ $, api, el, showAlert, … }). */
export function createProgressTab(ui) {
    const { $, api, el, showAlert } = ui;
    let measurements = null;   // items de GET /admin/progress/measurements, de más nuevo a más viejo
    let sessions = null;
    let chartKey = DEFAULT_CHART_MEASURE;
    let measuresRequest = 0;
    let sessionsRequest = 0;

    function load() {
        loadMeasurements();
        loadSessions();
    }

    // ---------- Medidas ----------

    async function loadMeasurements() {
        const request = ++measuresRequest;
        const status = $('measures-status');
        showAlert($('measures-error'), '');
        if (measurements === null) {
            status.textContent = 'Cargando medidas…';
        }
        try {
            const data = await api('GET', '/admin/progress/measurements');
            if (request !== measuresRequest) {
                return;
            }
            status.textContent = '';
            measurements = Array.isArray(data?.items) ? data.items : [];
            renderMeasurements();
        } catch (err) {
            if (request !== measuresRequest || err.sessionExpired) {
                return;
            }
            status.textContent = '';
            showAlert($('measures-error'), err.message);
        }
    }

    function renderMeasurements() {
        const empty = measurements.length === 0;
        $('measures-empty').hidden = !empty;
        $('measures-content').hidden = empty;
        if (empty) {
            return;
        }
        renderCards();
        renderChartSelect();
        renderChart();
        renderTable();
    }

    function renderCards() {
        $('measure-cards').replaceChildren(...currentValues(measurements).map((v) => {
            let change = 'Sin datos';
            if (v.diff !== null) {
                change = `${formatMeasureDiff(v.diff, v)} desde el ${formatShortDate(v.initialOn)}`;
            } else if (v.current !== null) {
                change = 'Un solo registro';
            }
            return el('li', { className: 'stat' + (v.current === null ? ' is-empty' : '') },
                el('p', { className: 'eyebrow', text: v.label }),
                el('p', { className: 'stat-value num', text: formatMeasure(v.current, v) }),
                el('p', { className: 'stat-change', text: change }),
                v.currentOn ? el('p', { className: 'stat-date', text: `Último: ${formatShortDate(v.currentOn, { year: true })}` }) : null);
        }));
    }

    function renderChartSelect() {
        const available = MEASURES.filter((m) => chartSeries(measurements, m.key).length > 0);
        if (!available.some((m) => m.key === chartKey)) {
            chartKey = available[0]?.key ?? DEFAULT_CHART_MEASURE;
        }
        const select = $('chart-measure');
        select.replaceChildren(...available.map((m) => el('option', { value: m.key, text: m.unit === '%' ? m.label : `${m.label} (${m.unit})` })));
        select.value = chartKey;
    }

    function renderChart() {
        const measure = measureByKey(chartKey);
        const series = chartSeries(measurements, chartKey);
        $('chart-title').textContent = `Evolución · ${measure.label}`;
        const container = $('chart');
        if (series.length === 0) {
            container.replaceChildren(el('p', { className: 'muted', text: 'No hay valores de esta medida.' }));
            return;
        }
        const width = Math.max(CHART_MIN_WIDTH, Math.round(container.clientWidth));
        const box = { width, height: CHART_HEIGHT, padding: CHART_PADDING };
        container.replaceChildren(chartSvg(series, measure, box));
        if (series.length === 1) {
            container.append(el('p', { className: 'muted chart-note', text: 'Solo hay un registro: la línea aparecerá con el segundo.' }));
        }
    }

    function chartSvg(series, measure, box) {
        const { width, height, padding } = box;
        const geometry = chartGeometry(series, box);
        const first = series[0];
        const last = series[series.length - 1];
        const summary = series.length === 1
            ? `${measure.label}: ${formatMeasure(first.value, measure)} el ${formatShortDate(first.date, { year: true })}.`
            : `${measure.label}: de ${formatMeasure(first.value, measure)} el ${formatShortDate(first.date, { year: true })} `
              + `a ${formatMeasure(last.value, measure)} el ${formatShortDate(last.date, { year: true })}, en ${series.length} registros.`;

        const svg = svgNode('svg', { viewBox: `0 0 ${width} ${height}`, role: 'img', 'aria-labelledby': 'chart-svg-title', class: 'chart-svg' });
        svg.append(svgNode('title', { id: 'chart-svg-title' }, summary));
        const tickDecimals = measure.decimals === 2 ? 1 : measure.decimals;
        for (const tick of geometry.ticks) {
            svg.append(
                svgNode('line', { x1: padding.left, x2: width - padding.right, y1: tick.y, y2: tick.y, class: 'chart-grid' }),
                svgNode('text', { x: padding.left - 10, y: tick.y, class: 'chart-label', 'text-anchor': 'end', 'dominant-baseline': 'middle' },
                    formatNumber(tick.value, tickDecimals)));
        }
        const labelY = height - padding.bottom + 22;
        const dateLabels = geometry.points.length === 1
            ? [{ point: geometry.points[0], anchor: 'middle' }]
            : [{ point: geometry.points[0], anchor: 'start' }, { point: geometry.points[geometry.points.length - 1], anchor: 'end' }];
        for (const { point, anchor } of dateLabels) {
            svg.append(svgNode('text', { x: point.x, y: labelY, class: 'chart-label', 'text-anchor': anchor }, formatShortDate(point.date, { year: true })));
        }
        if (geometry.points.length > 1) {
            svg.append(svgNode('path', { d: geometry.path, class: 'chart-line' }));
        }
        for (const point of geometry.points) {
            const dot = svgNode('circle', { cx: point.x, cy: point.y, r: POINT_RADIUS, class: 'chart-dot' });
            dot.append(svgNode('title', {}, `${formatShortDate(point.date, { year: true })}: ${formatMeasure(point.value, measure)}`));
            svg.append(dot);
        }
        return svg;
    }

    function renderTable() {
        $('measure-rows').replaceChildren(...measurements.map((m) => el('tr', {},
            el('th', { scope: 'row', text: formatShortDate(m.measuredOn, { year: true }) }),
            MEASURES.map((measure) => {
                const value = m[measure.key];
                return el('td', { className: 'is-num num', text: typeof value === 'number' ? formatNumber(value, measure.decimals) : '—' });
            }),
            el('td', { className: 'notes', text: m.notes ?? '' }))));
    }

    // ---------- Entrenos ----------

    async function loadSessions() {
        const request = ++sessionsRequest;
        const status = $('sessions-status');
        const list = $('session-list');
        showAlert($('sessions-error'), '');
        if (sessions === null) {
            status.textContent = 'Cargando entrenos…';
            list.replaceChildren(...[0, 1, 2].map(() => el('li', { className: 'skeleton', 'aria-hidden': 'true' })));
        }
        try {
            const data = await api('GET', `/admin/progress/sessions?limit=${SESSIONS_LIMIT}`);
            if (request !== sessionsRequest) {
                return;
            }
            status.textContent = '';
            sessions = Array.isArray(data) ? data : [];
            renderSessions();
        } catch (err) {
            if (request !== sessionsRequest || err.sessionExpired) {
                return;
            }
            status.textContent = '';
            list.replaceChildren();
            showAlert($('sessions-error'), err.message);
        }
    }

    function renderSessions() {
        $('sessions-empty').hidden = sessions.length > 0;
        const thisYear = String(new Date().getFullYear());
        $('session-list').replaceChildren(...sessions.map((session, index) => {
            const exercises = Array.isArray(session.exercises) ? session.exercises : [];
            const date = typeof session.date === 'string' ? session.date : '';
            return el('li', {},
                el('details', { className: 'session', open: index === 0 },
                    el('summary', {},
                        el('span', { className: 'session-date', text: formatShortDate(date, { weekday: true, year: !date.startsWith(thisYear) }) }),
                        el('span', { className: 'session-name', text: session.dayName || 'Entreno' }),
                        el('span', { className: 'badge' + (session.finished ? ' is-done' : ''), text: session.finished ? 'Terminado' : 'Sin terminar' }),
                        el('span', { className: 'session-meta', text: sessionSummary(session) })),
                    exercises.length === 0
                        ? el('p', { className: 'muted session-empty', text: 'No hay series completadas en este entreno.' })
                        : el('ul', { className: 'session-exercises' }, exercises.map((exercise) => el('li', {},
                            el('span', { className: 'session-exercise', text: exercise.name ?? '' }),
                            el('span', { className: 'session-sets num', text: formatSets(exercise.sets) }))))));
        }));
    }

    function reset() {
        measurements = null;
        sessions = null;
        chartKey = DEFAULT_CHART_MEASURE;
        $('measures-content').hidden = true;
        $('measures-empty').hidden = true;
        $('sessions-empty').hidden = true;
        $('session-list').replaceChildren();
    }

    $('chart-measure').addEventListener('change', (event) => {
        chartKey = event.target.value;
        renderChart();
    });

    let resizeTimer = null;
    window.addEventListener('resize', () => {
        clearTimeout(resizeTimer);
        resizeTimer = setTimeout(() => {
            if (measurements && measurements.length > 0 && !$('measures-content').hidden && !$('progress-view').hidden) {
                renderChart();
            }
        }, RESIZE_DELAY_MS);
    });

    return { load, reset };
}

function svgNode(tag, attrs = {}, text) {
    const node = document.createElementNS(SVG_NS, tag);
    for (const [key, value] of Object.entries(attrs)) {
        node.setAttribute(key, String(value));
    }
    if (text !== undefined) {
        node.textContent = text;
    }
    return node;
}
