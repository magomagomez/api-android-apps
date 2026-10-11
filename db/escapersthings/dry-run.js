// Ensayo de la migración A contra una base real: BEGIN, migración, recuentos y SIEMPRE
// ROLLBACK. No deja ningún cambio.
//
// pg se instala fuera del proyecto Java (no hay package.json en el repo):
//   mkdir -p /tmp/escapers-pg && (cd /tmp/escapers-pg && npm i --no-save pg)
// Ejecutar (la URL de Heroku sale de `heroku config:get DATABASE_URL -a api-android-app`):
//   DATABASE_URL=... NODE_PATH=/tmp/escapers-pg/node_modules node db/escapersthings/dry-run.js

const fs = require('node:fs');
const path = require('node:path');
const { Client } = require('pg');
const { printChecks, runChecks } = require('./checks');

const MIGRATION = path.join(__dirname, '2026-10-11-escapers-v2.sql');

// La transacción la abre y la cierra este script: se quitan el BEGIN y el COMMIT del fichero.
function migrationBody() {
  const sql = fs.readFileSync(MIGRATION, 'utf8');
  const control = sql.match(/^(BEGIN|COMMIT);\s*$/gm) || [];
  if (control.length !== 2) {
    throw new Error(`Se esperaba un BEGIN y un COMMIT en la migración; hay ${control.length}`);
  }
  return sql.replace(/^(BEGIN|COMMIT);\s*$/gm, '');
}

async function main() {
  if (!process.env.DATABASE_URL) throw new Error('Falta la variable de entorno DATABASE_URL');
  const body = migrationBody();
  const client = new Client({ connectionString: process.env.DATABASE_URL, ssl: { rejectUnauthorized: false } });
  await client.connect();
  try {
    await client.query('BEGIN');
    await client.query(body);
    printChecks(await runChecks(client));
  } finally {
    await client.query('ROLLBACK');
    console.log('\nROLLBACK hecho: la base de datos queda como estaba.');
    await client.end();
  }
}

main().catch((e) => { console.error(`Error: ${e.message}`); process.exit(1); });
