import { createClient } from 'redis';

const LIST_KEY = 'support_posts';
const REPLIES_LIST_KEY = 'support_replies_list';
const REPLY_KEY_PREFIX = 'support_reply:';
const ANNOUNCEMENTS_KEY = 'support_announcements';

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

export { LIST_KEY, REPLIES_LIST_KEY, REPLY_KEY_PREFIX, ANNOUNCEMENTS_KEY };
