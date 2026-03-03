# Support & suggestions API (PhoneApp26)

Anonymous support and feature-suggestion board backend. Deploy to Vercel and connect the app to the deployed URL.

## Deploy to Vercel

1. **Create a Vercel KV database**
   - In [Vercel Dashboard](https://vercel.com/dashboard) → your project → Storage → Create Database → KV (Redis).
   - This adds `KV_REST_API_URL` and `KV_REST_API_TOKEN` to your project.

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

All submissions are anonymous (no user identifiers stored).
