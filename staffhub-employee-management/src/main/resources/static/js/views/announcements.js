import { api } from '../api.js';
import { confirmDialog, dateTime, escapeHtml, icon, openModal, toast } from '../ui.js';

let rootEl = null;
let staff = false;
let items = [];

export async function render(root, { me }) {
  rootEl = root;
  staff = me.role === 'ADMIN' || me.role === 'HR';

  root.innerHTML = `
    <div class="toolbar">
      <div class="filters"><span class="muted" style="font-size:13px;">Notices are visible to everyone in the company.</span></div>
      ${staff ? `<button class="btn btn-primary" type="button" id="add-ann-btn" data-testid="add-announcement-btn">${icon('plus', 16)} Post Announcement</button>` : ''}
    </div>
    <div id="ann-wrap" data-testid="announcements-wrap"></div>`;

  const addBtn = root.querySelector('#add-ann-btn');
  if (addBtn) addBtn.addEventListener('click', openCreator);

  await load();
}

async function load() {
  items = await api('/announcements');
  const wrap = rootEl.querySelector('#ann-wrap');

  if (items.length === 0) {
    wrap.innerHTML = `
      <div class="card"><div class="empty-state" data-testid="announcements-empty-state">
        <span class="empty-icon">${icon('megaphone', 22)}</span>
        <h3>No announcements yet</h3>
        <p>${staff ? 'Post the first notice to keep everyone in the loop.' : 'Company notices will appear here.'}</p>
      </div></div>`;
    return;
  }

  wrap.innerHTML = `<div class="ann-grid">${items.map(cardHtml).join('')}</div>`;
  wrap.querySelectorAll('[data-del]').forEach((btn) => {
    btn.addEventListener('click', () => remove(Number(btn.dataset.del)));
  });
}

function cardHtml(a) {
  return `
    <div class="ann-card" data-testid="announcement-card">
      <div class="ann-top">
        <span class="ann-icon">${icon('megaphone', 18)}</span>
        <div style="flex:1;">
          <div class="ann-title">${escapeHtml(a.title)}</div>
          <p class="ann-body">${escapeHtml(a.body)}</p>
          <div class="ann-meta">${icon('users', 12)} ${escapeHtml(a.authorName)} · ${dateTime(a.createdAt)}</div>
        </div>
        ${staff ? `<button class="icon-btn danger" type="button" title="Delete announcement" aria-label="Delete announcement" data-del="${a.id}" data-testid="delete-announcement-btn">${icon('trash', 15)}</button>` : ''}
      </div>
    </div>`;
}

async function remove(id) {
  const a = items.find((item) => item.id === id);
  const confirmed = await confirmDialog({
    title: 'Delete announcement', message: `Delete "${a.title}"?`, confirmLabel: 'Delete', danger: true,
  });
  if (!confirmed) return;
  try {
    await api('/announcements/' + id, { method: 'DELETE' });
    toast('Announcement deleted');
    await load();
  } catch (error) {
    toast(error.message, 'error');
  }
}

function openCreator() {
  openModal({
    title: 'Post announcement',
    sub: 'Everyone in the company will see this',
    body: `
      <form id="ann-form" data-testid="announcement-form">
        <div class="field">
          <label class="label" for="ann-title">Title</label>
          <input class="input" id="ann-title" name="title" required maxlength="200" placeholder="e.g. Office closed on Friday">
        </div>
        <div class="field">
          <label class="label" for="ann-body">Message</label>
          <textarea class="textarea" id="ann-body" name="body" required maxlength="2000" rows="5" placeholder="Share the details…"></textarea>
        </div>
        <p class="form-error" id="ann-form-error" hidden></p>
      </form>`,
    footer: `
      <button class="btn btn-secondary" type="button" data-cancel>Cancel</button>
      <button class="btn btn-primary" type="submit" form="ann-form" data-testid="save-announcement-btn">Post</button>`,
    onMount(overlay, close) {
      overlay.querySelector('[data-cancel]').addEventListener('click', close);
      const form = overlay.querySelector('#ann-form');
      const errorBox = overlay.querySelector('#ann-form-error');
      form.addEventListener('submit', async (event) => {
        event.preventDefault();
        errorBox.hidden = true;
        try {
          await api('/announcements', { method: 'POST', body: { title: form.title.value.trim(), body: form.body.value.trim() } });
          toast('Announcement posted');
          close();
          await load();
        } catch (error) {
          errorBox.textContent = error.message;
          errorBox.hidden = false;
        }
      });
    },
  });
}
