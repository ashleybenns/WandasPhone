import { getRedis, THREAD_PREFIX } from '../../../../lib/redis.js';

const ADMIN_SECRET = process.env.SUPPORT_ADMIN_SECRET;
const VALID_STATUSES = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];

function setCors(res, extra = {}) {
  const headers = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'PATCH, POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type',
    ...extra,
  };
  Object.entries(headers).forEach(([k, v]) => res.setHeader(k, v));
}

function parseBody(req) {
  const raw = req.body;
  if (raw == null) return {};
  if (typeof raw === 'object' && !Buffer.isBuffer(raw)) return raw;
  try {
    return JSON.parse(typeof raw === 'string' ? raw : String(raw));
  } catch {
    return {};
  }
}

export default async function handler(req, res) {
  setCors(res);
  if (req.method === 'OPTIONS') {
    res.status(204).end();
    return;
  }
  if (req.method !== 'PATCH' && req.method !== 'POST') {
    res.setHeader('Content-Type', 'application/json');
    res.status(405).json({ error: 'Method not allowed' });
    return;
  }
  const key = req.query.key || req.headers['x-admin-key'] || '';
  if (!ADMIN_SECRET || key !== ADMIN_SECRET) {
    res.setHeader('Content-Type', 'application/json');
    res.status(401).json({ error: 'Unauthorized' });
    return;
  }
  const id = req.query.id;
  const { status } = parseBody(req);
  if (!id || !status || !VALID_STATUSES.includes(String(status).toUpperCase())) {
    res.setHeader('Content-Type', 'application/json');
    res.status(400).json({ error: 'id and status required (OPEN, IN_PROGRESS, RESOLVED, CLOSED)' });
    return;
  }
  const newStatus = String(status).toUpperCase();
  let client;
  try {
    client = await getRedis();
    const raw = await client.get(THREAD_PREFIX + id);
    if (!raw) {
      res.setHeader('Content-Type', 'application/json');
      res.status(404).json({ error: 'Thread not found' });
      return;
    }
    const thread = JSON.parse(raw);
    thread.status = newStatus;
    await client.set(THREAD_PREFIX + id, JSON.stringify(thread));
    res.setHeader('Content-Type', 'application/json');
    res.status(200).json({ ok: true, status: newStatus });
  } catch (e) {
    console.error('admin thread status error', e);
    res.setHeader('Content-Type', 'application/json');
    res.status(500).json({ error: 'Server error' });
  } finally {
    if (client) await client.quit().catch(() => {});
  }
}
