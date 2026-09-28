/** Shared UI kit: icons, escaping, formatting, toasts, modals. */

const ICON_PATHS = {
  users: '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>',
  userCheck: '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="m16 11 2 2 4-4"/>',
  building: '<path d="M6 22V4a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v18Z"/><path d="M6 12H4a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2h2"/><path d="M18 9h2a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2h-2"/><path d="M10 6h4"/><path d="M10 10h4"/><path d="M10 14h4"/><path d="M10 18h4"/>',
  calendarCheck: '<path d="M8 2v4"/><path d="M16 2v4"/><rect x="3" y="4" width="18" height="18" rx="2"/><path d="M3 10h18"/><path d="m9 16 2 2 4-4"/>',
  plane: '<path d="M17.8 19.2 16 11l3.5-3.5C21 6 21.5 4 21 3c-1-.5-3 0-4.5 1.5L13 8 4.8 6.2c-.5-.1-.9.1-1.1.5l-.3.5c-.2.5-.1 1 .3 1.3L9 12l-2 3H4l-1 1 3 2 2 3 1-1v-3l3-2 3.5 5.3c.3.4.8.5 1.3.3l.5-.2c.4-.3.6-.7.5-1.2z"/>',
  wallet: '<path d="M19 7V4a1 1 0 0 0-1-1H5a2 2 0 0 0 0 4h15a1 1 0 0 1 1 1v4h-3a2 2 0 0 0 0 4h3a1 1 0 0 0 1-1v-2a1 1 0 0 0-1-1"/><path d="M3 5v14a2 2 0 0 0 2 2h15a1 1 0 0 0 1-1v-4"/>',
  plus: '<path d="M5 12h14"/><path d="M12 5v14"/>',
  pencil: '<path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/><path d="m15 5 4 4"/>',
  trash: '<path d="M3 6h18"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/><path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><path d="M10 11v6"/><path d="M14 11v6"/>',
  x: '<path d="M18 6 6 18"/><path d="m6 6 12 12"/>',
  check: '<path d="M20 6 9 17l-5-5"/>',
  alert: '<path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><path d="M12 9v4"/><path d="M12 17h.01"/>',
  info: '<circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/>',
  clock: '<circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>',
  search: '<circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>',
  inbox: '<path d="M22 12h-6l-2 3h-4l-2-3H2"/><path d="M5.45 5.11 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z"/>',
  grid: '<rect x="3" y="3" width="7" height="9" rx="1"/><rect x="14" y="3" width="7" height="5" rx="1"/><rect x="14" y="12" width="7" height="9" rx="1"/><rect x="3" y="16" width="7" height="5" rx="1"/>',
  bell: '<path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/>',
  briefcase: '<rect width="20" height="14" x="2" y="7" rx="2"/><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/>',
  megaphone: '<path d="m3 11 18-5v12L3 14v-3z"/><path d="M11.6 16.8a3 3 0 1 1-5.8-1.6"/>',
  key: '<circle cx="7.5" cy="15.5" r="5.5"/><path d="m21 2-9.6 9.6"/><path d="m15.5 7.5 3 3L22 7l-3-3"/>',
  shield: '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10"/>',
  userPlus: '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><line x1="19" x2="19" y1="8" y2="14"/><line x1="22" x2="16" y1="11" y2="11"/>',
  flag: '<path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"/><line x1="4" x2="4" y1="22" y2="15"/>',
  arrowRight: '<path d="M5 12h14"/><path d="m12 5 7 7-7 7"/>',
  copy: '<rect width="14" height="14" x="8" y="8" rx="2"/><path d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"/>',
  download: '<path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" x2="12" y1="15" y2="3"/>',
};

export function icon(name, size = 16) {
  return `<svg viewBox="0 0 24 24" width="${size}" height="${size}" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${ICON_PATHS[name] || ''}</svg>`;
}

export function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}

const moneyFormat = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 });

export function money(value) {
  const number = Number(value ?? 0);
  return moneyFormat.format(Number.isFinite(number) ? number : 0);
}

export function date(iso) {
  if (!iso) return '—';
  const parsed = new Date(String(iso).slice(0, 10) + 'T00:00:00');
  return Number.isNaN(parsed.getTime())
    ? String(iso)
    : parsed.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}

export function time(value) {
  if (!value) return '—';
  return String(value).slice(0, 5);
}

export function dateTime(iso) {
  if (!iso) return '—';
  const parsed = new Date(iso);
  return Number.isNaN(parsed.getTime())
    ? String(iso)
    : parsed.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }) + ', ' + String(iso).slice(11, 16);
}

export function initials(name) {
  return String(name || '?')
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0].toUpperCase())
    .join('');
}

export function badge(text, variant) {
  return `<span class="badge badge-${variant}">${escapeHtml(text)}</span>`;
}

export function debounce(fn, waitMs) {
  let timer = null;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), waitMs);
  };
}

/* ---------------------------------------------------------------- toasts */

export function toast(message, type = 'success') {
  const stack = document.getElementById('toast-stack');
  if (!stack) return;
  const element = document.createElement('div');
  element.className = `toast ${type}`;
  element.setAttribute('data-testid', `toast-${type}`);
  element.innerHTML = `<span class="toast-icon">${icon(type === 'success' ? 'check' : type === 'error' ? 'alert' : 'info', 15)}</span><span>${escapeHtml(message)}</span>`;
  stack.appendChild(element);
  setTimeout(() => {
    element.classList.add('leaving');
    setTimeout(() => element.remove(), 200);
  }, 3600);
}

/* --------------------------------------------------------------- modals */

/**
 * Opens a modal dialog. `body` and `footer` are HTML strings; `onMount(overlay, close)`
 * is called immediately so callers can wire events. Escape and backdrop clicks close.
 */
export function openModal({ title, sub = '', body, footer = '', size = '', onMount }) {
  const overlay = document.createElement('div');
  overlay.className = 'modal-overlay';
  overlay.innerHTML = `
    <div class="modal ${size}" role="dialog" aria-modal="true" aria-label="${escapeHtml(title)}">
      <div class="modal-header">
        <div>
          <h3>${escapeHtml(title)}</h3>
          ${sub ? `<p class="modal-sub">${escapeHtml(sub)}</p>` : ''}
        </div>
        <button class="icon-btn" type="button" data-modal-close aria-label="Close dialog">${icon('x', 16)}</button>
      </div>
      <div class="modal-body">${body}</div>
      ${footer ? `<div class="modal-footer">${footer}</div>` : ''}
    </div>`;

  document.body.appendChild(overlay);
  const close = () => {
    document.removeEventListener('keydown', onKey);
    overlay.remove();
  };
  const onKey = (event) => { if (event.key === 'Escape') close(); };
  document.addEventListener('keydown', onKey);
  overlay.addEventListener('mousedown', (event) => { if (event.target === overlay) close(); });
  overlay.querySelector('[data-modal-close]').addEventListener('click', close);
  if (onMount) onMount(overlay, close);
  const firstInput = overlay.querySelector('input, select, textarea');
  if (firstInput) firstInput.focus();
  return { overlay, close };
}

/** Confirmation dialog; resolves true when confirmed. */
export function confirmDialog({ title, message, confirmLabel = 'Confirm', danger = false }) {
  return new Promise((resolve) => {
    openModal({
      title,
      size: 'narrow',
      body: `<p style="font-size:13.5px; color: var(--ink-muted);">${escapeHtml(message)}</p>`,
      footer: `
        <button class="btn btn-secondary" type="button" data-cancel>Cancel</button>
        <button class="btn ${danger ? 'btn-danger' : 'btn-primary'}" type="button" data-confirm data-testid="confirm-dialog-btn">${escapeHtml(confirmLabel)}</button>`,
      onMount(overlay, close) {
        overlay.querySelector('[data-cancel]').addEventListener('click', () => { close(); resolve(false); });
        overlay.querySelector('[data-confirm]').addEventListener('click', () => { close(); resolve(true); });
        overlay.addEventListener('mousedown', (event) => { if (event.target === overlay) resolve(false); });
      },
    });
  });
}

/* ---------------------------------------------------- credentials modal */

/** Shows one-time login credentials with copy buttons. */
export function credentialsModal({ title = 'Login created', sub = '', username, password, message = '' }) {
  const row = (label, value, id) => `
    <div class="cred-row">
      <span class="cred-label">${escapeHtml(label)}</span>
      <code class="cred-value" data-testid="cred-${id}">${escapeHtml(value)}</code>
      <button class="icon-btn" type="button" data-copy="${escapeHtml(value)}" title="Copy ${escapeHtml(label)}"
              aria-label="Copy ${escapeHtml(label)}">${icon('copy', 15)}</button>
    </div>`;

  openModal({
    title,
    sub,
    size: 'narrow',
    body: `
      <div class="cred-box" data-testid="credentials-box">
        ${message ? `<p class="cred-message">${escapeHtml(message)}</p>` : ''}
        ${row('Username', username, 'username')}
        ${row('Password', password, 'password')}
        <p class="cred-note">${icon('alert', 13)} Copy these now — the password is shown only once and cannot be viewed again.</p>
      </div>`,
    footer: `<button class="btn btn-primary btn-block" type="button" data-done data-testid="credentials-done-btn">Done</button>`,
    onMount(overlay, close) {
      overlay.querySelector('[data-done]').addEventListener('click', close);
      overlay.querySelectorAll('[data-copy]').forEach((button) => {
        button.addEventListener('click', async () => {
          try {
            await navigator.clipboard.writeText(button.dataset.copy);
            toast('Copied to clipboard', 'info');
          } catch (_) {
            toast('Copy failed — select the text manually', 'error');
          }
        });
      });
    },
  });
}
