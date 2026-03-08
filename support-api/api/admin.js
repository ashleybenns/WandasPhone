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
    .reply-cell { max-width: 320px; }
    .reply-existing { background: #e8f5e9; padding: 8px; border-radius: 4px; margin-bottom: 6px; font-size: 0.9em; white-space: pre-wrap; word-break: break-word; }
    .reply-form textarea { width: 100%; min-height: 60px; padding: 8px; border: 1px solid #ccc; border-radius: 4px; font-family: inherit; resize: vertical; }
    .reply-form button { margin-top: 4px; padding: 6px 12px; background: #2e7d32; color: #fff; border: none; border-radius: 4px; cursor: pointer; font-size: 0.9em; }
    .reply-form button:hover { background: #1b5e20; }
    .section { background: #fff; padding: 20px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
    .section h2 { margin-top: 0; }
    .announce-list { list-style: none; padding: 0; margin: 0 0 16px 0; }
    .announce-list li { padding: 10px; border-bottom: 1px solid #eee; white-space: pre-wrap; word-break: break-word; }
    .announce-form textarea { width: 100%; min-height: 80px; padding: 8px; border: 1px solid #ccc; border-radius: 4px; font-family: inherit; resize: vertical; }
    .announce-form button { margin-top: 8px; padding: 8px 16px; background: #0d47a1; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
    .announce-form button:hover { background: #1565c0; }
  </style>
</head>
<body>
  <h1>Support & suggestions</h1>
  <div class="auth">
    <input type="password" id="key" placeholder="Admin key" autocomplete="off" />
    <button type="button" id="load">Load messages</button>
    <button type="button" id="loadAnnounce">Load announcements</button>
    <div id="authError" class="error"></div>
  </div>
  <div id="list"></div>
  <div class="section" id="announceSection">
    <h2>Messages to all (announcements)</h2>
    <p>These appear in the app for all carers. Use for updates, new features, etc.</p>
    <ul class="announce-list" id="announceList"></ul>
    <div class="announce-form">
      <textarea id="announceBody" placeholder="New announcement..."></textarea>
      <br><button type="button" id="postAnnounce">Post announcement</button>
      <span id="announceError" class="error"></span>
    </div>
  </div>
  <script>
    function getKey() { return document.getElementById('key').value.trim(); }
    function formatDate(ms) {
      if (!ms) return '-';
      return new Date(ms).toLocaleString();
    }

    document.getElementById('load').onclick = async function() {
      const key = getKey();
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
        let table = '<table><thead><tr><th>Date</th><th>Category</th><th>Message</th><th>Reply</th></tr></thead><tbody>';
        posts.forEach(function(p) {
          const replyHtml = p.reply
            ? '<div class="reply-existing">' + (p.reply.reply || '').replace(/</g, '&lt;').replace(/>/g, '&gt;') + '</div>'
            : '';
          table += '<tr data-id="' + (p.id || '').replace(/"/g, '&quot;') + '"><td class="date">' + formatDate(p.createdAt) + '</td><td class="category">' + (p.category || '-') + '</td><td class="body">' + (p.body || '').replace(/</g, '&lt;').replace(/>/g, '&gt;') + '</td><td class="reply-cell">' + replyHtml + '<div class="reply-form"><textarea data-id="' + (p.id || '').replace(/"/g, '&quot;') + '" placeholder="Reply to this message..."></textarea><br><button type="button" class="reply-btn">Send reply</button></div></td></tr>';
        });
        table += '</tbody></table>';
        listEl.innerHTML = table;
        listEl.querySelectorAll('.reply-btn').forEach(function(btn) {
          btn.onclick = async function() {
            const row = btn.closest('tr');
            const messageId = row.getAttribute('data-id');
            const textarea = row.querySelector('.reply-form textarea');
            const reply = (textarea && textarea.value || '').trim();
            if (!reply) return;
            const key = getKey();
            if (!key) { authError.textContent = 'Enter the admin key.'; return; }
            try {
              const r = await fetch('/api/admin/reply?key=' + encodeURIComponent(key), {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ messageId: messageId, reply: reply })
              });
              const d = await r.json();
              if (r.ok && d.ok) {
                textarea.value = '';
                document.getElementById('load').click();
              } else {
                authError.textContent = d.error || 'Reply failed.';
              }
            } catch (e) {
              authError.textContent = 'Network error: ' + e.message;
            }
          };
        });
      } catch (e) {
        authError.textContent = 'Network error: ' + e.message;
      }
    };

    document.getElementById('loadAnnounce').onclick = async function() {
      const listEl = document.getElementById('announceList');
      try {
        const res = await fetch('/api/announcements');
        const data = await res.json();
        const list = data.announcements || [];
        if (list.length === 0) {
          listEl.innerHTML = '<li>No announcements yet.</li>';
          return;
        }
        listEl.innerHTML = list.map(function(a) {
          return '<li><span class="date">' + formatDate(a.createdAt) + '</span><br>' + (a.body || '').replace(/</g, '&lt;').replace(/>/g, '&gt;') + '</li>';
        }).join('');
      } catch (e) {
        listEl.innerHTML = '<li>Failed to load.</li>';
      }
    };

    document.getElementById('postAnnounce').onclick = async function() {
      const key = getKey();
      const bodyEl = document.getElementById('announceBody');
      const errEl = document.getElementById('announceError');
      const body = (bodyEl && bodyEl.value || '').trim();
      errEl.textContent = '';
      if (!body) { errEl.textContent = 'Enter announcement text.'; return; }
      if (!key) { errEl.textContent = 'Enter the admin key.'; return; }
      try {
        const r = await fetch('/api/admin/announce?key=' + encodeURIComponent(key), {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ body: body })
        });
        const d = await r.json();
        if (r.ok && d.ok) {
          bodyEl.value = '';
          document.getElementById('loadAnnounce').click();
        } else {
          errEl.textContent = d.error || 'Post failed.';
        }
      } catch (e) {
        errEl.textContent = 'Network error: ' + e.message;
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
