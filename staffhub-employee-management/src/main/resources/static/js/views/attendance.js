import { api } from '../api.js';
import { badge, date, escapeHtml, icon, initials, time, toast } from '../ui.js';

const state = { date: '', employees: [], records: [] };
let rootEl = null;
let staff = false;

export async function render(root, { me }) {
  rootEl = root;
  staff = me.role === 'ADMIN' || me.role === 'HR';
  if (!staff) {
    return renderEmployee(root);
  }

  const [day, employees] = await Promise.all([api('/attendance'), api('/employees')]);
  state.date = day.date;
  state.employees = employees;
  state.records = day.records;

  const todayIso = new Date().toISOString().slice(0, 10);

  root.innerHTML = `
    <div class="card">
      <div class="card-header" style="flex-wrap: wrap;">
        <div>
          <h3>Daily log</h3>
          <p class="card-sub">One check-in per employee per day, recorded server-side.</p>
        </div>
        <div class="summary-chips">
          <span class="chip">Present <span class="chip-value" id="chip-present" data-testid="attendance-present-count">${day.presentCount}</span></span>
          <span class="chip">On the clock <span class="chip-value" id="chip-working">${day.presentCount - day.checkedOutCount}</span></span>
          <span class="chip">Checked out <span class="chip-value" id="chip-out">${day.checkedOutCount}</span></span>
        </div>
      </div>

      <div class="card-body" style="padding: 20px; border-bottom: 1px solid var(--border); display: flex; flex-direction: column; gap: 14px;">
        <div class="check-panel">
          <div class="field">
            <label class="label" for="attendance-date">Date</label>
            <input class="input" type="date" id="attendance-date" value="${escapeHtml(state.date)}" style="width: 180px;" data-testid="attendance-date-input">
          </div>
          <div class="field" style="flex: 1;">
            <label class="label" for="attendance-employee">Employee</label>
            <select class="select" id="attendance-employee" data-testid="attendance-employee-select">
              <option value="" selected>Select an employee…</option>
              ${employees.map((employee) => `<option value="${employee.id}">${escapeHtml(employee.firstName)} ${escapeHtml(employee.lastName)} — ${escapeHtml(employee.departmentName)}</option>`).join('')}
            </select>
          </div>
          <div class="field">
            <label class="label">&nbsp;</label>
            <span id="attendance-action-slot"></span>
          </div>
        </div>
        <p class="muted" id="attendance-hint" style="font-size: 12.5px;">
          ${state.date === todayIso
            ? 'Pick an employee, then check them in or out. The action adapts to their day.'
            : 'You are viewing a past day — switch back to today to record attendance.'}
        </p>
      </div>

      <div id="attendance-table-wrap" data-testid="attendance-table-wrap"></div>
    </div>`;

  root.querySelector('#attendance-date').addEventListener('change', (event) => {
    state.date = event.target.value;
    loadDay();
  });
  root.querySelector('#attendance-employee').addEventListener('change', () => renderActionButton());
  renderActionButton();
  renderTable();
}

async function loadDay() {
  const day = await api('/attendance?date=' + encodeURIComponent(state.date));
  state.records = day.records;
  document.getElementById('chip-present').textContent = day.presentCount;
  document.getElementById('chip-working').textContent = day.presentCount - day.checkedOutCount;
  document.getElementById('chip-out').textContent = day.checkedOutCount;
  renderActionButton();
  renderTable();
}

function recordForSelectedEmployee() {
  const select = rootEl.querySelector('#attendance-employee');
  if (!select || !select.value) return null;
  const employeeId = Number(select.value);
  return state.records.find((record) => record.employeeId === employeeId) || null;
}

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

function renderActionButton() {
  const slot = rootEl.querySelector('#attendance-action-slot');
  if (!slot) return;

  const isToday = state.date === todayIso();
  const record = recordForSelectedEmployee();
  const select = rootEl.querySelector('#attendance-employee');
  const disabled = !select.value;

  if (!isToday) {
    slot.innerHTML = '<button class="btn btn-secondary" type="button" disabled>Viewing past day</button>';
    return;
  }
  if (disabled) {
    slot.innerHTML = '<button class="btn btn-primary" type="button" disabled>Select employee</button>';
    return;
  }
  if (!record) {
    slot.innerHTML = `<button class="btn btn-primary" type="button" id="attendance-action-btn" data-testid="attendance-checkin-btn">Check in</button>`;
  } else if (!record.checkOut) {
    slot.innerHTML = `<button class="btn btn-danger" type="button" id="attendance-action-btn" data-testid="attendance-checkout-btn">Check out</button>`;
  } else {
    slot.innerHTML = '<button class="btn btn-secondary" type="button" disabled>Day complete</button>';
  }

  const button = slot.querySelector('#attendance-action-btn');
  if (button) button.addEventListener('click', onActionClick);
}

async function onActionClick() {
  const select = rootEl.querySelector('#attendance-employee');
  const employeeId = Number(select.value);
  const record = recordForSelectedEmployee();
  const action = record ? '/attendance/check-out' : '/attendance/check-in';
  const employee = state.employees.find((item) => item.id === employeeId);

  try {
    const response = await api(action, { method: 'POST', body: { employeeId } });
    toast(record
      ? `Checked out ${employee.firstName} at ${time(response.checkOut)}`
      : `Checked in ${employee.firstName} at ${time(response.checkIn)}`);
    await loadDay();
  } catch (error) {
    toast(error.message, 'error');
  }
}

function renderTable() {
  const wrap = rootEl.querySelector('#attendance-table-wrap');
  if (state.records.length === 0) {
    wrap.innerHTML = `
      <div class="empty-state" style="border: none; border-radius: 0;" data-testid="attendance-empty-state">
        <span class="empty-icon">${icon('clock', 22)}</span>
        <h3>No attendance recorded</h3>
        <p>Nobody has checked in on ${date(state.date)} yet.</p>
      </div>`;
    return;
  }

  wrap.innerHTML = attendanceTable(state.records, 'staff');
}

function attendanceTable(records, mode) {
  const rows = records.map((record) => {
    const duration = record.checkIn && record.checkOut ? durationLabel(record.checkIn, record.checkOut) : null;
    const subtitle = mode === 'self' ? date(record.date) : record.departmentName;
    return `
      <tr data-testid="attendance-row">
        <td>
          <div class="employee-cell">
            <span class="avatar">${escapeHtml(initials(record.employeeName))}</span>
            <div>
              <span class="name">${escapeHtml(record.employeeName)}</span>
              <span class="cell-sub">${escapeHtml(subtitle || '')}</span>
            </div>
          </div>
        </td>
        <td><span class="time-chip">${icon('clock', 13)} ${time(record.checkIn)}</span></td>
        <td><span class="time-chip">${record.checkOut ? icon('check', 13) + ' ' + time(record.checkOut) : '—'}</span></td>
        <td class="mono">${duration ? escapeHtml(duration) : '—'}</td>
        <td>${record.checkOut ? badge('Completed', 'success') : badge('Working', 'info')}</td>
      </tr>`;
  }).join('');

  return `
    <div class="table-scroll">
      <table class="data-table" data-testid="attendance-table">
        <thead>
          <tr><th>Employee</th><th>Check-in</th><th>Check-out</th><th>Worked</th><th>Status</th></tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>
    </div>`;
}

/* -------------------------------------------------------- employee view */
async function renderEmployee(root) {
  const data = await api('/attendance/mine');
  const today = data.today;
  const checkedIn = !!today;
  const checkedOut = !!(today && today.checkOut);
  const actionLabel = !checkedIn ? 'Check in' : (!checkedOut ? 'Check out' : 'Day complete');
  const actionClass = !checkedIn ? '' : (!checkedOut ? 'checkout' : '');
  const status = !checkedIn
    ? 'You have not checked in today.'
    : (!checkedOut ? `On the clock since ${time(today.checkIn)}.` : `Checked in ${time(today.checkIn)} · out ${time(today.checkOut)}.`);

  root.innerHTML = `
    <div class="self-hero">
      <div>
        <h2>Today</h2>
        <p>${escapeHtml(status)}</p>
      </div>
      <div class="self-check">
        <button class="btn-check ${actionClass}" type="button" id="self-check-btn" data-testid="attendance-self-btn" ${checkedOut ? 'disabled' : ''}>${actionLabel}</button>
      </div>
    </div>

    <div class="section-head"><h3>My attendance history</h3></div>
    <div class="card">
      ${data.records.length === 0 ? `
        <div class="empty-state" data-testid="my-attendance-empty">
          <span class="empty-icon">${icon('clock', 22)}</span>
          <h3>No attendance yet</h3>
          <p>Check in to start building your history.</p>
        </div>` : attendanceTable(data.records, 'self')}
    </div>`;

  const btn = root.querySelector('#self-check-btn');
  if (btn && !checkedOut) {
    btn.addEventListener('click', async () => {
      btn.disabled = true;
      try {
        await api(checkedIn ? '/attendance/check-out' : '/attendance/check-in', { method: 'POST', body: {} });
        toast(checkedIn ? 'Checked out' : 'Checked in');
        renderEmployee(root);
      } catch (error) {
        toast(error.message, 'error');
        btn.disabled = false;
      }
    });
  }
}

function durationLabel(checkIn, checkOut) {
  const [inH, inM] = String(checkIn).slice(0, 5).split(':').map(Number);
  const [outH, outM] = String(checkOut).slice(0, 5).split(':').map(Number);
  const minutes = (outH * 60 + outM) - (inH * 60 + inM);
  if (minutes <= 0) return '0m';
  return `${Math.floor(minutes / 60)}h ${String(minutes % 60).padStart(2, '0')}m`;
}
