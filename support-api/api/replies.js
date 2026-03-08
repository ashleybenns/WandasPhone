import { getRedis, REPLIES_LIST_KEY } from '../lib/redis.js';

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
  let client;
  try {
    client = await getRedis();
    const raw = await client.lRange(REPLIES_LIST_KEY, 0, 49);
    const replies = (raw || []).map((s) => {
      try {
        return typeof s === 'string' ? JSON.parse(s) : s;
      } catch {
        return null;
      }
    }).filter(Boolean);
    res.setHeader('Content-Type', 'application/json');
    res.status(200).json({ replies });
  } catch (e) {
    console.error('replies error', e);
    res.setHeader('Content-Type', 'application/json');
    res.status(200).json({ replies: [] });
  } finally {
    if (client) await client.quit().catch(() => {});
  }
}
