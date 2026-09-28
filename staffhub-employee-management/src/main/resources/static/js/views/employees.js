import { api } from '../api.js';
import { badge, confirmDialog, credentialsModal, date, debounce, escapeHtml, icon, initials, money, openModal, toast } from '../ui.js';

const state = { q: '', departmentId: '', status: '' };
let rootEl = null;
let tableWrap = null;
let departmentsCache = [];
let rolesCache = [];

export async function render(root) {
  rootEl = root;
  state.q = '';
  state.departmentId = '';
  state.status = '';

  const [departments, roles] = await Promise.all([api('/departments'), api('/employees/roles')]);
  departmentsCache = departments;
  rolesCache = roles;

  const departmentOptions = departments
    .map((department) => `<option value="${department.id}">${escapeHtml(department.name)}</option>`)
    .join('');
  const roleOptions = roles.map((role) => `<option value="${escapeHtml(role)}"></option>`).join('');

  root.innerHTML = `
    <div class="toolbar">
      <div class="filters">
        <div class="search-wrap">
          ${icon('search', 15)}
          <input class="input" type="text" id="employee-search" placeholder="Search name, email, code…"
                 data-testid="employee-search-input">
        </div>
        <select class="select" id="employee-department-filter" style="width: 190px;" data-testid="employee-department-filter">
          <option value="">All departments</option>
          ${departmentOptions}
        </select>
        <select class="select" id="employee-status-filter" style="width: 150px;" data-testid="employee-status-filter">
          <option value="">All statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="INACTIVE">Inactive</option>
        </select>
      </div>
      <button class="btn btn-primary" type="button" id="add-employee-btn" data-testid="add-employee-btn">
        ${icon('plus', 16)} Add Employee
      </button>
    </div>
    <div id="employee-table-wrap" data-testid="employee-table-wrap"></div>`;

  tableWrap = root.querySelector('#employee-table-wrap');

  root.querySelector('#employee-search').addEventListener('input', debounce((event) => {
    state.q = event.target.value.trim();
    loadTable();
  }, 250));
  root.querySelector('#employee-department-filter').addEventListener('change', (event) => {
    state.departmentId = event.target.value;
    loadTable();
  });
  root.querySelector('#employee-status-filter').addEventListener('change', (event) => {
    state.status = event.target.value;
    loadTable();
  });
  root.querySelector('#add-employee-btn').addEventListener('click', () => openEditor(null));

  await loadTable();
}

async function loadTable() {
  const params = new URLSearchParams();
  if (state.q) params.set('q', state.q);
  if (state.departmentId) params.set('departmentId', state.departmentId);
  if (state.status) params.set('status', state.status);

  const employees = await api('/employees' + (params.toString() ? '?' + params.toString() : ''));

  if (employees.length === 0) {
    tableWrap.innerHTML = `
      <div class="empty-state" data-testid="employees-empty-state">
        <span class="empty-icon">${icon('users', 22)}</span>
        <h3>No employees found</h3>
        <p>${state.q || state.departmentId || state.status
          ? 'No one matches the current search and filters. Try clearing them.'
          : 'Add your first employee to start building the directory.'}</p>
        <button class="btn btn-primary" type="button" id="empty-add-employee">${icon('plus', 16)} Add Employee</button>
      </div>`;
    const emptyAdd = tableWrap.querySelector('#empty-add-employee');
    if (emptyAdd) emptyAdd.addEventListener('click', () => openEditor(null));
    return;
  }

  const rows = employees
    .map((employee) => `
      <tr data-testid="employee-row" data-employee-id="${employee.id}">
        <td>
          <div class="employee-cell">
            <span class="avatar">${escapeHtml(initials(employee.firstName + ' ' + employee.lastName))}</span>
            <div>
              <span class="name">${escapeHtml(employee.firstName)} ${escapeHtml(employee.lastName)}</span>
              <span class="cell-sub mono">${escapeHtml(employee.employeeCode || '')}</span>
            </div>
          </div>
        </td>
        <td>
          ${escapeHtml(employee.email)}
          <span class="cell-sub">${escapeHtml(employee.phone || '—')}</span>
        </td>
        <td>${escapeHtml(employee.departmentName)}</td>
        <td>${escapeHtml(employee.role)}</td>
        <td class="cell-money">${money(employee.salary)}<span class="cell-sub">monthly</span></td>
        <td>${date(employee.hireDate)}</td>
        <td>${employee.status === 'ACTIVE' ? badge('Active', 'success') : badge('Inactive', 'neutral')}</td>
        <td>
          <div class="cell-actions">
            <button class="icon-btn" type="button" title="Reset login" aria-label="Reset login"
                    data-action="reset" data-testid="reset-employee-login-btn">${icon('key', 15)}</button>
            <button class="icon-btn" type="button" title="Edit employee" aria-label="Edit employee"
                    data-action="edit" data-testid="edit-employee-btn">${icon('pencil', 15)}</button>
            <button class="icon-btn danger" type="button" title="Delete employee" aria-label="Delete employee"
                    data-action="delete" data-testid="delete-employee-btn">${icon('trash', 15)}</button>
          </div>
        </td>
      </tr>`)
    .join('');

  tableWrap.innerHTML = `
    <div class="card">
      <div class="table-scroll">
        <table class="data-table" data-testid="employees-table">
          <thead>
            <tr>
              <th>Employee</th><th>Contact</th><th>Department</th><th>Role</th>
              <th class="cell-money">Salary</th><th>Hire date</th><th>Status</th><th></th>
            </tr>
          </thead>
          <tbody>${rows}</tbody>
        </table>
      </div>
    </div>`;

  tableWrap.querySelectorAll('[data-action="reset"]').forEach((button) => {
    button.addEventListener('click', async () => {
      const id = Number(button.closest('tr').dataset.employeeId);
      const employee = employees.find((item) => item.id === id);
      const confirmed = await confirmDialog({
        title: 'Reset login',
        message: `Generate a new password for ${employee.firstName} ${employee.lastName}? Any existing password will stop working.`,
        confirmLabel: 'Reset login',
      });
      if (!confirmed) return;
      try {
        const res = await api('/employees/' + id + '/reset-account', { method: 'POST' });
        credentialsModal({
          title: 'Employee login',
          sub: `${employee.firstName} ${employee.lastName}`,
          username: res.username,
          password: res.password,
          message: res.message,
        });
      } catch (error) {
        toast(error.message, 'error');
      }
    });
  });
  tableWrap.querySelectorAll('[data-action="edit"]').forEach((button) => {
    button.addEventListener('click', () => {
      const id = Number(button.closest('tr').dataset.employeeId);
      const employee = employees.find((item) => item.id === id);
      if (employee) openEditor(employee);
    });
  });
  tableWrap.querySelectorAll('[data-action="delete"]').forEach((button) => {
    button.addEventListener('click', async () => {
      const id = Number(button.closest('tr').dataset.employeeId);
      const employee = employees.find((item) => item.id === id);
      const confirmed = await confirmDialog({
        title: 'Delete employee',
        message: `Delete ${employee.firstName} ${employee.lastName}? Their attendance history, leave requests and payslips are removed too. This cannot be undone.`,
        confirmLabel: 'Delete',
        danger: true,
      });
      if (!confirmed) return;
      try {
        await api('/employees/' + id, { method: 'DELETE' });
        toast(`Deleted ${employee.firstName} ${employee.lastName}`);
        await loadTable();
      } catch (error) {
        toast(error.message, 'error');
      }
    });
  });
}

function openEditor(employee) {
  const isEdit = employee !== null;
  const departmentOptions = departmentsCache
    .map((department) => `<option value="${department.id}" ${isEdit && employee.departmentId === department.id ? 'selected' : ''}>${escapeHtml(department.name)}</option>`)
    .join('');

  openModal({
    title: isEdit ? 'Edit employee' : 'Add employee',
    sub: isEdit ? `${employee.firstName} ${employee.lastName} · ${employee.employeeCode || ''}` : 'Create a new employee record',
    size: 'wide',
    body: `
      <form id="employee-form" data-testid="employee-form">
        <div class="form-grid">
          <div class="field">
            <label class="label" for="emp-first-name">First name</label>
            <input class="input" id="emp-first-name" name="firstName" required maxlength="80" value="${isEdit ? escapeHtml(employee.firstName) : ''}" placeholder="Jane">
          </div>
          <div class="field">
            <label class="label" for="emp-last-name">Last name</label>
            <input class="input" id="emp-last-name" name="lastName" required maxlength="80" value="${isEdit ? escapeHtml(employee.lastName) : ''}" placeholder="Doe">
          </div>
          <div class="field">
            <label class="label" for="emp-email">Email</label>
            <input class="input" id="emp-email" name="email" type="email" required maxlength="160" value="${isEdit ? escapeHtml(employee.email) : ''}" placeholder="jane.doe@staffhub.io">
          </div>
          <div class="field">
            <label class="label" for="emp-phone">Phone</label>
            <input class="input" id="emp-phone" name="phone" maxlength="30" value="${isEdit ? escapeHtml(employee.phone || '') : ''}" placeholder="+1 202 555 0100">
          </div>
          <div class="field">
            <label class="label" for="emp-department">Department</label>
            <select class="select" id="emp-department" name="departmentId" required data-testid="employee-department-select">
              <option value="" disabled ${isEdit ? '' : 'selected'}>Select department…</option>
              ${departmentOptions}
            </select>
          </div>
          <div class="field">
            <label class="label" for="emp-role">Role</label>
            <input class="input" id="emp-role" name="role" required maxlength="100" list="employee-role-options"
                   value="${isEdit ? escapeHtml(employee.role) : ''}" placeholder="Software Engineer">
            <datalist id="employee-role-options">${roleOptions}</datalist>
          </div>
          <div class="field">
            <label class="label" for="emp-salary">Monthly salary (USD)</label>
            <input class="input" id="emp-salary" name="salary" type="number" min="1" step="0.01" required
                   value="${isEdit ? employee.salary : ''}" placeholder="6000">
          </div>
          <div class="field">
            <label class="label" for="emp-hire-date">Hire date</label>
            <input class="input" id="emp-hire-date" name="hireDate" type="date" required
                   value="${isEdit ? employee.hireDate : ''}">
          </div>
          <div class="field span-2">
            <label class="label" for="emp-status">Status</label>
            <select class="select" id="emp-status" name="status" data-testid="employee-status-select">
              <option value="ACTIVE" ${isEdit && employee.status !== 'ACTIVE' ? '' : 'selected'}>Active</option>
              <option value="INACTIVE" ${isEdit && employee.status === 'INACTIVE' ? 'selected' : ''}>Inactive</option>
            </select>
          </div>
        </div>
        <p class="form-error" id="employee-form-error" hidden></p>
      </form>`,
    footer: `
      <button class="btn btn-secondary" type="button" data-cancel>Cancel</button>
      <button class="btn btn-primary" type="submit" form="employee-form" data-testid="save-employee-btn">
        ${isEdit ? 'Save changes' : 'Add employee'}
      </button>`,
    onMount(overlay, close) {
      overlay.querySelector('[data-cancel]').addEventListener('click', close);
      const form = overlay.querySelector('#employee-form');
      const errorBox = overlay.querySelector('#employee-form-error');
      form.addEventListener('submit', async (event) => {
        event.preventDefault();
        errorBox.hidden = true;
        const payload = {
          firstName: form.firstName.value.trim(),
          lastName: form.lastName.value.trim(),
          email: form.email.value.trim(),
          phone: form.phone.value.trim(),
          departmentId: Number(form.departmentId.value),
          role: form.role.value.trim(),
          salary: Number(form.salary.value),
          hireDate: form.hireDate.value,
          status: form.status.value,
        };
        try {
          if (isEdit) {
            await api('/employees/' + employee.id, { method: 'PUT', body: payload });
            toast(`Updated ${payload.firstName} ${payload.lastName}`);
            close();
            await loadTable();
          } else {
            const created = await api('/employees', { method: 'POST', body: payload });
            close();
            await loadTable();
            credentialsModal({
              title: 'Employee login created',
              sub: `${payload.firstName} ${payload.lastName}`,
              username: created.username,
              password: created.password,
              message: `${payload.firstName} was added to the directory. Share these login details securely.`,
            });
          }
        } catch (error) {
          errorBox.textContent = error.message;
          errorBox.hidden = false;
        }
      });
    },
  });
}
