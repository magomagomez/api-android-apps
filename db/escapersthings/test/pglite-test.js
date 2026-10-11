// Prueba la migración A en un Postgres en memoria (PGlite, WASM): sin Docker ni servidor.
// Crea el esquema v1, carga una copia de los datos de v1, ejecuta la migración dos veces y
// comprueba los resultados y que la segunda vez no cambia nada.
//
// Datos (no se versionan: son notas reales). Una carpeta con:
//   escapes.json   -> GET /escapers-api/v1/escapeRooms   (id, nombre, finalizado, ciudad, tipo,
//                     finalizado_javi, finalizado_cris)
//   rg/<id>.json   -> GET /escapers-api/v1/escapeRooms/<id>/grades   (nombre, grades{...})
//
// Uso (PGlite en una carpeta temporal, fuera del repo):
//   mkdir -p /tmp/pglite && (cd /tmp/pglite && npm i --no-save @electric-sql/pglite)
//   NODE_PATH=/tmp/pglite/node_modules node db/escapersthings/test/pglite-test.js <carpeta-datos>
// Sale con código 1 si alguna comprobación falla.

const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { PGlite } = require('@electric-sql/pglite');
const { CATEGORIES, runChecks, printChecks } = require('../checks');

const MIGRATION = path.join(__dirname, '..', '2026-10-11-escapers-v2.sql');
const USERS = ['Magomez', 'Cris', 'Alex', 'Sara', 'Uri'];
// Notas reales de salas que ya no existen (sala -> usuarios).
const ORPHAN_GRADES = { 82: [1, 2, 3, 4, 5], 83: [1, 2, 3, 4, 5], 84: [1, 2, 4, 5], 200: [1], 0: [5] };

const V1_SCHEMA = `
  CREATE TABLE users (
    id serial PRIMARY KEY, name varchar NOT NULL, password varchar NOT NULL DEFAULT '0');
  CREATE TABLE escape_rooms (
    id serial PRIMARY KEY, name varchar NOT NULL, image_url varchar, done int DEFAULT 0,
    ubication varchar, type varchar NOT NULL, javi_done int, cris_done int,
    global_total numeric, terror_total numeric);
  CREATE TABLE user_categoria_nota (
    user_id int, escape_room_id int, enigma numeric, game_master numeric, inmersion numeric,
    horror numeric, global numeric, PRIMARY KEY (user_id, escape_room_id));`;

async function loadData(db, dataDir) {
  for (const name of USERS) {
    await db.query('INSERT INTO users (name, password) VALUES ($1, $2)', [name, 'x']);
  }
  const rooms = JSON.parse(fs.readFileSync(path.join(dataDir, 'escapes.json'), 'utf8'));
  for (const r of rooms) {
    await db.query(
      `INSERT INTO escape_rooms (id, name, image_url, done, ubication, type, javi_done, cris_done,
                                 global_total, terror_total)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)`,
      [r.id, r.nombre, r.imagen_path, r.finalizado, r.ciudad, r.tipo, r.finalizado_javi,
       r.finalizado_cris, r.nota_total, r.terror_total]);
    const gradesFile = path.join(dataDir, 'rg', `${r.id}.json`);
    const grades = fs.existsSync(gradesFile) ? JSON.parse(fs.readFileSync(gradesFile, 'utf8')) : [];
    for (const g of grades) {
      const userId = USERS.indexOf(g.nombre) + 1;
      assert.ok(userId > 0, `Usuario desconocido ${g.nombre} en la sala ${r.id}`);
      await insertGrade(db, userId, r.id, g.grades);
    }
  }
  await db.query("SELECT setval('escape_rooms_id_seq', (SELECT max(id) FROM escape_rooms))");
  for (const [roomId, userIds] of Object.entries(ORPHAN_GRADES)) {
    for (const userId of userIds) {
      await insertGrade(db, userId, Number(roomId), { enigma: 5, game_master: 5, inmersion: 5, terror: 5, global: 5 });
    }
  }
  // Nota de un usuario que no existe.
  await insertGrade(db, 99, rooms[0].id, { global: 5 });
}

function insertGrade(db, userId, roomId, g) {
  return db.query(
    `INSERT INTO user_categoria_nota (user_id, escape_room_id, enigma, game_master, inmersion, horror, global)
     VALUES ($1, $2, $3, $4, $5, $6, $7)`,
    [userId, roomId, g.enigma ?? null, g.game_master ?? null, g.inmersion ?? null, g.terror ?? null, g.global ?? null]);
}

// Foto completa de los datos para comparar la primera y la segunda ejecución.
async function snapshot(db) {
  const tables = {
    users: 'SELECT * FROM users ORDER BY id',
    escape_rooms: 'SELECT * FROM escape_rooms ORDER BY id',
    user_categoria_nota: 'SELECT * FROM user_categoria_nota ORDER BY user_id, escape_room_id',
    escape_room_players: 'SELECT * FROM escape_room_players ORDER BY escape_room_id, user_id',
    constraints: "SELECT conrelid::regclass::text, conname, pg_get_constraintdef(oid) FROM pg_constraint WHERE connamespace = 'public'::regnamespace ORDER BY 1, 2",
  };
  const snap = {};
  for (const [name, sql] of Object.entries(tables)) snap[name] = (await db.query(sql)).rows;
  return JSON.stringify(snap);
}

function verify(c) {
  assert.equal(c.notasHuerfanas, 0, 'quedan notas huérfanas');
  assert.equal(c.notasSinJugador, 0, 'hay notas de quien no figura como jugador');
  for (const u of c.jugadoresPorUsuario) {
    assert.ok(u.jugadas >= u.notas, `${u.name}: ${u.jugadas} jugadas < ${u.notas} notas`);
  }
  for (const { categoria } of c.categorias) assert.ok(CATEGORIES.includes(categoria), `categoría ${categoria}`);
  for (const { ciudad } of c.ciudades) {
    assert.ok(ciudad === null || ciudad === ciudad.trim(), `ciudad con espacios "${ciudad}"`);
    assert.ok(!['Barceona', 'Hospitalet', 'Cornella', 'Rubi', 'Esparraguera', ''].includes(ciudad), `ciudad ${ciudad}`);
  }
  const special = Object.fromEntries(c.salasEspeciales.map((r) => [r.name, r.jugadores]));
  assert.deepEqual(special, {
    'Misterio de Kemet': '1,2,3,4,5', Olimpo: '1,2,3,4,5', Exorcismo: '1,2,3,5', In: '1,2,3,5',
  });
  // 2 de notas (con nombre fijo), 2 de jugadores, 1 de fotos y 1 de tokens.
  assert.equal(c.clavesAjenas.length, 6, 'número de claves ajenas');
  const names = c.clavesAjenas.map((k) => k.nombre);
  assert.ok(names.includes('user_categoria_nota_escape_room_fk'), 'falta user_categoria_nota_escape_room_fk');
  assert.ok(names.includes('user_categoria_nota_user_fk'), 'falta user_categoria_nota_user_fk');
  assert.ok(c.clavesAjenas.every((k) => k.definicion.includes('ON DELETE CASCADE')), 'clave ajena sin cascada');
}

const BEFORE = {
  Salas: 'SELECT count(*)::int AS n FROM escape_rooms',
  Notas: 'SELECT count(*)::int AS n FROM user_categoria_nota',
  'Notas huérfanas': `
    SELECT count(*)::int AS n FROM user_categoria_nota n
     WHERE NOT EXISTS (SELECT 1 FROM escape_rooms e WHERE e.id = n.escape_room_id)
        OR NOT EXISTS (SELECT 1 FROM users u WHERE u.id = n.user_id)`,
  'Categorías distintas': 'SELECT count(DISTINCT type)::int AS n FROM escape_rooms',
  'Ciudades distintas': 'SELECT count(DISTINCT ubication)::int AS n FROM escape_rooms',
};

async function main() {
  const dataDir = process.argv[2];
  if (!dataDir) throw new Error('Uso: node pglite-test.js <carpeta-datos>');
  const sql = fs.readFileSync(MIGRATION, 'utf8');
  const db = new PGlite();
  await db.exec(V1_SCHEMA);
  await loadData(db, dataDir);

  console.log('=== Antes');
  for (const [label, q] of Object.entries(BEFORE)) {
    console.log(`${label}: ${(await db.query(q)).rows[0].n}`);
  }
  await db.exec(sql);
  console.log('\n=== Después de la primera ejecución');
  const first = await runChecks(db);
  printChecks(first);
  verify(first);

  const before = await snapshot(db);
  await db.exec(sql);
  assert.equal(await snapshot(db), before, 'la segunda ejecución cambió datos o esquema');
  console.log('\nSegunda ejecución: sin cambios. Todas las comprobaciones OK.');
  await db.close();
}

main().catch((e) => { console.error(`FALLO: ${e.message}`); process.exit(1); });
