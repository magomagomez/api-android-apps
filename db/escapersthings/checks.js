// Recuentos de comprobación de la migración A, compartidos por dry-run.js (pg) y por
// test/pglite-test.js (PGlite). `db` es cualquier objeto con query(sql) -> { rows }.

const CATEGORIES = ['Aventura', 'Terror', 'Cinéfilo', 'Tensión', 'Histórico', 'Hall Escape', 'Battle Royale'];

const QUERIES = {
  salas: 'SELECT count(*)::int AS n FROM escape_rooms',
  notas: 'SELECT count(*)::int AS n FROM user_categoria_nota',
  notasHuerfanas: `
    SELECT count(*)::int AS n FROM user_categoria_nota n
     WHERE NOT EXISTS (SELECT 1 FROM escape_rooms e WHERE e.id = n.escape_room_id)
        OR NOT EXISTS (SELECT 1 FROM users u WHERE u.id = n.user_id)`,
  notasSinJugador: `
    SELECT count(*)::int AS n FROM user_categoria_nota n
     WHERE NOT EXISTS (SELECT 1 FROM escape_room_players p
                        WHERE p.escape_room_id = n.escape_room_id AND p.user_id = n.user_id)`,
  jugadoresPorUsuario: `
    SELECT u.id, u.name,
           (SELECT count(*)::int FROM escape_room_players p WHERE p.user_id = u.id) AS jugadas,
           (SELECT count(*)::int FROM user_categoria_nota n WHERE n.user_id = u.id) AS notas
      FROM users u ORDER BY u.id`,
  categorias: 'SELECT type AS categoria, count(*)::int AS salas FROM escape_rooms GROUP BY 1 ORDER BY 2 DESC, 1',
  ciudades: 'SELECT ubication AS ciudad, count(*)::int AS salas FROM escape_rooms GROUP BY 1 ORDER BY 2 DESC, 1',
  salasSinJugadores: `
    SELECT e.id, e.name FROM escape_rooms e
     WHERE NOT EXISTS (SELECT 1 FROM escape_room_players p WHERE p.escape_room_id = e.id)
     ORDER BY e.name`,
  salasEspeciales: `
    SELECT e.id, e.name, string_agg(p.user_id::text, ',' ORDER BY p.user_id) AS jugadores
      FROM escape_rooms e LEFT JOIN escape_room_players p ON p.escape_room_id = e.id
     WHERE lower(trim(e.name)) IN ('misterio de kemet', 'olimpo', 'exorcismo', 'in')
     GROUP BY e.id, e.name ORDER BY e.name`,
  clavesAjenas: `
    SELECT conrelid::regclass::text AS tabla, conname AS nombre, pg_get_constraintdef(oid) AS definicion
      FROM pg_constraint
     WHERE contype = 'f'
       AND conrelid::regclass::text IN ('user_categoria_nota', 'escape_room_players',
                                        'escape_room_photos', 'escapers_tokens')
     ORDER BY 1, 2`,
};

async function runChecks(db) {
  const result = {};
  for (const [name, sql] of Object.entries(QUERIES)) {
    const { rows } = await db.query(sql);
    result[name] = rows.length === 1 && 'n' in rows[0] ? rows[0].n : rows;
  }
  return result;
}

function printChecks(c) {
  console.log(`Salas: ${c.salas}`);
  console.log(`Notas: ${c.notas}`);
  console.log(`Notas huérfanas (debe ser 0): ${c.notasHuerfanas}`);
  console.log(`Notas de quien no figura como jugador (debe ser 0): ${c.notasSinJugador}`);
  console.log('Jugadas y notas por usuario:');
  console.table(c.jugadoresPorUsuario);
  console.log(`Categorías distintas: ${c.categorias.length}`);
  console.table(c.categorias);
  console.log(`Ciudades distintas: ${c.ciudades.length}`);
  console.table(c.ciudades);
  console.log(`Salas sin jugadores: ${c.salasSinJugadores.length}`);
  if (c.salasSinJugadores.length) console.table(c.salasSinJugadores);
  console.log('Salas con jugadores fijados a mano:');
  console.table(c.salasEspeciales);
  console.log('Claves ajenas:');
  console.table(c.clavesAjenas);
}

module.exports = { CATEGORIES, runChecks, printChecks };
