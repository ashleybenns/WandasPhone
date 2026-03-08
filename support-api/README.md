# Support & suggestions API (PhoneApp26)

Anonymous, thread-based support and feature-suggestion messaging. Each user has their own threads (identified by anonymous `deviceId`). Deploy to Vercel and connect the app to the deployed URL.

## Deploy to Vercel

1. **Create a Redis store and connect to the project**
   - In [Vercel Dashboard](https://vercel.com/dashboard) → your project → **Storage** → Create Database → choose **Redis** (or KV/Upstash).
   - Connect the store to your project so it gets **REDIS_URL**. The API uses this.

2. **Deploy this folder**
   - From this directory: `npx vercel` (or link the repo and set Root Directory to `support-api`).
   - Note the deployment URL.

3. **Configure the Android app**
   - In the app module’s `build.gradle.kts`, set:
     - `buildConfigField("String", "SUPPORT_API_BASE_URL", "\"https://YOUR-DEPLOYMENT-URL\"")`
   - Rebuild the app.

## Endpoints

**App (anonymous, per-device threads)**

- **POST /api/post**  
  Body: `{ "deviceId": "<anonymous UUID>", "category": "support" | "feature_suggestion", "body": "…" }`  
  Creates a new thread. Returns `{ "ok": true, "threadId": "…" }`.

- **GET /api/threads?deviceId=**  
  Returns `{ "threads": [ { "id", "deviceId", "category", "body", "createdAt", "updatedAt", "status", "replies": [ { "id", "message", "timestamp", "isAdmin" }, … ] }, … ] }` — threads for this device only.

- **GET /api/threads/:id?deviceId=**  
  Returns one thread with replies (only if `deviceId` matches).

- **POST /api/threads/:id/reply**  
  Body: `{ "deviceId": "…", "message": "…" }`. Adds a user follow-up to the thread (only if `deviceId` matches).

- **GET /api/posts?since=&deviceId=**  
  Returns `{ "count": N }` — number of this device’s threads with `updatedAt` > `since` (for unread badge).

- **GET /api/announcements**  
  Returns `{ "announcements": [ { "id", "body", "createdAt" }, … ] }` — messages to all carers (updates, new features).

**Admin (require `?key=SUPPORT_ADMIN_SECRET` or header `X-Admin-Key`)**

- **GET /api/admin**  
  HTML admin UI: thread list, conversation view, reply, status (OPEN / IN_PROGRESS / RESOLVED / CLOSED), and announcements.

- **GET /api/admin/threads?key=**  
  Returns all threads with replies.

- **GET /api/admin/threads/:id?key=**  
  Returns one thread with replies.

- **POST /api/admin/threads/:id/reply?key=**  
  Body: `{ "message": "…" }`. Adds a support reply.

- **PATCH /api/admin/threads/:id/status?key=**  
  Body: `{ "status": "OPEN" | "IN_PROGRESS" | "RESOLVED" | "CLOSED" }`.

- **POST /api/admin/announce?key=**  
  Body: `{ "body": "…" }`. Posts an announcement.

**Admin secret:** Set **SUPPORT_ADMIN_SECRET** in Vercel. Use it on the admin page or in the `key` query / `X-Admin-Key` header.

All data is anonymous (only `deviceId` ties threads to a device; no personal information).
