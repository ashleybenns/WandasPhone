import { createClient } from 'redis';

const LIST_KEY = 'support_posts';

/** Get a Redis client using REDIS_URL. Caller must call client.quit() when done. */
export async function getRedis() {
  const url = process.env.REDIS_URL;
  if (!url) {
    throw new Error('Missing required environment variable REDIS_URL');
  }
  const client = createClient({ url });
  await client.connect();
  return client;
}

export { LIST_KEY };
