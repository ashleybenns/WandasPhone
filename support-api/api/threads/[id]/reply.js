import {
  getRedis,
  THREAD_PREFIX,
  THREAD_REPLIES_PREFIX,
} from '../../../lib/redis.js';

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
  const id = req.query.id;
  const { deviceId, message } = parseBody(req);
  if (!id || !deviceId || typeof message !== 'string' || !message.trim()) {
    res.setHeader('Content-Type', 'application/json');
    res.status(400).json({ error: 'id, deviceId and message required' });
    return;
  }
  let client;
  try {
    client = await getRedis();
    const raw = await client.get(THREAD_PREFIX + id);
    if (!raw) {
      res.setHeader('Content-Type', 'application/json');
      res.status(404).json({ error: 'Thread not found' });
      return;
    }
    let thread;
    try {
      thread = JSON.parse(raw);
    } catch {
      res.setHeader('Content-Type', 'application/json');
      res.status(404).json({ error: 'Thread not found' });
      return;
    }
    if (thread.deviceId !== deviceId) {
      res.setHeader('Content-Type', 'application/json');
      res.status(403).json({ error: 'Forbidden' });
      return;
    }
    const reply = {
      id: Date.now().toString(36) + Math.random().toString(36).slice(2),
      message: String(message).trim().slice(0, 4000),
      timestamp: Date.now(),
      isAdmin: false,
    };
    await client.lPush(THREAD_REPLIES_PREFIX + id, JSON.stringify(reply));
    thread.updatedAt = Date.now();
    await client.set(THREAD_PREFIX + id, JSON.stringify(thread));
    res.setHeader('Content-Type', 'application/json');
    res.status(200).json({ ok: true, reply });
  } catch (e) {
    console.error('thread reply error', e);
    res.setHeader('Content-Type', 'application/json');
    res.status(500).json({ error: 'Server error' });
  } finally {
    if (client) await client.quit().catch(() => {});
  }
}
