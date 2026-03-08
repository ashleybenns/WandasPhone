const HTML = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Support & suggestions – Admin</title>
  <style>
    * { box-sizing: border-box; }
    body { font-family: system-ui, sans-serif; max-width: 900px; margin: 0 auto; padding: 20px; background: #f5f5f5; }
    h1 { margin-top: 0; }
    .auth { background: #fff; padding: 20px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
    .auth input { padding: 8px 12px; width: 280px; margin-right: 8px; border: 1px solid #ccc; border-radius: 4px; }
    .auth button { padding: 8px 16px; background: #0d47a1; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
    .auth button:hover { background: #1565c0; }
    .error { color: #c62828; margin-top: 8px; }
    table { width: 100%; border-collapse: collapse; background: #fff; box-shadow: 0 1px 3px rgba(0,0,0,0.1); border-radius: 8px; overflow: hidden; }
    th, td { padding: 12px; text-align: left; border-bottom: 1px solid #eee; }
    th { background: #e3f2fd; font-weight: 600; }
    tr:hover { background: #fafafa; }
    .category { font-weight: 500; color: #1565c0; }
    .body { white-space: pre-wrap; word-break: break-word; max-width: 400px; }
    .date { color: #666; font-size: 0.9em; }
    .empty { padding: 24px; text-align: center; color: #666; background: #fff; border-radius: 8px; }
  </style>
</head>
<body>
  <h1>Support & suggestions</h1>
  <div class="auth">
    <input type="password" id="key" placeholder="Admin key" autocomplete="off" />
    <button type="button" id="load">Load messages</button>
    <div id="authError" class="error"></div>
  </div>
  <div id="list"></div>
  <script>
    document.getElementById('load').onclick = async function() {
      const key = document.getElementById('key').value.trim();
      const authError = document.getElementById('authError');
      const listEl = document.getElementById('list');
      authError.textContent = '';
      listEl.innerHTML = '';
      if (!key) { authError.textContent = 'Enter the admin key.'; return; }
      try {
        const res = await fetch('/api/admin/posts?key=' + encodeURIComponent(key));
        const data = await res.json();
        if (!res.ok) {
          authError.textContent = data.error || 'Failed to load (check key).';
          return;
        }
        const posts = data.posts || [];
        if (posts.length === 0) {
          listEl.innerHTML = '<div class="empty">No messages yet.</div>';
          return;
        }
        const formatDate = (ms) => {
          if (!ms) return '-';
          const d = new Date(ms);
          return d.toLocaleString();
        };
        let table = '<table><thead><tr><th>Date</th><th>Category</th><th>Message</th></tr></thead><tbody>';
        posts.forEach(function(p) {
          table += '<tr><td class="date">' + formatDate(p.createdAt) + '</td><td class="category">' + (p.category || '-') + '</td><td class="body">' + (p.body || '').replace(/</g, '&lt;').replace(/>/g, '&gt;') + '</td></tr>';
        });
        table += '</tbody></table>';
        listEl.innerHTML = table;
      } catch (e) {
        authError.textContent = 'Network error: ' + e.message;
      }
    };
  </script>
</body>
</html>`;

export default async function handler(req, res) {
  if (req.method !== 'GET') {
    res.setHeader('Content-Type', 'application/json');
    res.status(405).json({ error: 'Method not allowed' });
    return;
  }
  res.setHeader('Content-Type', 'text/html; charset=utf-8');
  res.status(200).send(HTML);
}
