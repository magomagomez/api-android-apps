// Backoffice de JCFit: DOM y red. La lógica pura está en admin-logic.js.
// Regla: nada de innerHTML; todo dato del servidor o del usuario entra con textContent o propiedades.

import {
    youtubeVideoId,
    youtubeThumbnailUrl,
    youtubeWatchUrl,
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
} from './admin-logic.js';

const API_BASE = '/jcfit-api/v1';
const TOKEN_KEY = 'jcfit-admin-token';
const NETWORK_ERROR = 'No se pudo conectar con el servidor. Comprueba la conexión y vuelve a intentarlo.';
const SESSION_EXPIRED = 'Falta la sesión o ha caducado; vuelve a entrar';
const TOAST_MS = 2600;
const VIDEO_ID = /^[A-Za-z0-9_-]{11}$/;
const NEW_ROW = { exerciseId: '', exerciseName: '', archived: false, sets: '3', repsMin: '8', repsMax: '12', restSeconds: '90' };

const $ = (id) => document.getElementById(id);

const state = {
    exercises: [],          // listado de la pestaña Ejercicios (con o sin archivados)
    activeExercises: [],    // no archivados, para elegir en un día
    editingExercise: null,  // Exercise del servidor, o null si es nuevo
    program: null,
    programMissing: false,
    dayEdit: null,          // { dayOfWeek, existing, form }
    rowKey: 0,
};

// ---------- Sesión ----------

function readToken() {
    try {
        return sessionStorage.getItem(TOKEN_KEY);
    } catch {
        return null;
    }
}

function writeToken(token) {
    try {
        if (token) {
            sessionStorage.setItem(TOKEN_KEY, token);
        } else {
            sessionStorage.removeItem(TOKEN_KEY);
        }
    } catch {
        // Sin sessionStorage la sesión dura lo que la pestaña; no hay más que hacer.
    }
}

// ---------- Red ----------

class ApiError extends Error {
    constructor(message, { status = 0, code = null, sessionExpired = false } = {}) {
        super(message);
        this.status = status;
        this.code = code;
        this.sessionExpired = sessionExpired;
    }
}

async function api(method, path, body, { auth = true } = {}) {
    const headers = { Accept: 'application/json' };
    if (body !== undefined) {
        headers['Content-Type'] = 'application/json';
    }
    const token = readToken();
    if (auth && token) {
        headers.Authorization = `Bearer ${token}`;
    }
    let response;
    try {
        response = await fetch(API_BASE + path, {
            method,
            headers,
            body: body === undefined ? undefined : JSON.stringify(body),
            credentials: 'omit',
            cache: 'no-store',
        });
    } catch {
        throw new ApiError(NETWORK_ERROR);
    }
    const data = await readJson(response);
    if (response.status === 401 && auth) {
        const message = typeof data?.message === 'string' && data.message.trim() ? data.message : SESSION_EXPIRED;
        writeToken(null);
        showLogin(message);
        throw new ApiError(message, { status: 401, code: data?.error ?? null, sessionExpired: true });
    }
    if (!response.ok) {
        throw new ApiError(errorMessageFrom(response.status, data), { status: response.status, code: data?.error ?? null });
    }
    return data;
}

async function readJson(response) {
    if (response.status === 204) {
        return null;
    }
    try {
        return await response.json();
    } catch {
        return null;
    }
}

// ---------- Utilidades de DOM ----------

function el(tag, props = {}, ...children) {
    const node = document.createElement(tag);
    for (const [key, value] of Object.entries(props)) {
        if (value === undefined || value === null || value === false) {
            continue;
        }
        if (key === 'className') {
            node.className = value;
        } else if (key === 'text') {
            node.textContent = value;
        } else if (key.startsWith('on') && typeof value === 'function') {
            node.addEventListener(key.slice(2).toLowerCase(), value);
        } else if (key in node && typeof value !== 'string') {
            node[key] = value;
        } else {
            node.setAttribute(key, value === true ? '' : value);
        }
    }
    for (const child of children.flat()) {
        if (child !== null && child !== undefined && child !== false) {
            node.append(child);
        }
    }
    return node;
}

const ICON_PATHS = {
    up: 'M6 15l6-6 6 6',
    down: 'M6 9l6 6 6-6',
    remove: 'M6 6l12 12M18 6L6 18',
};

function icon(name) {
    const ns = 'http://www.w3.org/2000/svg';
    const svg = document.createElementNS(ns, 'svg');
    svg.setAttribute('viewBox', '0 0 24 24');
    svg.setAttribute('fill', 'none');
    svg.setAttribute('stroke', 'currentColor');
    svg.setAttribute('stroke-width', '2');
    svg.setAttribute('stroke-linecap', 'round');
    svg.setAttribute('stroke-linejoin', 'round');
    svg.setAttribute('aria-hidden', 'true');
    const path = document.createElementNS(ns, 'path');
    path.setAttribute('d', ICON_PATHS[name]);
    svg.append(path);
    return svg;
}

function showAlert(node, message) {
    node.textContent = message ?? '';
    node.hidden = !message;
}

let toastTimer = null;
function toast(message) {
    const node = $('toast');
    node.textContent = message;
    node.classList.add('is-visible');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => node.classList.remove('is-visible'), TOAST_MS);
}

/** Deshabilita los botones mientras dura `task` y cambia el texto del principal. */
async function whileBusy(buttons, busyLabel, task) {
    const [main] = buttons;
    const label = main.textContent;
    const previous = buttons.map((b) => b.disabled);
    buttons.forEach((b) => { b.disabled = true; });
    main.textContent = busyLabel;
    main.setAttribute('aria-busy', 'true');
    try {
        return await task();
    } finally {
        main.textContent = label;
        main.removeAttribute('aria-busy');
        buttons.forEach((b, i) => { b.disabled = previous[i]; });
    }
}

function clearFieldErrors(scope) {
    scope.querySelectorAll('[aria-invalid="true"]').forEach((n) => {
        n.removeAttribute('aria-invalid');
        n.removeAttribute('aria-describedby');
    });
    scope.querySelectorAll('.field-error[data-generated]').forEach((n) => n.remove());
    scope.querySelectorAll('.field-error[data-error-for]').forEach((n) => showAlert(n, ''));
}

/** Pinta los errores de validación junto a sus campos y lleva el foco al primero. */
function showFieldErrors(scope, errors) {
    clearFieldErrors(scope);
    let first = null;
    for (const { field, message } of errors) {
        const fixed = scope.querySelector(`[data-error-for="${field}"]`);
        if (fixed) {
            showAlert(fixed, message);
            first ??= scope.querySelector('#day-add-row') ?? fixed;
            continue;
        }
        const input = scope.querySelector(`[data-field="${field}"]`);
        if (!input) {
            continue;
        }
        const id = `${input.id}-error`;
        input.setAttribute('aria-invalid', 'true');
        input.setAttribute('aria-describedby', id);
        input.closest('.field').append(el('p', { className: 'field-error', id, 'data-generated': 'true', text: message }));
        first ??= input;
    }
    first?.focus();
}

// ---------- Vistas ----------

function showLogin(notice) {
    $('app-view').hidden = true;
    $('login-view').hidden = false;
    showAlert($('login-notice'), notice ?? '');
    showAlert($('login-error'), '');
    $('login-password').value = '';
    $('login-username').focus();
}

function showApp() {
    $('login-view').hidden = true;
    $('app-view').hidden = false;
    selectTab('exercises');
}

function selectTab(name) {
    document.querySelectorAll('.tab').forEach((tab) => {
        if (tab.dataset.tab === name) {
            tab.setAttribute('aria-current', 'page');
        } else {
            tab.removeAttribute('aria-current');
        }
    });
    $('exercises-view').hidden = name !== 'exercises';
    $('program-view').hidden = name !== 'program';
    if (name === 'exercises') {
        loadExercises();
    } else {
        loadProgram();
    }
}

// ---------- Login ----------

async function onLogin(event) {
    event.preventDefault();
    const username = $('login-username').value.trim();
    const password = $('login-password').value;
    if (!username || !password) {
        showAlert($('login-error'), 'Escribe el usuario y la contraseña');
        return;
    }
    showAlert($('login-error'), '');
    try {
        await whileBusy([$('login-submit')], 'Entrando…', async () => {
            const data = await api('POST', '/auth/login', { username, password }, { auth: false });
            if (!data || typeof data.token !== 'string' || !data.token) {
                throw new ApiError('El servidor no devolvió una sesión válida');
            }
            writeToken(data.token);
        });
        $('login-password').value = '';
        showAlert($('login-notice'), '');
        showApp();
    } catch (err) {
        showAlert($('login-error'), err.message);
    }
}

async function onLogout() {
    const button = $('logout');
    try {
        await whileBusy([button], 'Saliendo…', () => api('POST', '/auth/logout'));
    } catch (err) {
        if (err.sessionExpired) {
            return;
        }
        // Si el servidor no responde, la sesión local se borra igualmente.
    }
    writeToken(null);
    resetState();
    showLogin('Has salido del backoffice');
}

function resetState() {
    state.exercises = [];
    state.activeExercises = [];
    state.editingExercise = null;
    state.program = null;
    state.programMissing = false;
    state.dayEdit = null;
    $('exercise-editor').hidden = true;
    $('day-editor').hidden = true;
}

// ---------- Ejercicios ----------

let exercisesRequest = 0;

async function loadExercises() {
    const request = ++exercisesRequest;
    const includeArchived = $('show-archived').checked;
    const status = $('exercise-list-status');
    const list = $('exercise-list');
    if (state.exercises.length === 0) {
        list.replaceChildren(...[0, 1, 2].map(() => el('li', { className: 'skeleton', 'aria-hidden': 'true' })));
    }
    status.textContent = 'Cargando ejercicios…';
    try {
        const data = await api('GET', `/admin/exercises?includeArchived=${includeArchived}`);
        if (request !== exercisesRequest) {
            return;
        }
        state.exercises = Array.isArray(data) ? data : [];
        status.textContent = state.exercises.length === 0
            ? (includeArchived ? 'No hay ejercicios.' : 'No hay ejercicios todavía. Crea el primero.')
            : '';
        renderExerciseList();
    } catch (err) {
        if (request !== exercisesRequest || err.sessionExpired) {
            return;
        }
        list.replaceChildren();
        status.textContent = err.message;
    }
}

function renderExerciseList() {
    const selectedId = state.editingExercise?.id ?? null;
    $('exercise-list').replaceChildren(...state.exercises.map((exercise) => {
        const meta = [exercise.muscleGroup, exercise.archived ? 'Archivado' : null].filter(Boolean).join(' · ');
        return el('li', {},
            el('button', {
                type: 'button',
                className: 'exercise-item' + (exercise.archived ? ' is-archived' : ''),
                'aria-current': exercise.id === selectedId ? 'true' : null,
                onClick: () => openExerciseEditor(exercise),
            },
            thumbnailFor(exercise.youtubeVideoId),
            el('span', { className: 'exercise-text' },
                el('span', { className: 'exercise-name', text: exercise.name }),
                meta ? el('span', { className: 'exercise-meta', text: meta }) : null)));
    }));
}

function thumbnailFor(videoId) {
    if (typeof videoId === 'string' && VIDEO_ID.test(videoId)) {
        return el('img', { className: 'exercise-thumb', src: youtubeThumbnailUrl(videoId), alt: '', loading: 'lazy', width: '72', height: '40' });
    }
    return el('span', { className: 'exercise-thumb', 'aria-hidden': 'true' });
}

function openExerciseEditor(exercise) {
    state.editingExercise = exercise;
    const form = $('exercise-form');
    clearFieldErrors(form);
    showAlert($('exercise-error'), '');
    $('exercise-editor-title').textContent = exercise ? 'Editar ejercicio' : 'Nuevo ejercicio';
    $('ex-name').value = exercise?.name ?? '';
    $('ex-youtube').value = exercise?.youtubeUrl ?? '';
    $('ex-muscle').value = exercise?.muscleGroup ?? '';
    $('ex-instructions').value = exercise?.instructions ?? '';
    $('exercise-archived-badge').hidden = !exercise?.archived;
    $('exercise-archive').hidden = !exercise || exercise.archived;
    $('exercise-restore').hidden = !exercise?.archived;
    updateYoutubePreview();
    $('exercise-editor').hidden = false;
    renderExerciseList();
    $('ex-name').focus();
}

function closeExerciseEditor() {
    state.editingExercise = null;
    $('exercise-editor').hidden = true;
    renderExerciseList();
}

function exerciseFormValues() {
    return {
        name: $('ex-name').value,
        youtubeUrl: $('ex-youtube').value,
        muscleGroup: $('ex-muscle').value,
        instructions: $('ex-instructions').value,
    };
}

function updateYoutubePreview() {
    const id = youtubeVideoId($('ex-youtube').value);
    $('ex-preview').hidden = id === null;
    if (id !== null) {
        $('ex-thumb').src = youtubeThumbnailUrl(id);
        $('ex-open').href = youtubeWatchUrl(id);
        // Si estaba marcado como no válido y ya lo es, quita el aviso.
        const input = $('ex-youtube');
        if (input.getAttribute('aria-invalid') === 'true') {
            input.removeAttribute('aria-invalid');
            input.removeAttribute('aria-describedby');
            $('ex-youtube-error')?.remove();
        }
    } else {
        $('ex-thumb').removeAttribute('src');
    }
}

function checkYoutubeOnChange() {
    const input = $('ex-youtube');
    const errors = validateExercise({ name: 'x', youtubeUrl: input.value }).filter((e) => e.field === 'youtubeUrl');
    if (errors.length > 0 && input.getAttribute('aria-invalid') !== 'true') {
        input.setAttribute('aria-invalid', 'true');
        input.setAttribute('aria-describedby', 'ex-youtube-error');
        input.closest('.field').append(el('p', { className: 'field-error', id: 'ex-youtube-error', 'data-generated': 'true', text: errors[0].message }));
    }
}

function exerciseButtons() {
    return [$('exercise-save'), $('exercise-archive'), $('exercise-restore'), $('exercise-cancel')];
}

async function onSaveExercise(event) {
    event.preventDefault();
    const form = $('exercise-form');
    const values = exerciseFormValues();
    const errors = validateExercise(values);
    showAlert($('exercise-error'), '');
    if (errors.length > 0) {
        showFieldErrors(form, errors);
        return;
    }
    clearFieldErrors(form);
    const editing = state.editingExercise;
    const body = buildExerciseBody(values);
    try {
        const saved = await whileBusy(exerciseButtons(), 'Guardando…', () => editing
            ? api('PUT', `/admin/exercises/${editing.id}`, { ...body, archived: Boolean(editing.archived) })
            : api('POST', '/admin/exercises', body));
        toast(editing ? 'Ejercicio guardado' : 'Ejercicio creado');
        state.editingExercise = saved ?? editing;
        if (saved) {
            openExerciseEditor(saved);
        }
        await loadExercises();
    } catch (err) {
        if (!err.sessionExpired) {
            showAlert($('exercise-error'), err.message);
        }
    }
}

async function onArchiveExercise() {
    const editing = state.editingExercise;
    if (!editing) {
        return;
    }
    showAlert($('exercise-error'), '');
    try {
        await whileBusy([$('exercise-archive'), $('exercise-save'), $('exercise-cancel')], 'Archivando…',
            () => api('DELETE', `/admin/exercises/${editing.id}`));
        toast(`«${editing.name}» archivado`);
        if ($('show-archived').checked) {
            openExerciseEditor({ ...editing, archived: true });
        } else {
            closeExerciseEditor();
        }
        await loadExercises();
    } catch (err) {
        if (!err.sessionExpired) {
            showAlert($('exercise-error'), err.message);
        }
    }
}

async function onRestoreExercise() {
    const editing = state.editingExercise;
    if (!editing) {
        return;
    }
    const values = exerciseFormValues();
    const errors = validateExercise(values);
    if (errors.length > 0) {
        showFieldErrors($('exercise-form'), errors);
        return;
    }
    showAlert($('exercise-error'), '');
    try {
        const saved = await whileBusy([$('exercise-restore'), $('exercise-save'), $('exercise-cancel')], 'Recuperando…',
            () => api('PUT', `/admin/exercises/${editing.id}`, { ...buildExerciseBody(values), archived: false }));
        toast(`«${editing.name}» recuperado`);
        openExerciseEditor(saved ?? { ...editing, archived: false });
        await loadExercises();
    } catch (err) {
        if (!err.sessionExpired) {
            showAlert($('exercise-error'), err.message);
        }
    }
}

// ---------- Programa ----------

let programRequest = 0;

async function loadProgram() {
    const request = ++programRequest;
    const status = $('program-status');
    showAlert($('program-load-error'), '');
    if (!state.program && !state.programMissing) {
        status.textContent = 'Cargando programa…';
    }
    try {
        const [program, exercises] = await Promise.all([
            api('GET', '/admin/program').catch((err) => {
                if (err.code === 'no_active_program') {
                    return null;
                }
                throw err;
            }),
            api('GET', '/admin/exercises?includeArchived=false'),
        ]);
        if (request !== programRequest) {
            return;
        }
        status.textContent = '';
        state.program = program;
        state.programMissing = program === null;
        state.activeExercises = Array.isArray(exercises) ? exercises : [];
        renderProgram();
    } catch (err) {
        if (request !== programRequest || err.sessionExpired) {
            return;
        }
        status.textContent = '';
        showAlert($('program-load-error'), err.message);
    }
}

function renderProgram() {
    const program = state.program;
    $('program-panel').hidden = false;
    $('program-empty').hidden = !state.programMissing;
    $('program-form-title').textContent = state.programMissing ? 'Crear programa' : 'Programa activo';
    $('program-save').textContent = state.programMissing ? 'Crear programa' : 'Guardar';
    if (program) {
        $('pr-name').value = program.name ?? '';
        $('pr-start').value = program.startDate ?? '';
    }
    $('week-panel').hidden = state.programMissing || state.dayEdit !== null;
    renderWeek();
}

function renderWeek() {
    $('week').replaceChildren(...weekDays(state.program).map(({ dayOfWeek, name, day }) => {
        const count = day?.exercises?.length ?? 0;
        return el('li', {},
            el('article', { className: 'day-card' + (day ? '' : ' is-rest') },
                el('div', { className: 'day-card-body' },
                    el('p', { className: 'eyebrow', text: name }),
                    el('p', { className: 'day-card-name', text: day ? day.name : 'Descanso' }),
                    day?.muscles ? el('p', { className: 'day-card-muscles', text: day.muscles }) : null,
                    day ? el('p', { className: 'day-card-count' },
                        el('span', { className: 'num', text: String(count) }),
                        count === 1 ? 'ejercicio' : 'ejercicios') : null),
                el('button', {
                    type: 'button',
                    className: 'btn btn-ghost',
                    'aria-label': `${day ? 'Editar' : 'Añadir entreno el'} ${name.toLowerCase()}`,
                    text: day ? 'Editar' : 'Añadir entreno',
                    onClick: () => openDayEditor(dayOfWeek),
                })));
    }));
}

async function onSaveProgram(event) {
    event.preventDefault();
    const form = $('program-form');
    const values = { name: $('pr-name').value, startDate: $('pr-start').value };
    const errors = validateProgram(values);
    showAlert($('program-error'), '');
    if (errors.length > 0) {
        showFieldErrors(form, errors);
        return;
    }
    clearFieldErrors(form);
    const creating = state.programMissing;
    try {
        const program = await whileBusy([$('program-save')], creating ? 'Creando…' : 'Guardando…',
            () => api('PUT', '/admin/program', buildProgramBody(values)));
        state.program = program;
        state.programMissing = false;
        renderProgram();
        toast(creating ? 'Programa creado' : 'Programa guardado');
    } catch (err) {
        if (!err.sessionExpired) {
            showAlert($('program-error'), err.message);
        }
    }
}

// ---------- Editor de un día ----------

function openDayEditor(dayOfWeek) {
    const day = state.program?.days?.find((d) => d.dayOfWeek === dayOfWeek) ?? null;
    const form = dayFormFromProgramDay(day);
    form.exercises = form.exercises.map(withKey);
    state.dayEdit = { dayOfWeek, existing: day !== null, form };
    const editor = $('day-form');
    clearFieldErrors(editor);
    showAlert($('day-error'), '');
    $('rest-confirm').hidden = true;
    $('day-editor-title').textContent = DAY_NAMES[dayOfWeek - 1];
    $('day-name').value = form.name;
    $('day-muscles').value = form.muscles;
    $('day-to-rest').hidden = day === null;
    renderDayRows();
    $('week-panel').hidden = true;
    $('day-editor').hidden = false;
    $('day-editor-title').focus();
}

function closeDayEditor() {
    const dayOfWeek = state.dayEdit?.dayOfWeek;
    state.dayEdit = null;
    $('day-editor').hidden = true;
    $('week-panel').hidden = state.programMissing;
    renderWeek();
    const cards = $('week').querySelectorAll('button');
    cards[(dayOfWeek ?? 1) - 1]?.focus();
}

function withKey(row) {
    state.rowKey += 1;
    return { ...row, key: state.rowKey };
}

function renderDayRows(focus) {
    const rows = state.dayEdit.form.exercises;
    const activeIds = new Set(state.activeExercises.map((e) => e.id));
    $('day-rows').replaceChildren(...rows.map((row, index) => dayRow(row, index, rows.length, activeIds)));
    if (focus) {
        const target = document.querySelector(`[data-row-key="${focus.key}"] [data-action="${focus.action}"]`);
        const fallback = document.querySelector(`[data-row-key="${focus.key}"] [data-action]:not(:disabled)`);
        (target && !target.disabled ? target : fallback)?.focus();
    }
}

function dayRow(row, index, total, activeIds) {
    const prefix = `row-${row.key}`;
    const update = (field) => (event) => { row[field] = event.target.value; };

    const select = el('select', { id: `${prefix}-exercise`, 'data-field': `exercises.${index}.exerciseId`, onChange: update('exerciseId') },
        el('option', { value: '', text: 'Elige un ejercicio' }),
        state.activeExercises.map((e) => el('option', { value: String(e.id), text: e.muscleGroup ? `${e.name} · ${e.muscleGroup}` : e.name })));
    if (row.exerciseId !== '' && !activeIds.has(Number(row.exerciseId))) {
        select.append(el('option', { value: row.exerciseId, text: `${row.exerciseName || 'Ejercicio ' + row.exerciseId} (archivado)` }));
    }
    select.value = row.exerciseId;

    const numberField = (field, label, attrs) => el('div', { className: 'field' },
        el('label', { for: `${prefix}-${field}`, text: label }),
        el('input', {
            id: `${prefix}-${field}`, type: 'number', inputmode: 'numeric', step: '1',
            'data-field': `exercises.${index}.${field}`, value: row[field], onInput: update(field), ...attrs,
        }));

    const actionButton = (action, label, disabled, onClick) => el('button', {
        type: 'button', className: 'btn icon-btn', 'data-action': action, 'aria-label': label, disabled, onClick,
    }, icon(action));

    const position = index + 1;
    return el('li', { className: 'day-row', 'data-row-key': String(row.key) },
        el('span', { className: 'day-row-pos', 'aria-hidden': 'true', text: String(position) }),
        el('div', { className: 'day-row-main' },
            el('div', { className: 'field' },
                el('label', { for: `${prefix}-exercise`, text: `Ejercicio ${position}` }),
                select),
            el('div', { className: 'day-row-numbers' },
                numberField('sets', 'Series', { min: '1', max: '20' }),
                numberField('repsMin', 'Reps mín.', { min: '1', max: '100' }),
                numberField('repsMax', 'Reps máx.', { min: '1', max: '100' }),
                numberField('restSeconds', 'Descanso (s)', { min: '0', max: '900', placeholder: '—' }))),
        el('div', { className: 'day-row-actions' },
            actionButton('up', `Subir el ejercicio ${position}`, index === 0, () => moveRow(index, -1, row.key, 'up')),
            actionButton('down', `Bajar el ejercicio ${position}`, index === total - 1, () => moveRow(index, 1, row.key, 'down')),
            actionButton('remove', `Quitar el ejercicio ${position}`, false, () => removeRow(index))));
}

function moveRow(index, delta, key, action) {
    state.dayEdit.form.exercises = moveItem(state.dayEdit.form.exercises, index, delta);
    renderDayRows({ key, action });
}

function removeRow(index) {
    const rows = removeItem(state.dayEdit.form.exercises, index);
    state.dayEdit.form.exercises = rows;
    renderDayRows();
    const next = rows[Math.min(index, rows.length - 1)];
    if (next) {
        document.querySelector(`[data-row-key="${next.key}"] [data-action="remove"]`)?.focus();
    } else {
        $('day-add-row').focus();
    }
}

function addRow() {
    const row = withKey(NEW_ROW);
    state.dayEdit.form.exercises = [...state.dayEdit.form.exercises, row];
    renderDayRows();
    $(`row-${row.key}-exercise`)?.focus();
}

function dayButtons() {
    return [$('day-save'), $('day-to-rest'), $('day-back'), $('day-add-row')];
}

async function onSaveDay(event) {
    event.preventDefault();
    const edit = state.dayEdit;
    if (!edit) {
        return;
    }
    edit.form.name = $('day-name').value;
    edit.form.muscles = $('day-muscles').value;
    const form = $('day-form');
    const activeIds = new Set(state.activeExercises.map((e) => e.id));
    const errors = validateDay(edit.form, activeIds);
    showAlert($('day-error'), '');
    if (errors.length > 0) {
        showFieldErrors(form, errors);
        return;
    }
    clearFieldErrors(form);
    try {
        const program = await whileBusy(dayButtons(), 'Guardando…',
            () => api('PUT', `/admin/program/days/${edit.dayOfWeek}`, buildDayBody(edit.form)));
        if (program) {
            state.program = program;
        }
        toast(`${DAY_NAMES[edit.dayOfWeek - 1]} guardado`);
        closeDayEditor();
    } catch (err) {
        if (err.sessionExpired) {
            return;
        }
        if (err.code === 'no_active_program') {
            state.program = null;
            state.programMissing = true;
            closeDayEditor();
            renderProgram();
            showAlert($('program-error'), err.message);
            return;
        }
        showAlert($('day-error'), err.message);
    }
}

function askRestConfirm() {
    $('rest-confirm').hidden = false;
    $('day-to-rest').setAttribute('aria-expanded', 'true');
    $('rest-confirm-no').focus();
}

function cancelRestConfirm() {
    $('rest-confirm').hidden = true;
    $('day-to-rest').setAttribute('aria-expanded', 'false');
    $('day-to-rest').focus();
}

async function onConfirmRest() {
    const edit = state.dayEdit;
    if (!edit) {
        return;
    }
    showAlert($('day-error'), '');
    try {
        await whileBusy([$('rest-confirm-yes'), $('rest-confirm-no'), ...dayButtons()], 'Convirtiendo…',
            () => api('DELETE', `/admin/program/days/${edit.dayOfWeek}`));
        if (state.program?.days) {
            state.program = { ...state.program, days: state.program.days.filter((d) => d.dayOfWeek !== edit.dayOfWeek) };
        }
        toast(`${DAY_NAMES[edit.dayOfWeek - 1]} es ahora descanso`);
        closeDayEditor();
    } catch (err) {
        if (!err.sessionExpired) {
            $('rest-confirm').hidden = true;
            showAlert($('day-error'), err.message);
        }
    }
}

// ---------- Arranque ----------

function wire() {
    $('login-form').addEventListener('submit', onLogin);
    $('logout').addEventListener('click', onLogout);
    document.querySelectorAll('.tab').forEach((tab) => tab.addEventListener('click', () => selectTab(tab.dataset.tab)));

    $('show-archived').addEventListener('change', loadExercises);
    $('new-exercise').addEventListener('click', () => openExerciseEditor(null));
    $('exercise-form').addEventListener('submit', onSaveExercise);
    $('exercise-cancel').addEventListener('click', closeExerciseEditor);
    $('exercise-archive').addEventListener('click', onArchiveExercise);
    $('exercise-restore').addEventListener('click', onRestoreExercise);
    $('ex-youtube').addEventListener('input', updateYoutubePreview);
    $('ex-youtube').addEventListener('change', checkYoutubeOnChange);

    $('program-form').addEventListener('submit', onSaveProgram);
    $('day-form').addEventListener('submit', onSaveDay);
    $('day-back').addEventListener('click', closeDayEditor);
    $('day-add-row').addEventListener('click', addRow);
    $('day-to-rest').addEventListener('click', askRestConfirm);
    $('day-to-rest').setAttribute('aria-controls', 'rest-confirm');
    $('day-to-rest').setAttribute('aria-expanded', 'false');
    $('rest-confirm-no').addEventListener('click', cancelRestConfirm);
    $('rest-confirm-yes').addEventListener('click', onConfirmRest);
}

wire();
if (readToken()) {
    showApp();
} else {
    showLogin();
}
