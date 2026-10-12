(() => {
'use strict';

/* ============================================================
   Helpers
   ============================================================ */
const $ = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];

const ICONS = {
  home: '<path d="M3 11l9-8 9 8"/><path d="M5 10v10h14V10"/><path d="M10 20v-5h4v5"/>',
  dashboard: '<rect x="3" y="3" width="7" height="9" rx="1.5"/><rect x="14" y="3" width="7" height="5" rx="1.5"/><rect x="14" y="12" width="7" height="9" rx="1.5"/><rect x="3" y="16" width="7" height="5" rx="1.5"/>',
  devices: '<rect x="4" y="4" width="16" height="16" rx="3"/><rect x="9" y="9" width="6" height="6" rx="1"/>',
  scenes: '<path d="M12 3l1.8 4.7 4.7 1.8-4.7 1.8L12 16l-1.8-4.7-4.7-1.8 4.7-1.8z"/><path d="M19 15l.8 2.2L22 18l-2.2.8L19 21l-.8-2.2L16 18l2.2-.8z"/>',
  energy: '<path d="M13 2L4 14h7l-1 8 9-12h-7z"/>',
  activity: '<path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01"/>',
  settings: '<path d="M4 21v-7M4 10V3M12 21v-9M12 8V3M20 21v-5M20 12V3M1 14h6M9 8h6M17 16h6"/>',
  logout: '<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  trash: '<path d="M3 6h18M8 6V4h8v2M6 6l1 14h10l1-14M10 11v6M14 11v6"/>',
  clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
  bulb: '<path d="M9 18h6M10 21h4M12 3a6 6 0 0 0-4 10.5c.8.8 1 1.5 1 2.5h6c0-1 .2-1.7 1-2.5A6 6 0 0 0 12 3z"/>',
  fan: '<circle cx="12" cy="12" r="1.8"/><path d="M12 10c0-4 1-7 4-7 2.5 0 3 3 1 5-1.2 1.2-3 1.8-5 2z"/><path d="M14 12c4 0 7 1 7 4 0 2.5-3 3-5 1-1.2-1.2-1.8-3-2-5z"/><path d="M12 14c0 4-1 7-4 7-2.5 0-3-3-1-5 1.2-1.2 3-1.8 5-2z"/><path d="M10 12c-4 0-7-1-7-4 0-2.5 3-3 5-1 1.2 1.2 1.8 3 2 5z"/>',
  ac: '<rect x="3" y="5" width="18" height="8" rx="2"/><path d="M7 17c0 1.5-1 2-1 3M12 17c0 1.5-1 2-1 3M17 17c0 1.5-1 2-1 3"/><path d="M7 9h10"/>',
  camera: '<path d="M23 7l-7 5 7 5V7z"/><rect x="1" y="5" width="15" height="14" rx="2"/>',
  moon: '<path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/>',
  away: '<path d="M3 11l9-8 9 8"/><path d="M5 10v10h14V10"/><path d="M9 14l6 4M15 14l-6 4"/>',
  movie: '<rect x="2" y="4" width="20" height="16" rx="2"/><path d="M7 4v16M17 4v16M2 12h20M2 8h5M2 16h5M17 8h5M17 16h5"/>',
  shield: '<path d="M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z"/>',
  bell: '<path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9M13.7 21a2 2 0 0 1-3.4 0"/>',
  check: '<path d="M20 6L9 17l-5-5"/>',
  bolt: '<path d="M13 2L4 14h7l-1 8 9-12h-7z"/>',
  rupee: '<path d="M6 4h12M6 9h12M6 4c6 0 8 2 8 5s-3 5-8 5l8 7"/>',
};
const icon = (name, size = 20) =>
  `<svg class="ic" viewBox="0 0 24 24" width="${size}" height="${size}" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${ICONS[name] || ''}</svg>`;

const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const fmt = (n, d = 2) => Number(n || 0).toLocaleString('en-IN', { minimumFractionDigits: d, maximumFractionDigits: d });
const money = n => '\u20B9' + fmt(n, 2);
const hh = h => String(h).padStart(2, '0') + ':00';
const TYPE_ICON = { Light: 'bulb', Fan: 'fan', AC: 'ac', Camera: 'camera' };
const COLORS = ['#6366f1', '#22d3ee', '#f59e0b', '#10b981', '#ec4899', '#8b5cf6'];

/* ============================================================
   State + API
   ============================================================ */
const state = {
  user: null, page: 'dashboard',
  summary: null, devices: [], rooms: [], scenes: [], report: null, log: [],
  roomFilter: 'all',
};

async function api(path, method = 'GET', body) {
  const opt = { method, credentials: 'same-origin', headers: {} };
  if (method !== 'GET') {
    opt.headers['Content-Type'] = 'application/json';
    opt.body = JSON.stringify(body || {});
  }
  let res;
  try { res = await fetch('/api/' + path, opt); }
  catch { throw new Error('Cannot reach the server. Is it running?'); }
  let data = null;
  try { data = await res.json(); } catch { /* empty body */ }
  if (!res.ok) {
    if (res.status === 401 && state.user) { state.user = null; showAuth(); }
    throw new Error((data && data.error) || 'Request failed (' + res.status + ')');
  }
  return data;
}

/* ============================================================
   Toasts + modal
   ============================================================ */
function toast(message, type = 'success') {
  const el = document.createElement('div');
  el.className = 'toast ' + type;
  el.textContent = message;
  $('#toasts').appendChild(el);
  setTimeout(() => el.remove(), type === 'error' ? 5000 : 3200);
}

function openModal(html) {
  closeModal();
  const wrap = document.createElement('div');
  wrap.className = 'backdrop';
  wrap.innerHTML = `<div class="modal" role="dialog" aria-modal="true">${html}</div>`;
  wrap.addEventListener('mousedown', e => { if (e.target === wrap) closeModal(); });
  $('#modal-root').appendChild(wrap);
  const first = $('input:not([type=radio]),select', wrap);
  if (first) first.focus();
}
function closeModal() { $('#modal-root').innerHTML = ''; }
document.addEventListener('keydown', e => { if (e.key === 'Escape') closeModal(); });

/* ============================================================
   Auth screen
   ============================================================ */
function showAuth() {
  $('#app-view').hidden = true;
  $('#auth-view').hidden = false;
  closeModal();
}

function setAuthTab(tab) {
  $$('[data-auth-tab].tab').forEach(b => b.classList.toggle('active', b.dataset.authTab === tab));
  $('#login-form').hidden = tab !== 'login';
  $('#signup-form').hidden = tab !== 'signup';
  $$('[data-error]').forEach(e => e.hidden = true);
}

function showFormError(form, msg) {
  const box = $('[data-error]', form);
  box.textContent = msg;
  box.hidden = false;
}

function passwordScore(pw) {
  let s = 0;
  if (pw.length >= 8) s++;
  if (/[a-z]/.test(pw) && /[A-Z]/.test(pw)) s++;
  if (/\d/.test(pw)) s++;
  if (/[^A-Za-z0-9]/.test(pw) || pw.length >= 12) s++;
  return s;
}

async function submitAuth(e, path, payloadFn) {
  e.preventDefault();
  const form = e.target;
  const btn = $('button[type=submit]', form);
  $('[data-error]', form).hidden = true;
  let payload;
  try { payload = payloadFn(form); } catch (err) { showFormError(form, err.message); return; }
  btn.disabled = true;
  const label = btn.textContent;
  btn.textContent = 'Please wait...';
  try {
    const r = await api(path, 'POST', payload);
    state.user = r.user;
    form.reset();
    await startApp();
  } catch (err) {
    showFormError(form, err.message);
  } finally {
    btn.disabled = false;
    btn.textContent = label;
  }
}

/* ============================================================
   App shell
   ============================================================ */
const PAGES = {
  dashboard: { title: 'Dashboard', sub: 'Live overview of your home', icon: 'dashboard' },
  devices:   { title: 'Devices', sub: 'Control every device, room by room', icon: 'devices' },
  scenes:    { title: 'Scenes', sub: 'One tap to change the whole house', icon: 'scenes' },
  energy:    { title: 'Energy', sub: 'Where your electricity goes', icon: 'energy' },
  activity:  { title: 'Activity', sub: 'Everything that happened at home', icon: 'activity' },
  settings:  { title: 'Settings', sub: 'Tariff, load limit and account', icon: 'settings' },
};

function buildShell() {
  $('#nav').innerHTML = Object.entries(PAGES).map(([key, p]) =>
    `<button class="nav-item" data-action="nav" data-page="${key}">${icon(p.icon)}<span>${p.title}</span></button>`).join('');
  const initials = state.user.displayName.split(/\s+/).map(w => w[0]).slice(0, 2).join('').toUpperCase();
  $('#user-avatar').textContent = initials;
  $('#user-name').textContent = state.user.displayName;
  $('#user-handle').textContent = '@' + state.user.username;
}

async function startApp() {
  $('#auth-view').hidden = true;
  $('#app-view').hidden = false;
  buildShell();
  const sc = await api('scenes');
  state.scenes = sc.scenes;
  await refresh(false);
  const wanted = location.hash.slice(1);
  navigate(PAGES[wanted] ? wanted : 'dashboard');
}

async function refresh(render = true) {
  const [summary, devs, report, log] = await Promise.all([api('summary'), api('devices'), api('report'), api('log')]);
  state.summary = summary;
  state.devices = devs.devices;
  state.rooms = devs.rooms;
  state.report = report;
  state.log = log.entries;
  if (render) renderPage();
}

function navigate(page) {
  state.page = page;
  location.hash = page;
  $$('.nav-item').forEach(n => n.classList.toggle('active', n.dataset.page === page));
  $('#page-title').textContent = PAGES[page].title;
  $('#page-sub').textContent = PAGES[page].sub;
  renderPage();
  window.scrollTo(0, 0);
}

function renderPage() {
  const s = state.summary;
  $('#clock-chip').innerHTML = `${icon('clock', 16)} Day ${s.day} &middot; ${esc(s.clock)}`;
  const view = { dashboard: renderDashboard, devices: renderDevices, scenes: renderScenes,
                 energy: renderEnergy, activity: renderActivity, settings: renderSettings }[state.page];
  $('#content').innerHTML = view();
}

/* run a server action, then reload everything so the UI always mirrors the back end */
async function act(fn, okMsg) {
  try {
    const r = await fn();
    await refresh();
    if (okMsg) toast(typeof okMsg === 'function' ? okMsg(r) : okMsg);
    return r;
  } catch (err) {
    toast(err.message, 'error');
    try { await refresh(); } catch { /* ignore */ }
  }
}

/* ============================================================
   Page renderers
   ============================================================ */
function statCard(iconName, tone, label, value, extra = '') {
  return `<div class="card stat"><div class="ico ico-${tone}">${icon(iconName, 22)}</div>
    <div style="flex:1;min-width:0"><div class="label">${label}</div><div class="value">${value}</div>${extra}</div></div>`;
}

function barList(map, unit = 'kWh') {
  const entries = Object.entries(map).sort((a, b) => b[1] - a[1]);
  const max = Math.max(...entries.map(e => e[1]), 0.0001);
  if (!entries.length || entries.every(e => e[1] === 0)) return '<div class="empty">No usage yet. Advance the clock to see data.</div>';
  return `<div class="bars">${entries.map(([k, v]) => `
    <div class="bar-row"><div class="bar-top"><span>${esc(k)}</span><span>${fmt(v, 2)} ${unit}</span></div>
    <div class="bar"><i style="width:${(v / max * 100).toFixed(1)}%"></i></div></div>`).join('')}</div>`;
}

function donut(map) {
  const entries = Object.entries(map).filter(([, v]) => v > 0);
  const total = entries.reduce((a, [, v]) => a + v, 0);
  if (!total) return '<div class="empty">No usage yet.</div>';
  let acc = 0;
  const stops = entries.map(([, v], i) => {
    const start = acc / total * 100; acc += v;
    return `${COLORS[i % COLORS.length]} ${start.toFixed(2)}% ${(acc / total * 100).toFixed(2)}%`;
  });
  return `<div class="donut-wrap">
    <div class="donut" style="background:conic-gradient(${stops.join(',')})"><div class="donut-hole"><strong>${fmt(total, 1)}</strong><span>kWh</span></div></div>
    <ul class="legend">${entries.map(([k, v], i) =>
      `<li><i style="background:${COLORS[i % COLORS.length]}"></i><span>${esc(k)}</span><span>${(v / total * 100).toFixed(0)}%</span></li>`).join('')}</ul></div>`;
}

function roomGroups() {
  const groups = {};
  state.devices.forEach(d => (groups[d.room] ||= []).push(d));
  return groups;
}

function renderDashboard() {
  const s = state.summary, r = state.report;
  const pct = Math.min(100, s.totalWatts / s.powerLimit * 100);
  const cls = pct > 85 ? 'bad' : pct > 60 ? 'warn' : '';
  const groups = roomGroups();
  const recent = state.log.slice(0, 6);

  return `
  ${s.overload ? `<div class="banner bad">${icon('bolt', 18)} Load of ${fmt(s.totalWatts, 0)} W is above your ${fmt(s.powerLimit, 0)} W limit. Switch something off.</div>` : ''}
  <div class="grid g4">
    ${statCard('devices', 'indigo', 'Active devices', `${s.activeCount}<small> / ${s.deviceCount}</small>`)}
    ${statCard('bolt', 'amber', 'Live load', `${fmt(s.totalWatts, 0)}<small> W</small>`,
        `<div class="progress ${cls}"><i style="width:${pct.toFixed(1)}%"></i></div>`)}
    ${statCard('energy', 'green', 'Energy used', `${fmt(s.totalKwh, 2)}<small> kWh</small>`)}
    ${statCard('rupee', 'violet', 'Estimated bill', money(s.totalCost), `<div class="sub" style="margin-top:2px">at ${money(s.rate)} per kWh</div>`)}
  </div>

  <div class="grid g2 mt">
    <div class="stack">
      <div class="card">
        <div class="card-head"><div><h3>Quick scenes</h3><p class="sub">Apply a mode to every device at once</p></div>
          <button class="btn btn-outline btn-sm" data-action="nav" data-page="scenes">All scenes</button></div>
        <div class="scene-grid">${state.scenes.map(sceneCard).join('')}</div>
      </div>
      <div class="card">
        <div class="card-head"><div><h3>Rooms</h3><p class="sub">Switch an entire room on or off</p></div></div>
        ${Object.keys(groups).length ? `<div class="room-list">${Object.entries(groups).map(([room, list]) => {
          const on = list.filter(d => d.on).length, w = list.reduce((a, d) => a + d.watts, 0);
          return `<div class="room-row"><div><strong>${esc(room)}</strong><small>${on} of ${list.length} on &middot; ${fmt(w, 0)} W</small></div>
            <div class="room-actions"><button class="btn btn-outline btn-sm" data-action="room-power" data-room="${esc(room)}" data-on="true">All on</button>
            <button class="btn btn-outline btn-sm" data-action="room-power" data-room="${esc(room)}" data-on="false">All off</button></div></div>`;
        }).join('')}</div>` : '<div class="empty">No devices yet. Add your first device.</div>'}
      </div>
    </div>

    <div class="stack">
      <div class="card">
        <h3>Simulation clock</h3><p class="sub">Move time forward to see schedules run and energy build up</p>
        <div class="bigclock" style="margin:16px 0">${esc(s.clock)} <span class="sub" style="font-size:15px;font-weight:600">Day ${s.day}</span></div>
        <div class="time-btns">${[1, 6, 12, 24].map(h => `<button class="btn btn-outline btn-sm" data-action="advance" data-hours="${h}">+${h}h</button>`).join('')}</div>
        <form class="inline-form" data-form="advance"><input type="number" name="hours" min="1" max="72" placeholder="Hours" required>
          <button class="btn btn-primary btn-sm" type="submit">Advance</button></form>
      </div>
      <div class="card">
        <div class="card-head"><div><h3>Energy by room</h3><p class="sub">Total so far</p></div></div>
        ${barList(r.byRoom)}
      </div>
      <div class="card">
        <div class="card-head"><div><h3>Recent activity</h3></div><button class="btn btn-outline btn-sm" data-action="nav" data-page="activity">View all</button></div>
        ${logList(recent)}
      </div>
    </div>
  </div>`;
}

function sceneCard(sc) {
  const map = { night: ['moon', 'indigo'], away: ['away', 'sky'], movie: ['movie', 'violet'] };
  const [ic, tone] = map[sc.key] || ['scenes', 'indigo'];
  return `<button class="scene" data-action="scene" data-key="${esc(sc.key)}">
    <span class="ico ico-${tone}">${icon(ic, 20)}</span><strong>${esc(sc.name)}</strong><small>${esc(sc.description)}</small></button>`;
}

function renderScenes() {
  return `<div class="card"><div class="card-head"><div><h3>Scenes</h3>
    <p class="sub">Each scene is a bundle of actions run by the back-end Controller.</p></div></div>
    <div class="scene-grid">${state.scenes.map(sceneCard).join('')}</div></div>`;
}

function deviceCard(d) {
  const st = d.settings;
  let controls = '';
  if (d.type === 'Light') {
    controls = `
      <div class="ctl"><div class="ctl-top"><span>Brightness</span><output>${st.brightness}%</output></div>
        <input type="range" min="0" max="100" value="${st.brightness}" data-live data-action="cfg" data-setting="brightness" data-id="${d.id}"></div>
      <div class="ctl"><div class="ctl-top"><span>Colour</span></div>
        <div class="seg">${['WARM', 'COOL', 'DAYLIGHT'].map(c =>
          `<button class="${st.color === c ? 'active' : ''}" data-action="set" data-id="${d.id}" data-setting="color" data-value="${c}">${c[0] + c.slice(1).toLowerCase()}</button>`).join('')}</div></div>`;
  } else if (d.type === 'Fan') {
    controls = `
      <div class="ctl"><div class="ctl-top"><span>Speed</span><output>${st.speed}</output></div>
        <input type="range" min="1" max="5" value="${st.speed}" data-live data-action="cfg" data-setting="speed" data-id="${d.id}"></div>
      <div class="row"><span class="ctl-top">Oscillate</span>
        <label class="switch"><input type="checkbox" data-action="osc" data-id="${d.id}" ${st.oscillate === 'on' ? 'checked' : ''}><span></span></label></div>`;
  } else if (d.type === 'AC') {
    controls = `
      <div class="ctl"><div class="ctl-top"><span>Target temperature</span></div>
        <div class="stepper"><button data-action="temp" data-id="${d.id}" data-delta="-1" aria-label="Lower">&minus;</button>
        <strong>${st.temp}&deg;C</strong><button data-action="temp" data-id="${d.id}" data-delta="1" aria-label="Raise">+</button></div></div>
      <div class="ctl"><div class="ctl-top"><span>Mode</span></div>
        <div class="seg">${['COOL', 'DRY', 'FAN'].map(m =>
          `<button class="${st.mode === m ? 'active' : ''}" data-action="set" data-id="${d.id}" data-setting="mode" data-value="${m}">${m[0] + m.slice(1).toLowerCase()}</button>`).join('')}</div></div>`;
  } else if (d.type === 'Camera') {
    controls = `
      <div class="row"><span class="ctl-top">Recording</span><span class="pill ${st.recording === 'on' ? 'alert' : ''}">${st.recording === 'on' ? '&#9679; Recording' : 'Idle'}</span></div>
      <div class="row"><span class="ctl-top">Motion events</span><strong>${st.motionEvents}</strong></div>
      <button class="btn btn-outline btn-sm" data-action="motion" data-id="${d.id}" ${d.on ? '' : 'disabled'}>${icon('bell', 15)} Simulate motion</button>`;
  }
  const sched = d.schedulable
    ? `<button class="link-btn" data-action="sched-open" data-id="${d.id}">${icon('clock', 15)} ${d.schedule ? `Daily ${hh(d.schedule.on)} to ${hh(d.schedule.off)}` : 'Add schedule'}</button>`
    : `<span class="sub" style="font-size:13px">${icon('shield', 14)} Always-on device</span>`;

  return `<article class="device ${d.on ? 'on' : ''}">
    <div class="dev-top"><div class="dev-icon t-${d.type}">${icon(TYPE_ICON[d.type], 24)}</div>
      <div class="dev-title"><h4 title="${esc(d.name)}">${esc(d.name)}</h4><small>${d.type} &middot; ID ${d.id}</small></div>
      <label class="switch" title="Power"><input type="checkbox" data-action="power" data-id="${d.id}" ${d.on ? 'checked' : ''} aria-label="Power ${esc(d.name)}"><span></span></label></div>
    <div class="dev-meta"><span class="pill ${d.on ? 'on' : ''}">${d.on ? fmt(d.watts, 0) + ' W' : 'Off'}</span>
      <span class="pill">${fmt(d.energyKwh, 2)} kWh used</span></div>
    <div class="dev-controls ${d.on || d.type === 'Camera' ? '' : 'dim'}">${controls}</div>
    <div class="dev-foot">${sched}
      <button class="link-btn danger" data-action="delete" data-id="${d.id}" aria-label="Remove ${esc(d.name)}">${icon('trash', 15)} Remove</button></div>
  </article>`;
}

function renderDevices() {
  const groups = roomGroups();
  const rooms = Object.keys(groups).sort();
  if (!rooms.length) return `<div class="card empty">No devices yet.<br><br><button class="btn btn-primary" data-action="add-open">${icon('plus', 16)} Add your first device</button></div>`;
  if (state.roomFilter !== 'all' && !groups[state.roomFilter]) state.roomFilter = 'all';
  const chips = ['all', ...rooms].map(r =>
    `<button class="chip ${state.roomFilter === r ? 'active' : ''}" data-action="filter" data-room="${esc(r)}">${r === 'all' ? 'All rooms' : esc(r)}</button>`).join('');
  const shown = rooms.filter(r => state.roomFilter === 'all' || r === state.roomFilter);
  return `<div class="chips">${chips}</div>` + shown.map(room => {
    const list = groups[room], on = list.filter(d => d.on).length;
    return `<section class="room-section"><div class="room-head"><h3>${esc(room)}<small>${on} of ${list.length} on</small></h3>
      <div class="room-actions"><button class="btn btn-outline btn-sm" data-action="room-power" data-room="${esc(room)}" data-on="true">All on</button>
      <button class="btn btn-outline btn-sm" data-action="room-power" data-room="${esc(room)}" data-on="false">All off</button></div></div>
      <div class="device-grid">${list.map(deviceCard).join('')}</div></section>`;
  }).join('');
}

function renderEnergy() {
  const r = state.report;
  const top = r.top;
  return `
  <div class="grid g3">
    ${statCard('energy', 'green', 'Total energy', `${fmt(r.totalKwh, 2)}<small> kWh</small>`)}
    ${statCard('rupee', 'violet', 'Estimated bill', money(r.totalCost))}
    ${statCard('bolt', 'amber', 'Top consumer', top ? esc(top.name) : '&ndash;', top ? `<div class="sub">${fmt(top.share, 0)}% of usage &middot; ${esc(top.room)}</div>` : '')}
  </div>
  <div class="tip mt">${icon('bulb', 20)}<div><strong>Saving tip</strong><div>${esc(r.suggestion)}</div></div></div>
  <div class="grid g2e mt">
    <div class="card"><div class="card-head"><div><h3>By device type</h3></div></div>${donut(r.byType)}</div>
    <div class="card"><div class="card-head"><div><h3>By room</h3></div></div>${barList(r.byRoom)}</div>
  </div>
  <div class="card mt"><div class="card-head"><div><h3>Per-device usage</h3><p class="sub">Rate: ${money(r.rate)} per kWh</p></div></div>
    <div class="table-wrap"><table><thead><tr><th>Device</th><th>Type</th><th>Room</th><th class="num">Hours on</th><th class="num">kWh</th><th class="num">Cost</th></tr></thead><tbody>
    ${r.devices.map(d => `<tr><td><strong>${esc(d.name)}</strong></td><td>${d.type}</td><td>${esc(d.room)}</td>
      <td class="num">${fmt(d.hours, 1)}</td><td class="num">${fmt(d.kwh, 3)}</td><td class="num">${money(d.cost)}</td></tr>`).join('') ||
      '<tr><td colspan="6" class="empty">No devices.</td></tr>'}
    </tbody></table></div></div>`;
}

function logList(entries) {
  if (!entries.length) return '<div class="empty">Nothing yet.</div>';
  return `<ul class="timeline">${entries.map(line => {
    const [time, ...rest] = line.split(' | ');
    const msg = rest.join(' | ');
    let cls = 'ok';
    if (/ALERT/.test(msg)) cls = 'alert';
    else if (/WARNING/.test(msg)) cls = 'warn';
    else if (/\[Schedule\]/.test(msg)) cls = 'sched';
    else if (/Scene/.test(msg)) cls = 'scene';
    return `<li><span class="dot ${cls}"></span><div><div>${esc(msg)}</div><time>${esc(time)}</time></div></li>`;
  }).join('')}</ul>`;
}

function renderActivity() {
  return `<div class="card"><div class="card-head"><div><h3>Activity log</h3><p class="sub">Newest first &middot; last 100 events</p></div></div>${logList(state.log)}</div>`;
}

function renderSettings() {
  const s = state.summary;
  return `<div class="grid g2e">
    <div class="card"><h3>Energy &amp; safety</h3><p class="sub" style="margin-bottom:16px">Used by the bill estimate and the overload warning.</p>
      <form data-form="settings">
        <label class="field"><span>Electricity rate (&#8377; per kWh)</span><input type="number" name="rate" step="0.01" min="0.01" value="${s.rate}" required></label>
        <label class="field"><span>Load limit (watts)</span><input type="number" name="limit" step="50" min="100" value="${s.powerLimit}" required></label>
        <button class="btn btn-primary" type="submit">Save changes</button></form></div>
    <div class="card"><h3>Account</h3><p class="sub" style="margin-bottom:16px">Signed in as <strong>${esc(state.user.displayName)}</strong> (@${esc(state.user.username)}).</p>
      <p class="sub" style="margin-bottom:16px">Your home and devices are private to your account.</p>
      <button class="btn btn-outline" data-action="logout">${icon('logout', 16)} Sign out</button></div></div>`;
}

/* ============================================================
   Modals
   ============================================================ */
function addDeviceModal() {
  const types = [['Light', 'bulb'], ['Fan', 'fan'], ['AC', 'ac'], ['Camera', 'camera']];
  openModal(`<h3>Add a device</h3><p class="sub">It joins your home straight away.</p>
    <form data-form="add-device">
      <div class="type-grid">${types.map(([t, ic], i) => `<div class="type-opt"><input type="radio" name="type" id="t-${t}" value="${t.toLowerCase()}" ${i === 0 ? 'checked' : ''}>
        <label for="t-${t}">${icon(ic, 24)}${t}</label></div>`).join('')}</div>
      <label class="field"><span>Device name</span><input name="name" maxlength="30" required placeholder="e.g. Study Lamp"></label>
      <label class="field"><span>Room</span><input name="room" maxlength="30" list="room-options" required placeholder="e.g. Study">
        <datalist id="room-options">${state.rooms.map(r => `<option value="${esc(r)}">`).join('')}</datalist></label>
      <div class="modal-actions"><button type="button" class="btn btn-outline" data-action="close-modal">Cancel</button>
        <button class="btn btn-primary" type="submit">Add device</button></div></form>`);
}

function scheduleModal(id) {
  const d = state.devices.find(x => x.id === id);
  if (!d) return;
  const opts = sel => Array.from({ length: 24 }, (_, h) => `<option value="${h}" ${h === sel ? 'selected' : ''}>${hh(h)}</option>`).join('');
  const cur = d.schedule || { on: 18, off: 23 };
  openModal(`<h3>Schedule &ndash; ${esc(d.name)}</h3><p class="sub">Runs every day. If OFF is earlier than ON, it runs overnight.</p>
    <form data-form="schedule" data-id="${id}">
      <div class="two"><label class="field"><span>Switch on at</span><select name="on">${opts(cur.on)}</select></label>
      <label class="field"><span>Switch off at</span><select name="off">${opts(cur.off)}</select></label></div>
      <div class="modal-actions">
        ${d.schedule ? `<button type="button" class="btn btn-outline" data-action="sched-clear" data-id="${id}" style="margin-right:auto">Remove schedule</button>` : ''}
        <button type="button" class="btn btn-outline" data-action="close-modal">Cancel</button>
        <button class="btn btn-primary" type="submit">Save schedule</button></div></form>`);
}

function confirmDelete(id) {
  const d = state.devices.find(x => x.id === id);
  if (!d) return;
  openModal(`<h3>Remove ${esc(d.name)}?</h3><p class="sub">This device and its usage history will be removed from your home.</p>
    <div class="modal-actions"><button class="btn btn-outline" data-action="close-modal">Cancel</button>
    <button class="btn btn-danger" data-action="delete-confirm" data-id="${id}">Remove</button></div>`);
}

/* ============================================================
   Event handling (one listener per event type)
   ============================================================ */
const deviceApi = (id, tail, body, method = 'POST') => api(`devices/${id}/${tail}`, method, body);
const findDevice = id => state.devices.find(d => d.id === Number(id));

document.addEventListener('click', e => {
  const tab = e.target.closest('[data-auth-tab]');
  if (tab) { e.preventDefault(); setAuthTab(tab.dataset.authTab); return; }

  const el = e.target.closest('[data-action]');
  if (!el || el.tagName === 'INPUT') return;
  const id = el.dataset.id ? Number(el.dataset.id) : null;

  switch (el.dataset.action) {
    case 'nav': navigate(el.dataset.page); break;
    case 'logout': api('logout', 'POST').finally(() => { state.user = null; showAuth(); setAuthTab('login'); }); break;
    case 'toggle-pw': {
      const input = $('input', el.parentElement);
      const show = input.type === 'password';
      input.type = show ? 'text' : 'password';
      el.textContent = show ? 'Hide' : 'Show';
      break;
    }
    case 'filter': state.roomFilter = el.dataset.room; renderPage(); break;
    case 'add-open': addDeviceModal(); break;
    case 'close-modal': closeModal(); break;
    case 'sched-open': scheduleModal(id); break;
    case 'sched-clear': closeModal(); act(() => deviceApi(id, 'schedule', null, 'DELETE'), 'Schedule removed'); break;
    case 'delete': confirmDelete(id); break;
    case 'delete-confirm': closeModal(); act(() => api('devices/' + id, 'DELETE'), 'Device removed'); break;
    case 'set': act(() => deviceApi(id, 'config', { setting: el.dataset.setting, value: el.dataset.value })); break;
    case 'temp': {
      const d = findDevice(id);
      act(() => deviceApi(id, 'config', { setting: 'temp', value: String(Number(d.settings.temp) + Number(el.dataset.delta)) }));
      break;
    }
    case 'motion':
      act(() => deviceApi(id, 'motion', {})).then(r => { if (r) toast(r.message, /ALERT/.test(r.message) ? 'warn' : 'error'); });
      break;
    case 'room-power':
      act(() => api('rooms/power', 'POST', { room: el.dataset.room, on: el.dataset.on === 'true' }),
          r => `${r.updated} device${r.updated === 1 ? '' : 's'} in ${el.dataset.room} turned ${el.dataset.on === 'true' ? 'on' : 'off'}`);
      break;
    case 'scene': {
      const sc = state.scenes.find(x => x.key === el.dataset.key);
      act(() => api('scenes/run', 'POST', { key: el.dataset.key }), `${sc ? sc.name : 'Scene'} activated`);
      break;
    }
    case 'advance': act(() => api('time/advance', 'POST', { hours: Number(el.dataset.hours) }), `Advanced ${el.dataset.hours} hours`); break;
    default: break;
  }
});

document.addEventListener('change', e => {
  const el = e.target.closest('input[data-action],select[data-action]');
  if (!el) return;
  const id = Number(el.dataset.id);
  switch (el.dataset.action) {
    case 'power': act(() => deviceApi(id, 'power', { on: el.checked })); break;
    case 'cfg': act(() => deviceApi(id, 'config', { setting: el.dataset.setting, value: el.value })); break;
    case 'osc': act(() => deviceApi(id, 'config', { setting: 'oscillate', value: el.checked ? 'on' : 'off' })); break;
    default: break;
  }
});

document.addEventListener('input', e => {
  if (e.target.matches('input[data-live]')) {
    const out = $('output', e.target.closest('.ctl'));
    if (out) out.textContent = e.target.value + (e.target.dataset.setting === 'brightness' ? '%' : '');
  }
  if (e.target.matches('#signup-form [name=password]')) {
    const score = passwordScore(e.target.value);
    $('#signup-form .strength').dataset.level = score;
    $('#pw-hint').textContent = ['Use 8+ characters with a mix of letters and numbers.', 'Weak', 'Fair', 'Good', 'Strong'][e.target.value ? score : 0];
  }
});

document.addEventListener('submit', async e => {
  const form = e.target;
  if (form.id === 'login-form' || form.id === 'signup-form') return;   // handled separately
  const kind = form.dataset.form;
  if (!kind) return;
  e.preventDefault();
  const f = new FormData(form);
  switch (kind) {
    case 'add-device':
      closeModal();
      act(() => api('devices', 'POST', { type: f.get('type'), name: f.get('name'), room: f.get('room') }), 'Device added');
      break;
    case 'schedule':
      closeModal();
      act(() => deviceApi(form.dataset.id, 'schedule', { on: Number(f.get('on')), off: Number(f.get('off')) }), 'Schedule saved');
      break;
    case 'advance':
      act(() => api('time/advance', 'POST', { hours: Number(f.get('hours')) }), `Advanced ${f.get('hours')} hours`);
      break;
    case 'settings':
      act(() => api('settings', 'POST', { rate: Number(f.get('rate')), limit: Number(f.get('limit')) }), 'Settings saved');
      break;
    default: break;
  }
});

$('#login-form').addEventListener('submit', e => submitAuth(e, 'login', f => {
  const username = f.username.value.trim(), password = f.password.value;
  if (!username || !password) throw new Error('Enter your username and password');
  return { username, password };
}));

$('#signup-form').addEventListener('submit', e => submitAuth(e, 'signup', f => {
  const name = f.name.value.trim(), username = f.username.value.trim(), password = f.password.value;
  if (name.length < 2) throw new Error('Please enter your name');
  if (!/^[A-Za-z0-9_]{3,20}$/.test(username)) throw new Error('Username must be 3-20 letters, numbers or underscores');
  if (password.length < 8) throw new Error('Password must be at least 8 characters');
  if (password !== f.confirm.value) throw new Error('Passwords do not match');
  return { name, username, password };
}));

window.addEventListener('hashchange', () => {
  const page = location.hash.slice(1);
  if (state.user && PAGES[page] && page !== state.page) navigate(page);
});

/* ============================================================
   Boot
   ============================================================ */
(async function init() {
  $('#brand-logo').innerHTML = icon('home', 20);
  $('#side-logo').innerHTML = icon('home', 20);
  $('#f1').innerHTML = icon('devices', 20);
  $('#f2').innerHTML = icon('scenes', 20);
  $('#f3').innerHTML = icon('rupee', 20);
  $('#logout-ic').innerHTML = icon('logout', 16);
  $('#add-ic').innerHTML = icon('plus', 16);
  try {
    const me = await api('me');
    if (me.user) { state.user = me.user; await startApp(); } else { showAuth(); }
  } catch {
    showAuth();
  }
})();
})();
