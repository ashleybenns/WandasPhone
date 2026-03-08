import { getRedis, ANNOUNCEMENTS_KEY } from '../../lib/redis.js';

const ADMIN_SECRET = process.env.SUPPORT_ADMIN_SECRET;

function setCors(res, extra = {}) {
  const headers = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'POST, OPTIONS',
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
  if (req.method !== 'POST') {
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
  const { body } = parseBody(req);
  if (typeof body !== 'string' || !body.trim()) {
    res.setHeader('Content-Type', 'application/json');
    res.status(400).json({ error: 'body required' });
    return;
  }
  let client;
  try {
    client = await getRedis();
    const item = {
      id: Date.now().toString(36) + Math.random().toString(36).slice(2),
      body: String(body).slice(0, 4000),
      createdAt: Date.now(),
    };
    await client.lPush(ANNOUNCEMENTS_KEY, JSON.stringify(item));
    res.setHeader('Content-Type', 'application/json');
    res.status(200).json({ ok: true, id: item.id });
  } catch (e) {
    console.error('admin/announce error', e);
    res.setHeader('Content-Type', 'application/json');
    res.status(500).json({ error: 'Server error' });
  } finally {
    if (client) await client.quit().catch(() => {});
  }
}
