import { getRedis, REPLIES_LIST_KEY, REPLY_KEY_PREFIX } from '../../lib/redis.js';

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
  const { messageId, reply } = parseBody(req);
  if (!messageId || typeof reply !== 'string' || !reply.trim()) {
    res.setHeader('Content-Type', 'application/json');
    res.status(400).json({ error: 'messageId and reply required' });
    return;
  }
  let client;
  try {
    client = await getRedis();
    const payload = {
      messageId: String(messageId),
      reply: String(reply).slice(0, 4000),
      repliedAt: Date.now(),
    };
    await client.set(REPLY_KEY_PREFIX + messageId, JSON.stringify(payload));
    await client.lPush(REPLIES_LIST_KEY, JSON.stringify(payload));
    res.setHeader('Content-Type', 'application/json');
    res.status(200).json({ ok: true });
  } catch (e) {
    console.error('admin/reply error', e);
    res.setHeader('Content-Type', 'application/json');
    res.status(500).json({ error: 'Server error' });
  } finally {
    if (client) await client.quit().catch(() => {});
  }
}
