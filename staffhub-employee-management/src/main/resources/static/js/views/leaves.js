import { api } from '../api.js';
import { badge, date, dateTime, escapeHtml, icon, initials, openModal, toast } from '../ui.js';

const state = { status: '', leaves: [], employees: [] };
let rootEl = null;
let meRef = null;
let staff = false;
let balance = null;

const TABS = [
  { key: '', label: 'All', testid: 'leave-tab-all' },
  { key: 'PENDING', label: 'Pending', testid: 'leave-tab-pending' },
  { key: 'APPROVED', label: 'Approved', testid: 'leave-tab-approved' },
  { key: 'REJECTED', label: 'Rejected', testid: 'leave-tab-rejected' },
];

export async function render(root, { me }) {
  rootEl = root;
  meRef = me;
  staff = me.role === 'ADMIN' || me.role === 'HR';
  state.status = '';

  if (staff) {
    state.employees = await api('/employees');
  }

  root.innerHTML = `
    <div class="toolbar">
      <div class="tabs" id="leave-tabs" data-testid="leave-tabs">
        ${TABS.map((tab) => `
          <button class="tab ${tab.key === '' ? 'active' : ''}" type="button" data-status="${tab.key}" data-testid="${tab.testid}">
            ${tab.label} <span class="count" data-count="${tab.key || 'ALL'}">0</span>
          </button>`).join('')}
      </div>
      <button class="btn btn-primary" type="button" id="add-leave-btn" data-testid="add-leave-btn">
        ${icon('plus', 16)} ${staff ? 'New Leave Request' : 'Apply for Leave'}
      </button>
    </div>
    ${staff ? '' : '<div id="leave-balance" data-testid="leave-balance"></div>'}
    <div id="leaves-wrap" data-testid="leaves-table-wrap"></div>`;

  root.querySelectorAll('#leave-tabs .tab').forEach((tab) => {
    tab.addEventListener('click', () => {
      state.status = tab.dataset.status;
      root.querySelectorAll('#leave-tabs .tab').forEach((item) => item.classList.toggle('active', item === tab));
      renderTable();
    });
  });
  root.querySelector('#add-leave-btn').addEventListener('click', () => openCreator());

  await loadLeaves();
}

async function loadLeaves() {
  state.leaves = await api('/leaves');

  const counts = { ALL: state.leaves.length, PENDING: 0, APPROVED: 0, REJECTED: 0 };
  state.leaves.forEach((leave) => { counts[leave.status] += 1; });
  rootEl.querySelectorAll('#leave-tabs [data-count]').forEach((node) => {
    node.textContent = counts[node.dataset.count] ?? 0;
  });

  if (!staff) {
    balance = await api('/leaves/balance');
    renderBalance();
  }

  renderTable();
}

function renderBalance() {
  const el = rootEl.querySelector('#leave-balance');
  if (!el || !balance) return;
  const chip = (label, value) => `<span class="chip">${label} <span class="chip-value">${value}</span></span>`;
  el.innerHTML = `
    <div class="card" style="padding: 14px 16px; margin-bottom: 14px;">
      <div class="summary-chips" data-testid="leave-balance-chips">
        ${chip('Annual quota', balance.quota + ' days')}
        ${chip('Used', balance.used)}
        ${chip('Pending', balance.pending)}
        ${chip('Remaining', balance.remaining + ' days')}
      </div>
    </div>`;
}

function renderTable() {
  const wrap = rootEl.querySelector('#leaves-wrap');
  const visible = state.status === '' ? state.leaves : state.leaves.filter((leave) => leave.status === state.status);

  if (visible.length === 0) {
    wrap.innerHTML = `
      <div class="empty-state" data-testid="leaves-empty-state">
        <span class="empty-icon">${icon('plane', 22)}</span>
        <h3>No ${state.status === '' ? '' : state.status.toLowerCase() + ' '}leave requests</h3>
        <p>${staff ? 'Requests raised by employees will land here for approval.' : 'Apply for time off and track its status here.'}</p>
      </div>`;
    return;
  }

  const rows = visible.map((leave) => {
    const actions = staff && leave.status === 'PENDING'
      ? `
        <button class="icon-btn approve" type="button" title="Approve" aria-label="Approve leave request"
                data-action="approve" data-leave-id="${leave.id}" data-testid="leave-approve-btn">${icon('check', 15)}</button>
        <button class="icon-btn danger" type="button" title="Reject" aria-label="Reject leave request"
                data-action="reject" data-leave-id="${leave.id}" data-testid="leave-reject-btn">${icon('x', 15)}</button>`
      : `<span class="muted" style="font-size: 12px;">—</span>`;

    return `
      <tr data-testid="leave-row">
        <td>
          <div class="employee-cell">
            <span class="avatar">${escapeHtml(initials(leave.employeeName))}</span>
            <div>
              <span class="name">${escapeHtml(leave.employeeName)}</span>
              <span class="cell-sub">${escapeHtml(leave.departmentName)}</span>
            </div>
          </div>
        </td>
        <td>${date(leave.startDate)} → ${date(leave.endDate)}<span class="cell-sub">${leave.days} day${leave.days === 1 ? '' : 's'}</span></td>
        <td style="max-width: 280px;">${escapeHtml(leave.reason)}</td>
        <td>${dateTime(leave.appliedAt)}</td>
        <td>${leave.status === 'PENDING' ? badge('Pending', 'warning') : leave.status === 'APPROVED' ? badge('Approved', 'success') : badge('Rejected', 'danger')}</td>
        <td><div class="cell-actions">${actions}</div></td>
      </tr>`;
  }).join('');

  wrap.innerHTML = `
    <div class="card">
      <div class="table-scroll">
        <table class="data-table" data-testid="leaves-table">
          <thead>
            <tr><th>Employee</th><th>Dates</th><th>Reason</th><th>Applied</th><th>Status</th><th></th></tr>
          </thead>
          <tbody>${rows}</tbody>
        </table>
      </div>
    </div>`;

  wrap.querySelectorAll('[data-action="approve"]').forEach((button) => {
    button.addEventListener('click', () => updateStatus(Number(button.dataset.leaveId), 'APPROVED'));
  });
  wrap.querySelectorAll('[data-action="reject"]').forEach((button) => {
    button.addEventListener('click', () => updateStatus(Number(button.dataset.leaveId), 'REJECTED'));
  });
}

async function updateStatus(id, status) {
  const leave = state.leaves.find((item) => item.id === id);
  try {
    await api(`/leaves/${id}/status`, { method: 'PUT', body: { status } });
    toast(`${leave.employeeName}'s leave ${status.toLowerCase()}`, status === 'APPROVED' ? 'success' : 'info');
    await loadLeaves();
  } catch (error) {
    toast(error.message, 'error');
  }
}

function openCreator() {
  const employeeField = staff ? `
    <div class="field">
      <label class="label" for="leave-employee">Employee</label>
      <select class="select" id="leave-employee" name="employeeId" required data-testid="leave-employee-select">
        <option value="" disabled selected>Select employee…</option>
        ${state.employees.map((employee) => `<option value="${employee.id}">${escapeHtml(employee.firstName)} ${escapeHtml(employee.lastName)} — ${escapeHtml(employee.departmentName)}</option>`).join('')}
      </select>
    </div>` : '';

  openModal({
    title: staff ? 'New leave request' : 'Apply for leave',
    sub: staff ? 'Raise time-off on behalf of an employee' : 'Request time off from work',
    body: `
      <form id="leave-form" data-testid="leave-form">
        ${employeeField}
        <div class="form-grid">
          <div class="field">
            <label class="label" for="leave-start">First day</label>
            <input class="input" id="leave-start" name="startDate" type="date" required>
          </div>
          <div class="field">
            <label class="label" for="leave-end">Last day</label>
            <input class="input" id="leave-end" name="endDate" type="date" required>
          </div>
        </div>
        <div class="field">
          <label class="label" for="leave-reason">Reason</label>
          <textarea class="textarea" id="leave-reason" name="reason" required maxlength="500" placeholder="Why is the time off needed?"></textarea>
        </div>
        <p class="form-error" id="leave-form-error" hidden></p>
        ${staff ? '' : `<p class="muted" style="font-size:12.5px;">You have <strong>${balance ? balance.remaining : '—'}</strong> of ${balance ? balance.quota : '—'} leave days remaining this year.</p>`}
      </form>`,
    footer: `
      <button class="btn btn-secondary" type="button" data-cancel>Cancel</button>
      <button class="btn btn-primary" type="submit" form="leave-form" data-testid="save-leave-btn">Submit request</button>`,
    onMount(overlay, close) {
      overlay.querySelector('[data-cancel]').addEventListener('click', close);
      const form = overlay.querySelector('#leave-form');
      const errorBox = overlay.querySelector('#leave-form-error');
      form.addEventListener('submit', async (event) => {
        event.preventDefault();
        errorBox.hidden = true;

        const employeeId = staff ? Number(form.employeeId.value) : meRef.employeeId;
        const startDate = form.startDate.value;
        const endDate = form.endDate.value;
        if (endDate < startDate) {
          errorBox.textContent = 'The last day cannot be before the first day.';
          errorBox.hidden = false;
          return;
        }
        if (!staff && balance) {
          const requested = Math.round((new Date(endDate) - new Date(startDate)) / 86400000) + 1;
          if (requested > balance.remaining) {
            errorBox.textContent = `This request (${requested} day${requested === 1 ? '' : 's'}) exceeds your remaining balance of ${balance.remaining} day${balance.remaining === 1 ? '' : 's'} this year.`;
            errorBox.hidden = false;
            return;
          }
        }

        try {
          await api('/leaves', {
            method: 'POST',
            body: { employeeId, startDate, endDate, reason: form.reason.value.trim() },
          });
          toast(staff ? 'Leave request created' : 'Leave request submitted');
          close();
          await loadLeaves();
        } catch (error) {
          errorBox.textContent = error.message;
          errorBox.hidden = false;
        }
      });
    },
  });
}
