/* SriramMart – page behaviour (no inline scripts, CSP-safe) */
(function () {
  'use strict';
  var meta = function (n) { var m = document.querySelector('meta[name="' + n + '"]'); return m ? m.content : ''; };
  var CTX = (meta('_ctx') || '/').replace(/\/$/, '');
  var $ = function (s, r) { return (r || document).querySelector(s); };
  var $$ = function (s, r) { return Array.prototype.slice.call((r || document).querySelectorAll(s)); };
  window.SM = { ctx: CTX, csrf: function () { return meta('_csrf'); } };

  function toast(msg, isErr) {
    var old = $('.toast'); if (old) old.remove();
    var t = document.createElement('div');
    t.className = 'toast' + (isErr ? ' err' : ''); t.setAttribute('role', 'status'); t.textContent = msg;
    document.body.appendChild(t);
    setTimeout(function () { t.remove(); }, 2600);
  }
  window.SM.toast = toast;

  function post(url, data) {
    var body = new URLSearchParams(data);
    return fetch(CTX + url, {
      method: 'POST', credentials: 'same-origin',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-CSRF-TOKEN': meta('_csrf'), 'Accept': 'application/json' },
      body: body.toString()
    }).then(function (r) {
      if (r.redirected || r.status === 401 || r.status === 403) { window.location.reload(); throw new Error('session'); }
      return r.json();
    });
  }

  function setCartCount(n) {
    $$('[data-cart-count]').forEach(function (b) { b.textContent = n; b.classList.toggle('hidden', !(n > 0)); });
  }

  document.addEventListener('click', function (e) {
    var t = e.target;

    // ---- add to cart
    var add = t.closest('[data-add-cart]');
    if (add) {
      e.preventDefault();
      if (add.disabled) return;
      var qty = 1, sel = add.getAttribute('data-qty-input');
      if (sel) { var q = $(sel); if (q) qty = parseInt(q.value, 10) || 1; }
      add.disabled = true;
      post('/api/cart/add', { productId: add.getAttribute('data-id'), qty: qty }).then(function (r) {
        toast(r.message, !r.ok);
        if (typeof r.count === 'number') setCartCount(r.count);
      }).catch(function (err) { if (err.message !== 'session') toast('Could not add to cart. Please try again.', true); })
        .then(function () { add.disabled = false; });
      return;
    }

    // ---- wishlist heart
    var heart = t.closest('[data-wish]');
    if (heart) {
      e.preventDefault();
      post('/api/wishlist/toggle', { productId: heart.getAttribute('data-id') }).then(function (r) {
        if (!r.ok) { toast(r.message, true); return; }
        $$('[data-wish][data-id="' + heart.getAttribute('data-id') + '"]').forEach(function (h) { h.classList.toggle('on', r.wished); });
        toast(r.message);
        if (!r.wished && $('[data-wishlist-page]')) { var card = heart.closest('.pcard'); if (card) card.remove(); }
      }).catch(function (err) { if (err.message !== 'session') toast('Could not update wishlist.', true); });
      return;
    }

    // ---- user menu
    var mt = t.closest('[data-menu-toggle]');
    if (mt) { var m = $('[data-menu]', mt.parentNode); if (m) m.classList.toggle('hidden'); return; }
    if (!t.closest('.usermenu')) $$('[data-menu]').forEach(function (m) { m.classList.add('hidden'); });

    // ---- quantity stepper (product page)
    var st = t.closest('[data-step]');
    if (st) {
      var inp = $('#qty'); if (!inp) return;
      var min = parseInt(inp.min || '1', 10), max = parseInt(inp.max || '10', 10);
      var v = (parseInt(inp.value, 10) || 1) + parseInt(st.getAttribute('data-step'), 10);
      inp.value = Math.max(min, Math.min(max, v));
      return;
    }

    // ---- product tabs
    var tab = t.closest('[data-tab]');
    if (tab) { openTab(tab.getAttribute('data-tab')); return; }

    // ---- gallery thumbs
    var th = t.closest('[data-thumb]');
    if (th) {
      var main = $('#mainImg'); if (main) main.src = th.getAttribute('data-src');
      $$('[data-thumb]').forEach(function (b) { b.classList.toggle('on', b === th); });
      return;
    }

    // ---- filter "more"
    var more = t.closest('[data-more]');
    if (more) {
      var grp = more.closest('.f-group'); var hidden = $$('.f-opt.extra', grp);
      var open = more.getAttribute('data-open') === '1';
      hidden.forEach(function (o) { o.classList.toggle('hidden', open); });
      more.setAttribute('data-open', open ? '0' : '1'); more.textContent = open ? '+ More' : '− Less';
      return;
    }

    // ---- messages that replace a dead link/button on the login card
    var om = t.closest('[data-oauth-msg]');
    if (om) { e.preventDefault(); toast(om.getAttribute('data-oauth-msg'), true); return; }

    // ---- password eye
    var eye = t.closest('[data-toggle-pw]');
    if (eye) { var f = document.getElementById(eye.getAttribute('data-toggle-pw')); if (f) f.type = f.type === 'password' ? 'text' : 'password'; return; }

    // ---- login/register tabs
    var at = t.closest('[data-auth-tab]');
    if (at) {
      var which = at.getAttribute('data-auth-tab');
      $$('[data-auth-tab]').forEach(function (b) { b.classList.toggle('on', b === at); });
      $$('[data-auth-panel]').forEach(function (p) { p.classList.toggle('hidden', p.getAttribute('data-auth-panel') !== which); });
      return;
    }

    // ---- ask the assistant about this product
    var ask = t.closest('[data-ask]');
    if (ask && window.SMChat) { window.SMChat.ask(ask.getAttribute('data-ask')); return; }

    // ---- confirm dialogs
    var c = t.closest('[data-confirm]');
    if (c && !window.confirm(c.getAttribute('data-confirm'))) { e.preventDefault(); }
  });

  function openTab(name) {
    $$('[data-tab]').forEach(function (b) { b.classList.toggle('on', b.getAttribute('data-tab') === name); });
    $$('[data-pane]').forEach(function (p) { p.classList.toggle('on', p.getAttribute('data-pane') === name); });
  }
  if (location.hash === '#reviews' && $('[data-pane="reviews"]')) openTab('reviews');

  document.addEventListener('change', function (e) {
    var t = e.target;
    if (t.matches('[data-autosubmit]')) { var f = t.form || t.closest('form'); if (f) f.submit(); }
    if (t.matches('[data-pay]')) { var u = $('[data-upi-field]'); if (u) u.classList.toggle('hidden', t.value !== 'UPI'); }
    if (t.matches('[data-account-type]')) { var s = $('[data-store-field]'); if (s) s.classList.toggle('hidden', t.value !== 'SELLER' || !t.checked); }
    if (t.matches('[data-preview]') && t.files && t.files[0]) {
      var img = $(t.getAttribute('data-preview')); if (img) img.src = URL.createObjectURL(t.files[0]);
    }
  });

  // initial state for conditional fields
  var checkedPay = $('[data-pay]:checked'); if (checkedPay) { var uf = $('[data-upi-field]'); if (uf) uf.classList.toggle('hidden', checkedPay.value !== 'UPI'); }
  var checkedType = $('[data-account-type]:checked'); if (checkedType) { var sf = $('[data-store-field]'); if (sf) sf.classList.toggle('hidden', checkedType.value !== 'SELLER'); }
})();
