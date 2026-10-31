/**
 * Correo de Regalos · fotos.
 *
 * Web app of Google Apps Script that keeps the photos of regalos-api in one Drive folder, with the
 * quota of the account that owns it. Deployed as "Execute as: me" and "Who has access: anyone":
 * regalos-api has no Google login, so every request carries the shared secret instead, kept in the
 * script property REGALOS_SECRET (Heroku: REGALOS_PHOTOS_SCRIPT_SECRET). It only ever touches files
 * inside FOLDER_ID, so the secret cannot reach anything else in this Drive.
 *
 * Protocol (POST, JSON body; the answer is always HTTP 200 with {"ok": true|false, ...}):
 *   {"secret", "action": "upload",   "name", "data": base64 JPEG} -> {"ok": true, "id"}
 *   {"secret", "action": "download", "fileId"}                     -> {"ok": true, "data": base64}
 *   {"secret", "action": "delete",   "fileId"}                     -> {"ok": true}
 * Errors: {"ok": false, "error": "forbidden" | "bad_request" | "not_found" | "failed"}.
 */

const FOLDER_ID = '1iBOhvnJ4ND89PNHCjQgx3BjFR8Lmyftm';
const MAX_BYTES = 6 * 1024 * 1024;
const FILE_ID = /^[A-Za-z0-9_-]{10,100}$/;

function doPost(e) {
  let request;
  try {
    request = JSON.parse(e.postData.contents);
  } catch (err) {
    return answer({ok: false, error: 'bad_request'});
  }
  const secret = PropertiesService.getScriptProperties().getProperty('REGALOS_SECRET');
  if (!secret || secret.length < 32 || !sameText(request.secret, secret)) {
    return answer({ok: false, error: 'forbidden'});
  }
  try {
    switch (request.action) {
      case 'upload':
        return upload(request);
      case 'download':
        return download(request.fileId);
      case 'delete':
        return remove(request.fileId);
      default:
        return answer({ok: false, error: 'bad_request'});
    }
  } catch (err) {
    console.error('regalos fotos: ' + request.action + ' failed: ' + err);
    return answer({ok: false, error: 'failed'});
  }
}

/** Nothing to see here without the secret. */
function doGet() {
  return answer({ok: false, error: 'forbidden'});
}

function upload(request) {
  const bytes = Utilities.base64Decode(String(request.data || ''));
  if (bytes.length === 0 || bytes.length > MAX_BYTES) {
    return answer({ok: false, error: 'bad_request'});
  }
  const name = String(request.name || '').replace(/[^A-Za-z0-9._-]/g, '').slice(0, 100) || 'regalos.jpg';
  const file = DriveApp.getFolderById(FOLDER_ID).createFile(Utilities.newBlob(bytes, 'image/jpeg', name));
  return answer({ok: true, id: file.getId()});
}

function download(fileId) {
  const file = fileInFolder(fileId);
  if (!file) {
    return answer({ok: false, error: 'not_found'});
  }
  return answer({ok: true, data: Utilities.base64Encode(file.getBlob().getBytes())});
}

/** To the bin: Drive empties it after 30 days, and a mistake can still be undone meanwhile. */
function remove(fileId) {
  const file = fileInFolder(fileId);
  if (!file) {
    return answer({ok: false, error: 'not_found'});
  }
  file.setTrashed(true);
  return answer({ok: true});
}

/** The file, only if it is a live file of the photo folder; null otherwise. */
function fileInFolder(fileId) {
  if (!FILE_ID.test(String(fileId))) {
    return null;
  }
  let file;
  try {
    file = DriveApp.getFileById(fileId);
  } catch (err) {
    return null;
  }
  if (file.isTrashed()) {
    return null;
  }
  const parents = file.getParents();
  while (parents.hasNext()) {
    if (parents.next().getId() === FOLDER_ID) {
      return file;
    }
  }
  return null;
}

/** Compares in constant time, so the answer time says nothing about the secret. */
function sameText(given, expected) {
  const a = String(given || '');
  if (a.length !== expected.length) {
    return false;
  }
  let diff = 0;
  for (let i = 0; i < a.length; i++) {
    diff |= a.charCodeAt(i) ^ expected.charCodeAt(i);
  }
  return diff === 0;
}

function answer(body) {
  return ContentService.createTextOutput(JSON.stringify(body)).setMimeType(ContentService.MimeType.JSON);
}

/**
 * Run once from the editor (▶ Ejecutar) to grant Drive access and check the setup. It writes a
 * test file into the folder and moves it to the bin again.
 */
function comprobar() {
  const folder = DriveApp.getFolderById(FOLDER_ID);
  const secret = PropertiesService.getScriptProperties().getProperty('REGALOS_SECRET');
  const probe = folder.createFile('regalos-prueba.txt', 'prueba');
  probe.setTrashed(true);
  console.log('Carpeta: ' + folder.getName() + ' · se puede escribir: sí · clave secreta: '
      + (secret && secret.length >= 32 ? 'puesta' : 'FALTA (propiedad REGALOS_SECRET)'));
}
