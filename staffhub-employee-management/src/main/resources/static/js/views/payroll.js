import { api } from '../api.js';
import { badge, escapeHtml, icon, initials, money, openModal, toast } from '../ui.js';

const state = { month: '', data: null };
let rootEl = null;
let staff = false;

export async function render(root, { me }) {
  rootEl = root;
  staff = me.role === 'ADMIN' || me.role === 'HR';
  if (!staff) {
    return renderEmployee(root);
  }

  root.innerHTML = `
    <div class="toolbar">
      <div class="filters">
        <div class="field">
          <label class="label" for="payroll-month">Payroll month</label>
          <input class="input" type="month" id="payroll-month" style="width: 190px;" data-testid="payroll-month-input">
        </div>
      </div>
      <button class="btn btn-primary" type="button" id="generate-payroll-btn" data-testid="generate-payroll-btn">
        ${icon('plus', 16)} Generate Payroll
      </button>
    </div>

    <div id="payroll-summary" data-testid="payroll-summary"></div>
    <div id="payroll-wrap" data-testid="payroll-table-wrap"></div>`;

  root.querySelector('#payroll-month').addEventListener('change', (event) => {
    state.month = event.target.value;
    loadMonth();
  });
  root.querySelector('#generate-payroll-btn').addEventListener('click', generate);

  const month = await api('/payroll');
  state.month = month.month;
  root.querySelector('#payroll-month').value = month.month;
  setMonthData(month);
}

/* -------------------------------------------------------- employee view */
async function renderEmployee(root) {
  const records = await api('/payroll/mine');

  const totalNet = records.filter((r) => r.status === 'PAID')
    .reduce((sum, r) => sum + Number(r.netPay), 0);

  root.innerHTML = `
    <div class="stat-grid" style="grid-template-columns: repeat(3, minmax(0,1fr));">
      <div class="stat-card"><div class="stat-copy"><span class="stat-label">Payslips</span><span class="stat-value">${records.length}</span></div><span class="stat-icon info">${icon('wallet', 20)}</span></div>
      <div class="stat-card"><div class="stat-copy"><span class="stat-label">Paid to date</span><span class="stat-value money">${money(totalNet)}</span></div><span class="stat-icon success">${icon('check', 20)}</span></div>
      <div class="stat-card"><div class="stat-copy"><span class="stat-label">Pending</span><span class="stat-value">${records.filter((r) => r.status !== 'PAID').length}</span></div><span class="stat-icon warning">${icon('clock', 20)}</span></div>
    </div>
    <div class="card">
      ${records.length === 0 ? `
        <div class="empty-state" data-testid="my-payslips-empty">
          <span class="empty-icon">${icon('wallet', 22)}</span>
          <h3>No payslips yet</h3>
          <p>Your monthly payslips will appear here once payroll is run.</p>
        </div>` : `
        <div class="table-scroll">
          <table class="data-table" data-testid="my-payslips-table">
            <thead><tr><th>Month</th><th class="cell-money">Base</th><th class="cell-money">Bonus</th><th class="cell-money">Deductions</th><th class="cell-money">Net pay</th><th>Status</th><th></th></tr></thead>
            <tbody>
              ${records.map((r) => `
                <tr data-testid="my-payslip-row">
                  <td class="mono">${escapeHtml(r.month)}</td>
                  <td class="cell-money">${money(r.baseSalary)}</td>
                  <td class="cell-money">${money(r.bonus)}</td>
                  <td class="cell-money">${money(r.deductions)}</td>
                  <td class="cell-money" style="color: var(--ink);">${money(r.netPay)}</td>
                  <td>${r.status === 'PAID' ? badge('Paid', 'success') : badge('Pending', 'warning')}</td>
                  <td><button class="btn btn-secondary btn-sm" type="button" data-pdf="${r.id}" data-testid="download-payslip-btn">${icon('download', 14)} PDF</button></td>
                </tr>`).join('')}
            </tbody>
          </table>
        </div>`}
    </div>`;

  root.querySelectorAll('[data-pdf]').forEach((button) => {
    button.addEventListener('click', () => downloadPayslip(Number(button.dataset.pdf)));
  });
}

/* ----------------------------------------------------------- staff view */
function setMonthData(data) {
  state.data = data;
  renderSummary();
  renderTable();
}

async function loadMonth() {
  if (!state.month) return;
  setMonthData(await api('/payroll?month=' + encodeURIComponent(state.month)));
}

async function generate() {
  try {
    const response = await api('/payroll/generate', { method: 'POST', body: { month: state.month } });
    toast(response.message, response.generated > 0 ? 'success' : 'info');
    rootEl.querySelector('#payroll-month').value = response.month.month;
    setMonthData(response.month);
  } catch (error) {
    toast(error.message, 'error');
  }
}

function renderSummary() {
  const summary = rootEl.querySelector('#payroll-summary');
  if (!state.data) { summary.innerHTML = ''; return; }

  const gross = Number(state.data.totalBase) + Number(state.data.totalBonus);
  const card = (label, value, iconName, tone) => `
    <div class="stat-card" data-testid="payroll-stat-card">
      <div class="stat-copy">
        <span class="stat-label">${escapeHtml(label)}</span>
        <span class="stat-value money">${value}</span>
      </div>
      <span class="stat-icon ${tone}">${icon(iconName, 20)}</span>
    </div>`;

  summary.innerHTML = `
    <div class="stat-grid" style="grid-template-columns: repeat(4, minmax(0, 1fr));">
      ${card('Total gross', money(gross), 'wallet', 'info')}
      ${card('Deductions', money(state.data.totalDeductions), 'alert', 'warning')}
      ${card('Net payout', money(state.data.totalNet), 'check', 'success')}
      ${card('Payout progress', `${state.data.paidCount}/${state.data.records.length} paid`, 'users', 'neutral')}
    </div>`;
}

function renderTable() {
  const wrap = rootEl.querySelector('#payroll-wrap');
  if (!state.data) { wrap.innerHTML = ''; return; }

  if (state.data.records.length === 0) {
    wrap.innerHTML = `
      <div class="empty-state" data-testid="payroll-empty-state">
        <span class="empty-icon">${icon('wallet', 22)}</span>
        <h3>No payslips for ${escapeHtml(state.data.month)}</h3>
        <p>Generate payroll to create one payslip per active employee, then adjust bonus and deductions.</p>
      </div>`;
    return;
  }

  const rows = state.data.records.map((record) => {
    const pending = record.status === 'PENDING';
    return `
      <tr data-testid="payroll-row" data-payroll-id="${record.id}">
        <td>
          <div class="employee-cell">
            <span class="avatar">${escapeHtml(initials(record.employeeName))}</span>
            <div>
              <span class="name">${escapeHtml(record.employeeName)}</span>
              <span class="cell-sub">${escapeHtml(record.employeeCode || '')} · ${escapeHtml(record.role)}</span>
            </div>
          </div>
        </td>
        <td>${escapeHtml(record.departmentName)}</td>
        <td class="cell-money">${money(record.baseSalary)}</td>
        <td class="cell-money">${money(record.bonus)}</td>
        <td class="cell-money">${money(record.deductions)}</td>
        <td class="cell-money" style="color: var(--ink);">${money(record.netPay)}</td>
        <td>${pending ? badge('Pending', 'warning') : badge('Paid', 'info')}</td>
        <td>
          <div class="cell-actions">
            <button class="icon-btn" type="button" title="Download payslip PDF" aria-label="Download payslip"
                    data-pdf="${record.id}" data-testid="download-payslip-btn">${icon('download', 15)}</button>
            ${pending ? `
              <button class="icon-btn" type="button" title="Adjust bonus / deductions" aria-label="Adjust payslip"
                      data-action="edit" data-testid="edit-payroll-btn">${icon('pencil', 15)}</button>
              <button class="btn btn-success btn-sm" type="button" data-action="pay" data-testid="mark-paid-btn">Mark paid</button>`
              : '<span class="muted" style="font-size: 12px;">Settled</span>'}
          </div>
        </td>
      </tr>`;
  }).join('');

  wrap.innerHTML = `
    <div class="card">
      <div class="table-scroll">
        <table class="data-table" data-testid="payroll-table">
          <thead>
            <tr>
              <th>Employee</th><th>Department</th><th class="cell-money">Base</th><th class="cell-money">Bonus</th>
              <th class="cell-money">Deductions</th><th class="cell-money">Net pay</th><th>Status</th><th></th>
            </tr>
          </thead>
          <tbody>
            ${rows}
            <tr class="total-row" data-testid="payroll-total-row">
              <td colspan="2">Totals · ${escapeHtml(state.data.month)}</td>
              <td class="cell-money">${money(state.data.totalBase)}</td>
              <td class="cell-money">${money(state.data.totalBonus)}</td>
              <td class="cell-money">${money(state.data.totalDeductions)}</td>
              <td class="cell-money">${money(state.data.totalNet)}</td>
              <td colspan="2"></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>`;

  wrap.querySelectorAll('[data-action="edit"]').forEach((button) => {
    button.addEventListener('click', () => {
      const id = Number(button.closest('tr').dataset.payrollId);
      const record = state.data.records.find((item) => item.id === id);
      if (record) openAdjuster(record);
    });
  });
  wrap.querySelectorAll('[data-action="pay"]').forEach((button) => {
    button.addEventListener('click', async () => {
      const id = Number(button.closest('tr').dataset.payrollId);
      const record = state.data.records.find((item) => item.id === id);
      try {
        await api(`/payroll/${id}/pay`, { method: 'PUT' });
        toast(`Marked ${record.employeeName}'s ${record.month} payslip as paid`);
        await loadMonth();
      } catch (error) {
        toast(error.message, 'error');
      }
    });
  });

  wrap.querySelectorAll('[data-pdf]').forEach((button) => {
    button.addEventListener('click', () => downloadPayslip(Number(button.closest('tr').dataset.payrollId)));
  });
}

function downloadPayslip(id) {
  const a = document.createElement('a');
  a.href = '/api/payroll/' + id + '/payslip.pdf';
  a.rel = 'noopener';
  document.body.appendChild(a);
  a.click();
  a.remove();
}

function openAdjuster(record) {
  openModal({
    title: 'Adjust payslip',
    sub: `${record.employeeName} · ${record.month}`,
    size: 'narrow',
    body: `
      <form id="payroll-form" data-testid="payroll-adjust-form">
        <div class="form-grid">
          <div class="field">
            <label class="label" for="payroll-base">Base salary</label>
            <input class="input" id="payroll-base" value="${money(record.baseSalary)}" disabled>
          </div>
          <div class="field">
            <label class="label" for="payroll-bonus">Bonus (USD)</label>
            <input class="input" id="payroll-bonus" name="bonus" type="number" min="0" step="0.01" required
                   value="${Number(record.bonus)}" data-testid="payroll-bonus-input">
          </div>
          <div class="field span-2">
            <label class="label" for="payroll-deductions">Deductions (USD)</label>
            <input class="input" id="payroll-deductions" name="deductions" type="number" min="0" step="0.01" required
                   value="${Number(record.deductions)}" data-testid="payroll-deductions-input">
          </div>
        </div>
        <p class="muted" style="font-size: 12.5px;">Net pay is recalculated as base + bonus − deductions.</p>
        <p class="form-error" id="payroll-form-error" hidden></p>
      </form>`,
    footer: `
      <button class="btn btn-secondary" type="button" data-cancel>Cancel</button>
      <button class="btn btn-primary" type="submit" form="payroll-form" data-testid="save-payroll-btn">Save adjustments</button>`,
    onMount(overlay, close) {
      overlay.querySelector('[data-cancel]').addEventListener('click', close);
      const form = overlay.querySelector('#payroll-form');
      const errorBox = overlay.querySelector('#payroll-form-error');
      form.addEventListener('submit', async (event) => {
        event.preventDefault();
        errorBox.hidden = true;
        try {
          await api('/payroll/' + record.id, {
            method: 'PUT',
            body: { bonus: Number(form.bonus.value), deductions: Number(form.deductions.value) },
          });
          toast(`Updated ${record.employeeName}'s payslip`);
          close();
          await loadMonth();
        } catch (error) {
          errorBox.textContent = error.message;
          errorBox.hidden = false;
        }
      });
    },
  });
}
