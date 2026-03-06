import { getRedis, LIST_KEY } from './redis.js';

function setCors(res, extra = {}) {
  const headers = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
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
  if (req.method !== 'POST') {
    res.status(405).json({ error: 'Method not allowed' });
    return;
  }
  let client;
  try {
    const { category, body, context } = req.body || {};
    if (!category || typeof body !== 'string') {
      res.status(400).json({ error: 'category and body required' });
      return;
    }
    const item = {
      id: Date.now().toString(36) + Math.random().toString(36).slice(2),
      category: String(category).slice(0, 64),
      body: String(body).slice(0, 4000),
      context: context || null,
      createdAt: Date.now(),
    };
    client = await getRedis();
    await client.lPush(LIST_KEY, JSON.stringify(item));
    res.setHeader('Content-Type', 'application/json');
    res.status(200).json({ ok: true });
  } catch (e) {
    console.error('post error', e);
    res.status(500).json({ error: 'Server error' });
  } finally {
    if (client) await client.quit().catch(() => {});
  }
}
