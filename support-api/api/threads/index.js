import {
  getRedis,
  THREADS_LIST_KEY,
  THREAD_PREFIX,
  THREAD_REPLIES_PREFIX,
} from '../../lib/redis.js';

function setCors(res, extra = {}) {
  const headers = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type',
    ...extra,
  };
  Object.entries(headers).forEach(([k, v]) => res.setHeader(k, v));
}

export default async function handler(req, res) {
  setCors(res);
  if (req.method === 'OPTIONS') {
    res.status(204).end();
    return;
  }
  if (req.method !== 'GET') {
    res.setHeader('Content-Type', 'application/json');
    res.status(405).json({ error: 'Method not allowed' });
    return;
  }
  const deviceId = req.query.deviceId || '';
  if (!deviceId) {
    res.setHeader('Content-Type', 'application/json');
    res.status(400).json({ error: 'deviceId required' });
    return;
  }
  let client;
  try {
    client = await getRedis();
    const ids = await client.lRange(THREADS_LIST_KEY, 0, 499);
    const threads = [];
    for (const id of ids || []) {
      const raw = await client.get(THREAD_PREFIX + id);
      if (!raw) continue;
      let t;
      try {
        t = JSON.parse(raw);
      } catch {
        continue;
      }
      if (t.deviceId !== deviceId) continue;
      const replyRaw = await client.lRange(THREAD_REPLIES_PREFIX + id, 0, -1);
      t.replies = (replyRaw || []).map((s) => {
        try {
          return JSON.parse(s);
        } catch {
          return null;
        }
      }).filter(Boolean).reverse();
      threads.push(t);
    }
    threads.sort((a, b) => (b.updatedAt || b.createdAt || 0) - (a.updatedAt || a.createdAt || 0));
    res.setHeader('Content-Type', 'application/json');
    res.status(200).json({ threads });
  } catch (e) {
    console.error('threads list error', e);
    res.setHeader('Content-Type', 'application/json');
    res.status(500).json({ error: 'Server error' });
  } finally {
    if (client) await client.quit().catch(() => {});
  }
}
