/* SriramMart AI Assistant widget */
(function () {
  'use strict';
  var root = document.getElementById('chat');
  if (!root) return;
  var body = document.getElementById('chatBody'), input = document.getElementById('chatInput'), sendBtn = document.getElementById('chatSend');
  var fabs = Array.prototype.slice.call(document.querySelectorAll('.chat-fab'));
  var CTX = (window.SM && window.SM.ctx) || '';
  var name = root.getAttribute('data-name') || 'there';
  var avatar = root.getAttribute('data-avatar');
  var productId = root.getAttribute('data-product') || '';
  var KEY = 'sm_chat_v2';
  var msgs = [];
  try { msgs = JSON.parse(sessionStorage.getItem(KEY) || '[]'); } catch (e) { msgs = []; }
  var busy = false;

  var QUICK = [
    ['search', 'Find a product', 'Search for any item', 'local:find'],
    ['truck', 'Check order status', 'Track your delivery', 'Track my order'],
    ['percent', 'Best deals & offers', 'View latest discounts', 'Show me the best deals'],
    ['package', 'Product details', 'Know more about a product', 'local:details'],
    ['refresh', 'Return / Exchange', 'Get help with returns', 'What is the return policy?'],
    ['user', 'Account help', 'Login, profile & more', 'I need account help']
  ];

  function el(tag, cls, text) { var n = document.createElement(tag); if (cls) n.className = cls; if (text != null) n.textContent = text; return n; }
  function icon(id) {
    var ns = 'http://www.w3.org/2000/svg', s = document.createElementNS(ns, 'svg'), u = document.createElementNS(ns, 'use');
    s.setAttribute('class', 'icon'); u.setAttribute('href', '#i-' + id); s.appendChild(u); return s;
  }
  function now() { return new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }); }
  function save() { try { sessionStorage.setItem(KEY, JSON.stringify(msgs.slice(-30))); } catch (e) { /* ignore */ } }
  function scroll() { body.scrollTop = body.scrollHeight; }

  function botAvatar() { var i = el('img', 'av'); i.src = avatar; i.alt = ''; return i; }

  function renderMsg(m) {
    var row = el('div', 'msg ' + (m.role === 'user' ? 'user' : 'bot'));
    var wrap = el('div');
    var b = el('div', 'bubble', m.text); wrap.appendChild(b);
    b.appendChild(el('time', null, m.time || ''));
    if (m.cards && m.cards.length) {
      var list = el('div', 'pc-list');
      m.cards.forEach(function (c) {
        var a = el('a', 'pc'); a.href = CTX + c.url;
        var im = el('img'); im.src = CTX + c.image; im.alt = ''; a.appendChild(im);
        var d = el('div'); d.appendChild(el('b', null, c.name));
        var p = el('span', 'pr', c.price); d.appendChild(p);
        if (c.mrp) { d.appendChild(el('s', null, c.mrp)); }
        if (c.off) { d.appendChild(el('span', 'off', c.off + '% off')); }
        d.appendChild(el('div', 'small muted', c.stock));
        a.appendChild(d); list.appendChild(a);
      });
      wrap.appendChild(list);
    }
    if (m.sugg && m.sugg.length) {
      var s = el('div', 'sugg');
      m.sugg.forEach(function (q) { var bt = el('button', null, q); bt.type = 'button'; bt.addEventListener('click', function () { send(q); }); s.appendChild(bt); });
      wrap.appendChild(s);
    }
    if (m.role !== 'user') row.appendChild(botAvatar());
    row.appendChild(wrap);
    return row;
  }

  function renderAll() {
    body.textContent = '';
    var hello = el('div', 'msg bot'); hello.appendChild(botAvatar());
    var hb = el('div', 'bubble', 'Hello! 👋\nI\'m your SriramMart AI Assistant.\nI can help you find products, check offers, track orders, answer your questions and more. What would you like to do today?'
      + (productId ? '\n\nYou\'re viewing a product — ask me about its price, stock, warranty or specs.' : ''));
    hello.appendChild(hb); body.appendChild(hello);
    var q = el('div', 'quick');
    QUICK.forEach(function (x) {
      var b = el('button'); b.type = 'button'; b.appendChild(icon(x[0]));
      var d = el('div'); d.appendChild(el('b', null, x[1])); d.appendChild(el('small', null, x[2])); b.appendChild(d);
      b.addEventListener('click', function () { quick(x); }); q.appendChild(b);
    });
    body.appendChild(q);
    msgs.forEach(function (m) { body.appendChild(renderMsg(m)); });
    scroll();
  }

  function addBot(text, extra) {
    var m = { role: 'bot', text: text, time: now(), cards: extra && extra.cards, sugg: extra && extra.sugg };
    msgs.push(m); body.appendChild(renderMsg(m)); save(); scroll();
  }

  function quick(x) {
    var v = x[3];
    if (v === 'local:find') { msgs.push({ role: 'user', text: x[1], time: now() }); body.appendChild(renderMsg(msgs[msgs.length - 1])); addBot('Sure! Tell me what you\'re looking for — for example “laptop under ₹50,000”, “Samsung phone” or “running shoes”.', { sugg: ['Laptops under ₹50,000', 'Running shoes', 'Books under ₹400'] }); input.focus(); }
    else if (v === 'local:details') { msgs.push({ role: 'user', text: x[1], time: now() }); body.appendChild(renderMsg(msgs[msgs.length - 1])); addBot('Type the product name (for example “boAt Airdopes”) or open a product page and ask me about its price, stock, warranty or specifications.', { sugg: ['boAt Airdopes 141', 'Samsung Galaxy M35 5G'] }); input.focus(); }
    else send(v);
  }

  function typing() {
    var row = el('div', 'msg bot'); row.id = 'typing'; row.appendChild(botAvatar());
    var b = el('div', 'bubble'); var t = el('span', 'typing'); t.appendChild(el('i')); t.appendChild(el('i')); t.appendChild(el('i')); b.appendChild(t); row.appendChild(b);
    body.appendChild(row); scroll();
  }

  function send(text) {
    text = (text || '').trim();
    if (!text || busy) return;
    busy = true; input.value = '';
    var m = { role: 'user', text: text, time: now() };
    var hist = msgs.slice(-8).map(function (x) { return { role: x.role === 'user' ? 'user' : 'assistant', text: x.text }; });
    msgs.push(m); body.appendChild(renderMsg(m)); save(); typing();
    fetch(CTX + '/api/chat', {
      method: 'POST', credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': window.SM.csrf() },
      body: JSON.stringify({ message: text, productId: productId ? Number(productId) : null, history: hist })
    }).then(function (r) {
      if (r.status === 401 || r.status === 403 || r.redirected) { window.location.reload(); throw new Error('session'); }
      if (!r.ok) throw new Error('http ' + r.status);
      return r.json();
    }).then(function (d) {
      var t = document.getElementById('typing'); if (t) t.remove();
      addBot(d.reply, { cards: d.products, sugg: d.suggestions });
    }).catch(function (err) {
      var t = document.getElementById('typing'); if (t) t.remove();
      if (err.message !== 'session') addBot('Sorry, I couldn\'t reach the store right now. Please try again in a moment.');
    }).then(function () { busy = false; input.focus(); });
  }

  function open() {
    root.classList.remove('hidden'); fabs.forEach(function (f) { f.classList.add('hidden'); });
    try { sessionStorage.setItem(KEY + '_open', '1'); } catch (e) { /* ignore */ }
    input.focus(); scroll();
  }
  function close() {
    root.classList.add('hidden'); fabs.forEach(function (f) { f.classList.remove('hidden'); });
    try { sessionStorage.removeItem(KEY + '_open'); } catch (e) { /* ignore */ }
  }

  document.addEventListener('click', function (e) {
    if (e.target.closest('[data-chat-open]')) { open(); }
    else if (e.target.closest('[data-chat-close]')) { close(); }
    else if (e.target.closest('[data-chat-reset]')) { msgs = []; save(); renderAll(); }
  });
  sendBtn.addEventListener('click', function () { send(input.value); });
  input.addEventListener('keydown', function (e) { if (e.key === 'Enter') { e.preventDefault(); send(input.value); } });

  window.SMChat = { ask: function (t) { open(); send(t); }, open: open };
  renderAll();
  try { if (sessionStorage.getItem(KEY + '_open') === '1') open(); } catch (e) { /* ignore */ }
})();
