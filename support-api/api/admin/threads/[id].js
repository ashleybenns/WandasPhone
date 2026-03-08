import {
  getRedis,
  THREAD_PREFIX,
  THREAD_REPLIES_PREFIX,
} from '../../../lib/redis.js';

const ADMIN_SECRET = process.env.SUPPORT_ADMIN_SECRET;

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
  const key = req.query.key || req.headers['x-admin-key'] || '';
  if (!ADMIN_SECRET || key !== ADMIN_SECRET) {
    res.setHeader('Content-Type', 'application/json');
    res.status(401).json({ error: 'Unauthorized' });
    return;
  }
  const id = req.query.id;
  if (!id) {
    res.setHeader('Content-Type', 'application/json');
    res.status(400).json({ error: 'id required' });
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
    const replyRaw = await client.lRange(THREAD_REPLIES_PREFIX + id, 0, -1);
    thread.replies = (replyRaw || []).map((s) => {
      try {
        return JSON.parse(s);
      } catch {
        return null;
      }
    }).filter(Boolean).reverse();
    res.setHeader('Content-Type', 'application/json');
    res.status(200).json(thread);
  } catch (e) {
    console.error('admin thread get error', e);
    res.setHeader('Content-Type', 'application/json');
    res.status(500).json({ error: 'Server error' });
  } finally {
    if (client) await client.quit().catch(() => {});
  }
}
