import { api } from '../api.js';
import { badge, confirmDialog, credentialsModal, escapeHtml, icon, initials, openModal, toast } from '../ui.js';

let rootEl = null;
let accounts = [];

export async function render(root) {
  rootEl = root;

  root.innerHTML = `
    <div class="toolbar">
      <div class="filters"><span class="muted" style="font-size:13px;">Admin and HR logins. HR accounts can create employee logins.</span></div>
      <button class="btn btn-primary" type="button" id="add-hr-btn" data-testid="add-hr-btn">
        ${icon('userPlus', 16)} Create HR Account
      </button>
    </div>
    <div id="accounts-wrap" data-testid="accounts-wrap"></div>`;

  root.querySelector('#add-hr-btn').addEventListener('click', openCreator);
  await load();
}

async function load() {
  accounts = await api('/users');
  const wrap = rootEl.querySelector('#accounts-wrap');

  const rows = accounts.map((u) => `
    <tr data-testid="account-row" data-account-id="${u.id}">
      <td>
        <div class="employee-cell">
          <span class="avatar">${escapeHtml(initials(u.fullName))}</span>
          <div>
            <span class="name">${escapeHtml(u.fullName)}</span>
            <span class="cell-sub mono">@${escapeHtml(u.username)}</span>
          </div>
        </div>
      </td>
      <td>${u.role === 'ADMIN' ? badge('Admin', 'info') : badge('HR', 'neutral')}</td>
      <td>${escapeHtml(u.email || '—')}</td>
      <td>${u.enabled ? badge('Active', 'success') : badge('Disabled', 'danger')}</td>
      <td>
        <div class="cell-actions">
          ${u.role === 'ADMIN' ? '' : `
            <button class="icon-btn" type="button" title="Reset password" aria-label="Reset password"
                    data-action="reset" data-testid="reset-account-btn">${icon('key', 15)}</button>
            <button class="btn btn-secondary btn-sm" type="button" data-action="toggle" data-testid="toggle-account-btn">
              ${u.enabled ? 'Disable' : 'Enable'}
            </button>`}
        </div>
      </td>
    </tr>`).join('');

  wrap.innerHTML = `
    <div class="card">
      <div class="table-scroll">
        <table class="data-table" data-testid="accounts-table">
          <thead><tr><th>Account</th><th>Role</th><th>Email</th><th>Status</th><th></th></tr></thead>
          <tbody>${rows}</tbody>
        </table>
      </div>
    </div>`;

  wrap.querySelectorAll('[data-action="reset"]').forEach((btn) => {
    btn.addEventListener('click', () => resetPassword(Number(btn.closest('tr').dataset.accountId)));
  });
  wrap.querySelectorAll('[data-action="toggle"]').forEach((btn) => {
    btn.addEventListener('click', () => toggle(Number(btn.closest('tr').dataset.accountId)));
  });
}

async function resetPassword(id) {
  const account = accounts.find((a) => a.id === id);
  const confirmed = await confirmDialog({
    title: 'Reset password',
    message: `Generate a new password for ${account.fullName}? Their current password will stop working.`,
    confirmLabel: 'Reset password',
  });
  if (!confirmed) return;
  try {
    const res = await api(`/users/${id}/reset-password`, { method: 'POST' });
    credentialsModal({ title: 'New password', sub: account.fullName, username: res.username, password: res.password, message: res.message });
    await load();
  } catch (error) {
    toast(error.message, 'error');
  }
}

async function toggle(id) {
  try {
    const res = await api(`/users/${id}/toggle`, { method: 'PUT' });
    toast(`${res.fullName} ${res.enabled ? 'enabled' : 'disabled'}`, res.enabled ? 'success' : 'info');
    await load();
  } catch (error) {
    toast(error.message, 'error');
  }
}

function openCreator() {
  openModal({
    title: 'Create HR account',
    sub: 'A username and password are generated automatically',
    body: `
      <form id="hr-form" data-testid="hr-form">
        <div class="field">
          <label class="label" for="hr-name">Full name</label>
          <input class="input" id="hr-name" name="fullName" required maxlength="100" placeholder="e.g. Jordan Blake">
        </div>
        <div class="field">
          <label class="label" for="hr-email">Email (optional)</label>
          <input class="input" id="hr-email" name="email" type="email" maxlength="160" placeholder="jordan.blake@staffhub.io">
        </div>
        <p class="muted" style="font-size:12.5px;">A username and a secure password will be generated and shown once after creating.</p>
        <p class="form-error" id="hr-form-error" hidden></p>
      </form>`,
    footer: `
      <button class="btn btn-secondary" type="button" data-cancel>Cancel</button>
      <button class="btn btn-primary" type="submit" form="hr-form" data-testid="save-hr-btn">Create account</button>`,
    onMount(overlay, close) {
      overlay.querySelector('[data-cancel]').addEventListener('click', close);
      const form = overlay.querySelector('#hr-form');
      const errorBox = overlay.querySelector('#hr-form-error');
      form.addEventListener('submit', async (event) => {
        event.preventDefault();
        errorBox.hidden = true;
        try {
          const res = await api('/users/hr', {
            method: 'POST',
            body: { fullName: form.fullName.value.trim(), email: form.email.value.trim() },
          });
          close();
          credentialsModal({ title: 'HR account created', sub: res.fullName, username: res.username, password: res.password, message: res.message });
          await load();
        } catch (error) {
          errorBox.textContent = error.message;
          errorBox.hidden = false;
        }
      });
    },
  });
}
