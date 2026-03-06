import { getRedis, LIST_KEY } from './redis.js';

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
    res.status(405).json({ error: 'Method not allowed' });
    return;
  }
  let client;
  try {
    const since = parseInt(req.query.since, 10) || 0;
    client = await getRedis();
    const list = await client.lRange(LIST_KEY, 0, -1);
    const count = (list || []).filter((s) => {
      try {
        const o = typeof s === 'string' ? JSON.parse(s) : s;
        return (o?.createdAt ?? 0) > since;
      } catch {
        return false;
      }
    }).length;
    res.setHeader('Content-Type', 'application/json');
    res.status(200).json({ count });
  } catch (e) {
    console.error('posts error', e);
    res.setHeader('Content-Type', 'application/json');
    res.status(200).json({ count: 0 });
  } finally {
    if (client) await client.quit().catch(() => {});
  }
}
