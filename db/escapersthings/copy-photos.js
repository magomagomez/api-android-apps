// Copia los carteles y las fotos de grupo de la web antigua (magomagomez.com) a escapers-api/v2.
// Se puede repetir: salta las salas que ya tienen esa foto en la API.
//
// Requiere Node 22 (fetch nativo), sin dependencias:
//   ESCAPERS_USER=... ESCAPERS_PASSWORD=... [ESCAPERS_API=https://api-android-app.herokuapp.com] \
//     node db/escapersthings/copy-photos.js
// ESCAPERS_OLD_IMAGES cambia el origen de las imágenes (solo para probar el script en local).

const API = (process.env.ESCAPERS_API || 'https://api-android-app.herokuapp.com').replace(/\/$/, '') + '/escapers-api/v2';
const OLD_IMAGES = process.env.ESCAPERS_OLD_IMAGES || 'https://magomagomez.com/escapersthings/web/images';

// kind de la API -> campo del listado y carpeta de la web antigua.
const KINDS = [
  { kind: 'poster', field: 'poster', folder: '/escapes' },
  { kind: 'group', field: 'groupPhoto', folder: '/done' },
];

// Tipo real por la cabecera del fichero: muchas .png de la web antigua son JPEG.
function imageType(bytes) {
  if (bytes.length >= 3 && bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff) return 'image/jpeg';
  if (bytes.length >= 8 && bytes.subarray(0, 8).equals(Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]))) return 'image/png';
  if (bytes.length >= 12 && bytes.toString('latin1', 0, 4) === 'RIFF' && bytes.toString('latin1', 8, 12) === 'WEBP') return 'image/webp';
  return null;
}

async function api(method, route, { token, json, body, contentType } = {}) {
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  if (json !== undefined) headers['Content-Type'] = 'application/json';
  if (contentType) headers['Content-Type'] = contentType;
  const res = await fetch(API + route, { method, headers, body: json !== undefined ? JSON.stringify(json) : body });
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(`${method} ${route} -> ${res.status} ${text.slice(0, 200)}`);
  }
  return res;
}

async function copyOne(token, room, { kind, folder }) {
  const url = `${OLD_IMAGES}${folder}/${room.id}.png`;
  const res = await fetch(url);
  if (res.status === 404) return 'missing';
  if (!res.ok) throw new Error(`GET ${url} -> ${res.status}`);
  const bytes = Buffer.from(await res.arrayBuffer());
  const type = imageType(bytes);
  if (!type) return 'notImage';
  await api('PUT', `/escape-rooms/${room.id}/photos/${kind}`, { token, body: bytes, contentType: type });
  return 'uploaded';
}

async function main() {
  const username = process.env.ESCAPERS_USER;
  const password = process.env.ESCAPERS_PASSWORD;
  if (!username || !password) throw new Error('Faltan ESCAPERS_USER y ESCAPERS_PASSWORD');

  const { token } = await (await api('POST', '/auth/login', { json: { username, password } })).json();
  const summary = { uploaded: [], already: [], missing: [], errors: [] };
  const LOG = { uploaded: 'subida', missing: 'no existe', notImage: 'no es imagen' };
  try {
    const rooms = await (await api('GET', '/escape-rooms', { token })).json();
    for (const room of rooms) {
      for (const k of KINDS) {
        const label = `${room.id} ${room.name} (${k.kind})`;
        if (room[k.field]) { summary.already.push(label); continue; }
        try {
          const result = await copyOne(token, room, k);
          console.log(`${LOG[result]}: ${label}`);
          if (result === 'uploaded') summary.uploaded.push(label);
          else summary.missing.push(result === 'notImage' ? `${label}, no es imagen` : label);
        } catch (e) {
          summary.errors.push(`${label}: ${e.message}`);
          console.error(`error: ${label}: ${e.message}`);
        }
      }
    }
  } finally {
    await api('POST', '/auth/logout', { token }).catch(() => {});
  }

  console.log(`\nSubidas: ${summary.uploaded.length}`);
  console.log(`Ya estaban: ${summary.already.length}`);
  console.log(`No existen en la web antigua: ${summary.missing.length}`);
  summary.missing.forEach((m) => console.log(`  - ${m}`));
  console.log(`Errores: ${summary.errors.length}`);
  summary.errors.forEach((m) => console.log(`  - ${m}`));
  if (summary.errors.length) process.exitCode = 1;
}

main().catch((e) => { console.error(`Error: ${e.message}`); process.exit(1); });
