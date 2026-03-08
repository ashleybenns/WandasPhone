# Support & suggestions API (PhoneApp26)

Anonymous support and feature-suggestion board backend. Deploy to Vercel and connect the app to the deployed URL.

## Deploy to Vercel

1. **Create a Redis store and connect to the project**
   - In [Vercel Dashboard](https://vercel.com/dashboard) → your project → **Storage** → Create Database → choose **Redis** (or KV/Upstash).
   - Connect the store to your project so it gets **REDIS_URL** (connection string). The API uses this; no KV_REST_* vars needed.

2. **Deploy this folder**
   - From this directory: `npx vercel` (or link the repo and set Root Directory to `support-api`).
   - Note the deployment URL (e.g. `https://phoneapp26-support-api-xxx.vercel.app`).

3. **Configure the Android app**
   - In the app module’s `build.gradle.kts`, set:
     - `buildConfigField("String", "SUPPORT_API_BASE_URL", "\"https://YOUR-DEPLOYMENT-URL\"")`
   - Rebuild the app so it uses the real API.

## Endpoints

- **POST /api/post**  
  Body: `{ "category": "support" | "feature_suggestion", "body": "…", "context": optional }`  
  Stores one anonymous post. No auth.

- **GET /api/posts?since=**  
  Query: `since` = Unix timestamp (ms).  
  Response: `{ "count": N }` — number of posts created after `since`. Used for the “unread” badge.

- **GET /api/admin**  
  Returns an HTML admin page. Enter the admin key to load and view all messages.

- **GET /api/admin/posts?key=**  
  Query: `key` = admin secret (or header `X-Admin-Key`).  
  Response: `{ "posts": [ { "id", "category", "body", "context", "createdAt", "reply"?: { "messageId", "reply", "repliedAt" } }, … ] }` — all stored messages (newest first). Each post may include a support reply. Returns 401 if the key is missing or wrong.

- **POST /api/admin/reply?key=**  
  Body: `{ "messageId": "<post id>", "reply": "…" }`. Admin only. Stores a reply for that message; the app fetches it via GET /api/replies.

- **GET /api/replies**  
  Response: `{ "replies": [ { "messageId", "reply", "repliedAt" }, … ] }` — recent support replies (no auth). Used by the app to show “Replies from support”.

- **POST /api/admin/announce?key=**  
  Body: `{ "body": "…" }`. Admin only. Adds an announcement (message to all carers). App fetches via GET /api/announcements.

- **GET /api/announcements**  
  Response: `{ "announcements": [ { "id", "body", "createdAt" }, … ] }` — recent announcements (no auth). Used for updates, new features, etc.

**Admin secret:** Set environment variable **SUPPORT_ADMIN_SECRET** in Vercel (e.g. a long random string). Use this key on the admin page or when calling admin endpoints (`/api/admin/posts`, `/api/admin/reply`, `/api/admin/announce`).

All submissions are anonymous (no user identifiers stored).
