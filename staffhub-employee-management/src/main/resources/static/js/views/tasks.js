import { api } from '../api.js';
import { badge, confirmDialog, date, escapeHtml, icon, openModal, toast } from '../ui.js';

const COLUMNS = [
  { key: 'TODO', label: 'To do' },
  { key: 'IN_PROGRESS', label: 'In progress' },
  { key: 'DONE', label: 'Done' },
];

let rootEl = null;
let ctx = { me: null, staff: false };
let employeesCache = [];
const state = { employeeId: '', tasks: [] };

export async function render(root, { me }) {
  rootEl = root;
  ctx.me = me;
  ctx.staff = me.role === 'ADMIN' || me.role === 'HR';
  state.employeeId = '';

  if (ctx.staff) {
    employeesCache = await api('/employees');
  }

  const filter = ctx.staff ? `
    <select class="select" id="task-emp-filter" style="width: 220px;" data-testid="task-employee-filter">
      <option value="">All employees</option>
      ${employeesCache.map((e) => `<option value="${e.id}">${escapeHtml(e.firstName)} ${escapeHtml(e.lastName)}</option>`).join('')}
    </select>` : '<span class="muted" style="font-size:13px;">Move cards across columns as you make progress.</span>';

  root.innerHTML = `
    <div class="toolbar">
      <div class="filters">${filter}</div>
      <button class="btn btn-primary" type="button" id="add-task-btn" data-testid="add-task-btn">
        ${icon('plus', 16)} ${ctx.staff ? 'Assign Task' : 'Add Task'}
      </button>
    </div>
    <div id="task-board" data-testid="task-board"></div>`;

  root.querySelector('#add-task-btn').addEventListener('click', () => openCreator());
  const filterEl = root.querySelector('#task-emp-filter');
  if (filterEl) filterEl.addEventListener('change', (event) => { state.employeeId = event.target.value; loadTasks(); });

  await loadTasks();
}

async function loadTasks() {
  const params = new URLSearchParams();
  if (ctx.staff && state.employeeId) params.set('employeeId', state.employeeId);
  state.tasks = await api('/tasks' + (params.toString() ? '?' + params.toString() : ''));
  renderBoard();
}

function renderBoard() {
  const board = rootEl.querySelector('#task-board');
  board.className = 'task-board';
  board.innerHTML = COLUMNS.map((col) => {
    const items = state.tasks.filter((t) => t.status === col.key);
    return `
      <div class="task-col" data-testid="task-col-${col.key}">
        <div class="task-col-head">
          <span>${col.label}</span>
          <span class="task-col-count">${items.length}</span>
        </div>
        ${items.length === 0 ? '<div class="task-empty">Nothing here.</div>' : items.map(card).join('')}
      </div>`;
  }).join('');

  board.querySelectorAll('[data-move]').forEach((btn) => {
    btn.addEventListener('click', () => move(Number(btn.dataset.id), btn.dataset.move));
  });
  board.querySelectorAll('[data-del]').forEach((btn) => {
    btn.addEventListener('click', () => removeTask(Number(btn.dataset.del)));
  });
}

function card(t) {
  const prevKey = t.status === 'DONE' ? 'IN_PROGRESS' : (t.status === 'IN_PROGRESS' ? 'TODO' : null);
  const nextKey = t.status === 'TODO' ? 'IN_PROGRESS' : (t.status === 'IN_PROGRESS' ? 'DONE' : null);
  const controls = `
    ${prevKey ? `<button class="icon-btn" type="button" title="Move back" data-move="${prevKey}" data-id="${t.id}" data-testid="task-back-btn">${icon('arrowRight', 14)}<span class="sr-only">back</span></button>` : ''}
    ${nextKey ? `<button class="btn btn-secondary btn-sm" type="button" data-move="${nextKey}" data-id="${t.id}" data-testid="task-advance-btn">${nextKey === 'DONE' ? 'Mark done' : 'Start'}</button>` : `<span class="badge badge-success">Completed</span>`}`;

  const canDelete = ctx.staff || t.selfCreated;
  return `
    <div class="task-card pri-${escapeHtml(t.priority)}" data-testid="task-card">
      <div class="task-card-title">${escapeHtml(t.title)}</div>
      ${t.description ? `<div class="task-card-desc">${escapeHtml(t.description)}</div>` : ''}
      <div class="task-card-meta">
        ${priorityBadge(t.priority)}
        ${t.dueDate ? `<span>${icon('calendarCheck', 12)} ${date(t.dueDate)}</span>` : ''}
        ${ctx.staff ? `<span>${icon('users', 12)} ${escapeHtml(t.employeeName)}</span>` : ''}
        ${t.selfCreated ? '<span class="badge badge-neutral">Personal</span>' : ''}
      </div>
      <div class="task-card-foot">
        <div class="cell-actions">${controls}</div>
        ${canDelete ? `<button class="icon-btn danger" type="button" title="Delete task" aria-label="Delete task" data-del="${t.id}" data-testid="task-delete-btn">${icon('trash', 14)}</button>` : ''}
      </div>
    </div>`;
}

async function move(id, status) {
  try {
    await api(`/tasks/${id}/status`, { method: 'PUT', body: { status } });
    await loadTasks();
  } catch (error) {
    toast(error.message, 'error');
  }
}

async function removeTask(id) {
  const task = state.tasks.find((t) => t.id === id);
  const confirmed = await confirmDialog({
    title: 'Delete task', message: `Delete "${task.title}"? This cannot be undone.`,
    confirmLabel: 'Delete', danger: true,
  });
  if (!confirmed) return;
  try {
    await api('/tasks/' + id, { method: 'DELETE' });
    toast('Task deleted');
    await loadTasks();
  } catch (error) {
    toast(error.message, 'error');
  }
}

function openCreator() {
  const employeeField = ctx.staff ? `
    <div class="field">
      <label class="label" for="task-employee">Assign to</label>
      <select class="select" id="task-employee" name="employeeId" required data-testid="task-employee-select">
        <option value="" disabled selected>Select employee…</option>
        ${employeesCache.map((e) => `<option value="${e.id}">${escapeHtml(e.firstName)} ${escapeHtml(e.lastName)} — ${escapeHtml(e.departmentName)}</option>`).join('')}
      </select>
    </div>` : '';

  openModal({
    title: ctx.staff ? 'Assign task' : 'Add task',
    sub: ctx.staff ? 'Give an employee something to work on' : 'Add something to your list',
    body: `
      <form id="task-form" data-testid="task-form">
        ${employeeField}
        <div class="field">
          <label class="label" for="task-title">Title</label>
          <input class="input" id="task-title" name="title" required maxlength="200" placeholder="e.g. Prepare monthly report">
        </div>
        <div class="field">
          <label class="label" for="task-desc">Description</label>
          <textarea class="textarea" id="task-desc" name="description" maxlength="1000" placeholder="Optional details…"></textarea>
        </div>
        <div class="form-grid">
          <div class="field">
            <label class="label" for="task-priority">Priority</label>
            <select class="select" id="task-priority" name="priority" data-testid="task-priority-select">
              <option value="LOW">Low</option>
              <option value="MEDIUM" selected>Medium</option>
              <option value="HIGH">High</option>
            </select>
          </div>
          <div class="field">
            <label class="label" for="task-due">Due date</label>
            <input class="input" id="task-due" name="dueDate" type="date">
          </div>
        </div>
        <p class="form-error" id="task-form-error" hidden></p>
      </form>`,
    footer: `
      <button class="btn btn-secondary" type="button" data-cancel>Cancel</button>
      <button class="btn btn-primary" type="submit" form="task-form" data-testid="save-task-btn">${ctx.staff ? 'Assign task' : 'Add task'}</button>`,
    onMount(overlay, close) {
      overlay.querySelector('[data-cancel]').addEventListener('click', close);
      const form = overlay.querySelector('#task-form');
      const errorBox = overlay.querySelector('#task-form-error');
      form.addEventListener('submit', async (event) => {
        event.preventDefault();
        errorBox.hidden = true;
        const payload = {
          title: form.title.value.trim(),
          description: form.description.value.trim(),
          priority: form.priority.value,
          dueDate: form.dueDate.value || null,
        };
        if (ctx.staff) payload.employeeId = Number(form.employeeId.value);
        try {
          await api('/tasks', { method: 'POST', body: payload });
          toast(ctx.staff ? 'Task assigned' : 'Task added');
          close();
          await loadTasks();
        } catch (error) {
          errorBox.textContent = error.message;
          errorBox.hidden = false;
        }
      });
    },
  });
}

function priorityBadge(priority) {
  const map = { HIGH: 'danger', MEDIUM: 'warning', LOW: 'success' };
  return badge(priority.charAt(0) + priority.slice(1).toLowerCase(), map[priority] || 'neutral');
}
