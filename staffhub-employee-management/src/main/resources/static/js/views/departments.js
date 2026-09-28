import { api } from '../api.js';
import { badge, confirmDialog, escapeHtml, icon, openModal, toast } from '../ui.js';

let rootEl = null;

export async function render(root) {
  rootEl = root;

  root.innerHTML = `
    <div class="toolbar">
      <div class="filters">
        <span class="muted" style="font-size: 13px;">Teams and their current headcount.</span>
      </div>
      <button class="btn btn-primary" type="button" id="add-department-btn" data-testid="add-department-btn">
        ${icon('plus', 16)} Add Department
      </button>
    </div>
    <div id="departments-wrap" data-testid="departments-grid"></div>`;

  root.querySelector('#add-department-btn').addEventListener('click', () => openEditor(null));
  await loadGrid();
}

async function loadGrid() {
  const departments = await api('/departments');
  const wrap = rootEl.querySelector('#departments-wrap');

  if (departments.length === 0) {
    wrap.innerHTML = `
      <div class="empty-state" data-testid="departments-empty-state">
        <span class="empty-icon">${icon('building', 22)}</span>
        <h3>No departments yet</h3>
        <p>Create your first department to organize employees into teams.</p>
        <button class="btn btn-primary" type="button" id="empty-add-department">${icon('plus', 16)} Add Department</button>
      </div>`;
    wrap.querySelector('#empty-add-department').addEventListener('click', () => openEditor(null));
    return;
  }

  wrap.innerHTML = `
    <div class="dept-grid">
      ${departments.map((department) => `
        <div class="dept-card" data-testid="department-card" data-department-id="${department.id}">
          <div class="dept-top">
            <span class="dept-name">${escapeHtml(department.name)}</span>
            ${badge(`${department.employeeCount} ${department.employeeCount === 1 ? 'person' : 'people'}`, department.employeeCount === 0 ? 'neutral' : 'info')}
          </div>
          <p class="dept-desc">${escapeHtml(department.description || 'No description yet.')}</p>
          <div class="dept-footer">
            <span class="muted" style="font-size: 12px;">ID ${department.id}</span>
            <div class="cell-actions">
              <button class="icon-btn" type="button" title="Edit department" aria-label="Edit department"
                      data-action="edit" data-testid="edit-department-btn">${icon('pencil', 15)}</button>
              <button class="icon-btn danger" type="button" title="Delete department" aria-label="Delete department"
                      data-action="delete" data-testid="delete-department-btn">${icon('trash', 15)}</button>
            </div>
          </div>
        </div>`).join('')}
    </div>`;

  wrap.querySelectorAll('.dept-card [data-action="edit"]').forEach((button) => {
    button.addEventListener('click', () => {
      const card = button.closest('.dept-card');
      const department = departments.find((item) => item.id === Number(card.dataset.departmentId));
      if (department) openEditor(department);
    });
  });
  wrap.querySelectorAll('.dept-card [data-action="delete"]').forEach((button) => {
    button.addEventListener('click', async () => {
      const card = button.closest('.dept-card');
      const department = departments.find((item) => item.id === Number(card.dataset.departmentId));
      const confirmed = await confirmDialog({
        title: 'Delete department',
        message: `Delete ${department.name}? Departments with employees assigned cannot be deleted.`,
        confirmLabel: 'Delete',
        danger: true,
      });
      if (!confirmed) return;
      try {
        await api('/departments/' + department.id, { method: 'DELETE' });
        toast(`Deleted ${department.name}`);
        await loadGrid();
      } catch (error) {
        toast(error.message, 'error');
      }
    });
  });
}

function openEditor(department) {
  const isEdit = department !== null;

  openModal({
    title: isEdit ? 'Edit department' : 'Add department',
    size: 'narrow',
    body: `
      <form id="department-form" data-testid="department-form">
        <div class="field">
          <label class="label" for="dept-name">Name</label>
          <input class="input" id="dept-name" name="name" required maxlength="100"
                 value="${isEdit ? escapeHtml(department.name) : ''}" placeholder="Engineering">
        </div>
        <div class="field">
          <label class="label" for="dept-description">Description</label>
          <textarea class="textarea" id="dept-description" name="description" maxlength="500"
                    placeholder="What this team owns…">${isEdit ? escapeHtml(department.description || '') : ''}</textarea>
        </div>
        <p class="form-error" id="department-form-error" hidden></p>
      </form>`,
    footer: `
      <button class="btn btn-secondary" type="button" data-cancel>Cancel</button>
      <button class="btn btn-primary" type="submit" form="department-form" data-testid="save-department-btn">
        ${isEdit ? 'Save changes' : 'Add department'}
      </button>`,
    onMount(overlay, close) {
      overlay.querySelector('[data-cancel]').addEventListener('click', close);
      const form = overlay.querySelector('#department-form');
      const errorBox = overlay.querySelector('#department-form-error');
      form.addEventListener('submit', async (event) => {
        event.preventDefault();
        errorBox.hidden = true;
        const payload = { name: form.name.value.trim(), description: form.description.value.trim() };
        try {
          if (isEdit) {
            await api('/departments/' + department.id, { method: 'PUT', body: payload });
            toast('Updated ' + payload.name);
          } else {
            await api('/departments', { method: 'POST', body: payload });
            toast('Created ' + payload.name);
          }
          close();
          await loadGrid();
        } catch (error) {
          errorBox.textContent = error.message;
          errorBox.hidden = false;
        }
      });
    },
  });
}
