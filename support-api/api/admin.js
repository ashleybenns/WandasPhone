const HTML = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Support & suggestions – Admin</title>
  <style>
    * { box-sizing: border-box; }
    body { font-family: system-ui, sans-serif; margin: 0; background: #f5f5f5; height: 100vh; }
    .auth { background: #fff; padding: 12px 20px; border-bottom: 1px solid #ddd; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
    .auth input { padding: 8px 12px; width: 220px; border: 1px solid #ccc; border-radius: 4px; }
    .auth button { padding: 8px 16px; background: #0d47a1; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
    .auth button:hover { background: #1565c0; }
    .error { color: #c62828; font-size: 14px; }
    .layout { display: flex; height: calc(100vh - 52px); }
    .sidebar { width: 360px; border-right: 1px solid #ddd; background: #fff; overflow: auto; display: flex; flex-direction: column; }
    .sidebar h2 { margin: 0; padding: 16px; font-size: 18px; border-bottom: 1px solid #eee; }
    .thread-list { flex: 1; overflow: auto; }
    .thread-item { padding: 14px 16px; border-bottom: 1px solid #eee; cursor: pointer; transition: background 0.15s; }
    .thread-item:hover { background: #f5f5f5; }
    .thread-item.selected { background: #e3f2fd; }
    .thread-item .cat { font-weight: 600; color: #1565c0; font-size: 13px; }
    .thread-item .preview { font-size: 14px; color: #333; margin: 4px 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .thread-item .meta { font-size: 12px; color: #666; display: flex; justify-content: space-between; align-items: center; }
    .status-badge { font-size: 11px; padding: 2px 6px; border-radius: 4px; color: #fff; }
    .status-OPEN { background: #4CAF50; }
    .status-IN_PROGRESS { background: #FF9800; }
    .status-RESOLVED { background: #2196F3; }
    .status-CLOSED { background: #9E9E9E; }
    .main { flex: 1; display: flex; flex-direction: column; overflow: hidden; background: #fff; }
    .main-empty { flex: 1; display: flex; align-items: center; justify-content: center; color: #888; font-size: 16px; }
    .conv-header { padding: 16px 20px; border-bottom: 1px solid #ddd; display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 12px; }
    .conv-header h3 { margin: 0; font-size: 16px; }
    .conv-header .status-select { padding: 6px 10px; border-radius: 4px; border: 1px solid #ccc; font-weight: bold; }
    .conv-body { flex: 1; overflow: auto; padding: 20px; }
    .bubble { max-width: 85%; margin-bottom: 12px; padding: 12px 16px; border-radius: 12px; }
    .bubble.admin { background: #e3f2fd; align-self: flex-start; border-bottom-left-radius: 4px; }
    .bubble.user { background: #f5f5f5; margin-left: auto; border-bottom-right-radius: 4px; }
    .bubble .label { font-size: 11px; font-weight: bold; color: #666; margin-bottom: 4px; }
    .bubble .text { white-space: pre-wrap; word-break: break-word; }
    .bubble .time { font-size: 11px; color: #888; margin-top: 4px; }
    .original { background: #fafafa; padding: 16px; border-radius: 12px; margin-bottom: 16px; border: 1px solid #eee; }
    .conv-footer { padding: 16px 20px; border-top: 1px solid #ddd; display: flex; gap: 12px; align-items: flex-end; }
    .conv-footer textarea { flex: 1; min-height: 56px; padding: 12px; border: 1px solid #ccc; border-radius: 8px; font-family: inherit; resize: none; }
    .conv-footer button { padding: 12px 24px; background: #2196F3; color: #fff; border: none; border-radius: 8px; cursor: pointer; font-weight: bold; }
    .conv-footer button:hover { background: #1976D2; }
    .conv-footer button:disabled { background: #ccc; cursor: not-allowed; }
    .announce-section { border-top: 1px solid #ddd; padding: 16px 20px; background: #fafafa; }
    .announce-section h4 { margin: 0 0 8px 0; font-size: 14px; }
    .announce-list { list-style: none; padding: 0; margin: 0 0 12px 0; font-size: 13px; }
    .announce-list li { padding: 6px 0; border-bottom: 1px solid #eee; white-space: pre-wrap; word-break: break-word; }
    .announce-form { display: flex; gap: 8px; margin-top: 8px; }
    .announce-form textarea { flex: 1; min-height: 60px; padding: 8px; border: 1px solid #ccc; border-radius: 4px; font-family: inherit; resize: vertical; }
    .announce-form button { padding: 8px 16px; background: #0d47a1; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
    .category-emoji { font-size: 18px; }
  </style>
</head>
<body>
  <div class="auth">
    <input type="password" id="key" placeholder="Admin key" autocomplete="off" />
    <button type="button" id="loadThreads">Load threads</button>
    <div id="authError" class="error"></div>
  </div>
  <div class="layout">
    <div class="sidebar">
      <h2>📬 Threads</h2>
      <div class="thread-list" id="threadList"></div>
    </div>
    <div class="main" id="main">
      <div class="main-empty" id="mainEmpty">Select a thread</div>
      <div id="mainConv" style="display: none; flex: 1; flex-direction: column; overflow: hidden;">
        <div class="conv-header" id="convHeader"></div>
        <div class="conv-body" id="convBody"></div>
        <div class="conv-footer">
          <textarea id="replyText" placeholder="Write a reply..."></textarea>
          <button type="button" id="sendReply">Send</button>
        </div>
      </div>
    </div>
  </div>
  <div class="announce-section" id="announceSection">
    <h4>Messages to all (announcements)</h4>
    <ul class="announce-list" id="announceList"></ul>
    <div class="announce-form">
      <textarea id="announceBody" placeholder="New announcement..."></textarea>
      <button type="button" id="postAnnounce">Post</button>
    </div>
    <span id="announceError" class="error"></span>
  </div>
  <script>
    function getKey() { return document.getElementById('key').value.trim(); }
    function formatDate(ms) { if (!ms) return '-'; return new Date(ms).toLocaleString(); }
    var threads = [];
    var selectedId = null;

    var categoryEmoji = { support: '🆘', feature_suggestion: '💡' };
    var categoryName = { support: 'Support', feature_suggestion: 'Feature suggestion' };

    document.getElementById('loadThreads').onclick = async function() {
      var key = getKey();
      var authError = document.getElementById('authError');
      var listEl = document.getElementById('threadList');
      authError.textContent = '';
      listEl.innerHTML = '';
      if (!key) { authError.textContent = 'Enter the admin key.'; return; }
      try {
        var res = await fetch('/api/admin/threads?key=' + encodeURIComponent(key));
        var data = await res.json();
        if (!res.ok) { authError.textContent = data.error || 'Failed to load.'; return; }
        threads = data.threads || [];
        if (threads.length === 0) {
          listEl.innerHTML = '<div style="padding:24px;text-align:center;color:#666">No threads yet.</div>';
          return;
        }
        listEl.innerHTML = threads.map(function(t) {
          var cat = (t.category || 'support').toLowerCase();
          var emoji = categoryEmoji[cat] || '💬';
          var name = categoryName[cat] || t.category;
          var preview = (t.body || '').replace(/</g, '&lt;').replace(/>/g, '&gt;').slice(0, 60);
          if ((t.body || '').length > 60) preview += '…';
          var status = (t.status || 'OPEN');
          return '<div class="thread-item" data-id="' + (t.id || '').replace(/"/g, '&quot;') + '">' +
            '<div class="cat">' + emoji + ' ' + name + '</div>' +
            '<div class="preview">' + preview + '</div>' +
            '<div class="meta"><span>' + formatDate(t.updatedAt || t.createdAt) + '</span><span class="status-badge status-' + status + '">' + status + '</span></div>' +
            (t.replies && t.replies.length ? '<div class="meta">💬 ' + t.replies.length + ' replies</div>' : '') +
            '</div>';
        }).join('');
        listEl.querySelectorAll('.thread-item').forEach(function(el) {
          el.onclick = function() { selectThread(el.getAttribute('data-id')); };
        });
      } catch (e) { authError.textContent = 'Network error: ' + e.message; }
    };

    function selectThread(id) {
      selectedId = id;
      document.querySelectorAll('.thread-item').forEach(function(el) {
        el.classList.toggle('selected', el.getAttribute('data-id') === id);
      });
      var t = threads.find(function(x) { return x.id === id; });
      if (!t) return;
      document.getElementById('mainEmpty').style.display = 'none';
      var conv = document.getElementById('mainConv');
      conv.style.display = 'flex';
      var cat = (t.category || 'support').toLowerCase();
      document.getElementById('convHeader').innerHTML =
        '<div><h3>' + (categoryEmoji[cat] || '💬') + ' ' + (categoryName[cat] || t.category) + '</h3>' +
        '<span style="font-size:12px;color:#666">' + formatDate(t.createdAt) + ' • Device: ' + (t.deviceId || '').slice(0, 8) + '…</span></div>' +
        '<select class="status-select" id="statusSelect">' +
        ['OPEN','IN_PROGRESS','RESOLVED','CLOSED'].map(function(s) {
          return '<option value="' + s + '"' + (t.status === s ? ' selected' : '') + '>' + s + '</option>';
        }).join('') + '</select>';
      document.getElementById('statusSelect').onchange = function() {
        updateStatus(selectedId, this.value);
      };
      var body = document.getElementById('convBody');
      body.innerHTML =
        '<div class="original"><div class="text">' + (t.body || '').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\\n/g, '<br>') + '</div></div>' +
        (t.replies || []).map(function(r) {
          var side = r.isAdmin ? 'admin' : 'user';
          var label = r.isAdmin ? 'Support' : 'User';
          return '<div class="bubble ' + side + '"><div class="label">' + label + '</div><div class="text">' + (r.message || '').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\\n/g, '<br>') + '</div><div class="time">' + formatDate(r.timestamp) + '</div></div>';
        }).join('');
      body.scrollTop = body.scrollHeight;
      document.getElementById('replyText').value = '';
    }

    function updateStatus(id, status) {
      var key = getKey();
      if (!key) return;
      fetch('/api/admin/threads/' + encodeURIComponent(id) + '/status?key=' + encodeURIComponent(key), {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: status })
      }).then(function(r) { return r.json(); }).then(function(d) {
        if (d.ok) {
          var t = threads.find(function(x) { return x.id === id; });
          if (t) t.status = status;
        }
      });
    }

    document.getElementById('sendReply').onclick = async function() {
      if (!selectedId) return;
      var key = getKey();
      var text = document.getElementById('replyText').value.trim();
      if (!text || !key) return;
      var btn = document.getElementById('sendReply');
      btn.disabled = true;
      try {
        var r = await fetch('/api/admin/threads/' + encodeURIComponent(selectedId) + '/reply?key=' + encodeURIComponent(key), {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ message: text })
        });
        var d = await r.json();
        if (r.ok && d.ok) {
          document.getElementById('replyText').value = '';
          document.getElementById('loadThreads').click();
          setTimeout(function() { selectThread(selectedId); }, 100);
        } else {
          document.getElementById('authError').textContent = d.error || 'Send failed.';
        }
      } catch (e) { document.getElementById('authError').textContent = e.message; }
      btn.disabled = false;
    };

    document.getElementById('loadAnnounce') && (document.getElementById('loadAnnounce').onclick = loadAnnounce);
    function loadAnnounce() {
      fetch('/api/announcements').then(function(r) { return r.json(); }).then(function(d) {
        var list = d.announcements || [];
        document.getElementById('announceList').innerHTML = list.length === 0
          ? '<li>None yet.</li>'
          : list.map(function(a) { return '<li><span style="color:#666">' + formatDate(a.createdAt) + '</span><br>' + (a.body || '').replace(/</g, '&lt;').replace(/>/g, '&gt;') + '</li>'; }).join('');
      });
    }
    loadAnnounce();

    document.getElementById('postAnnounce').onclick = async function() {
      var key = getKey();
      var body = document.getElementById('announceBody').value.trim();
      var errEl = document.getElementById('announceError');
      errEl.textContent = '';
      if (!body) { errEl.textContent = 'Enter text.'; return; }
      if (!key) { errEl.textContent = 'Enter admin key.'; return; }
      try {
        var r = await fetch('/api/admin/announce?key=' + encodeURIComponent(key), {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ body: body })
        });
        var d = await r.json();
        if (r.ok && d.ok) { document.getElementById('announceBody').value = ''; loadAnnounce(); }
        else { errEl.textContent = d.error || 'Failed.'; }
      } catch (e) { errEl.textContent = e.message; }
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
