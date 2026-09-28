import { api } from '../api.js';
import { badge, date, dateTime, escapeHtml, icon, initials, money, time, toast } from '../ui.js';

function statCard({ label, value, iconName, tone, hint = '', moneyValue = false }) {
  return `
    <div class="stat-card" data-testid="stat-card">
      <div class="stat-copy">
        <span class="stat-label">${escapeHtml(label)}</span>
        <span class="stat-value ${moneyValue ? 'money' : ''}">${value}</span>
        ${hint ? `<span class="stat-hint">${escapeHtml(hint)}</span>` : ''}
      </div>
      <span class="stat-icon ${tone}">${icon(iconName, 20)}</span>
    </div>`;
}

export async function render(root, { me }) {
  if (me.role === 'EMPLOYEE') {
    return renderEmployee(root);
  }
  return renderStaff(root);
}

async function renderStaff(root) {
  const stats = await api('/dashboard/stats');

  const rows = stats.recentHires
    .map((employee) => `
      <tr data-testid="recent-hire-row">
        <td>
          <div class="employee-cell">
            <span class="avatar">${escapeHtml(initials(employee.firstName + ' ' + employee.lastName))}</span>
            <div>
              <span class="name">${escapeHtml(employee.firstName)} ${escapeHtml(employee.lastName)}</span>
              <span class="cell-sub">${escapeHtml(employee.email)}</span>
            </div>
          </div>
        </td>
        <td class="mono">${escapeHtml(employee.employeeCode || '—')}</td>
        <td>${escapeHtml(employee.departmentName)}</td>
        <td>${escapeHtml(employee.role)}</td>
        <td>${date(employee.hireDate)}</td>
        <td>${employee.status === 'ACTIVE' ? badge('Active', 'success') : badge('Inactive', 'neutral')}</td>
      </tr>`)
    .join('');

  root.innerHTML = `
    <div class="stat-grid">
      ${statCard({ label: 'Total Employees', value: stats.totalEmployees, iconName: 'users', tone: 'info', hint: `${stats.inactiveEmployees} inactive` })}
      ${statCard({ label: 'Active', value: stats.activeEmployees, iconName: 'userCheck', tone: 'success' })}
      ${statCard({ label: 'Departments', value: stats.totalDepartments, iconName: 'building', tone: 'neutral' })}
      ${statCard({ label: 'Present Today', value: stats.presentToday, iconName: 'calendarCheck', tone: 'success' })}
      ${statCard({ label: 'Pending Leaves', value: stats.pendingLeaves, iconName: 'plane', tone: 'warning' })}
      ${statCard({ label: `Net Payroll · ${stats.monthlyPayrollMonth}`, value: money(stats.monthlyPayroll), iconName: 'wallet', tone: 'info', moneyValue: true })}
    </div>

    <div class="card">
      <div class="card-header">
        <div>
          <h3>Recent hires</h3>
          <p class="card-sub">The latest additions to your team</p>
        </div>
        <a class="btn btn-secondary btn-sm" href="#/employees" data-testid="view-all-employees-btn">View all employees</a>
      </div>
      ${stats.recentHires.length === 0 ? `
        <div class="empty-state">
          <span class="empty-icon">${icon('users', 22)}</span>
          <h3>No employees yet</h3>
          <p>Add your first employee to see the directory come alive.</p>
          <a class="btn btn-primary" href="#/employees">Go to employees</a>
        </div>` : `
        <div class="table-scroll">
          <table class="data-table" data-testid="recent-hires-table">
            <thead>
              <tr><th>Employee</th><th>Code</th><th>Department</th><th>Role</th><th>Hire date</th><th>Status</th></tr>
            </thead>
            <tbody>${rows}</tbody>
          </table>
        </div>`}
    </div>`;
}

async function renderEmployee(root) {
  const data = await api('/dashboard/me');

  const checkedIn = data.checkedInToday;
  const checkedOut = !!data.checkOutTime;
  const actionLabel = !checkedIn ? 'Check in' : (!checkedOut ? 'Check out' : 'Day complete');
  const actionClass = !checkedIn ? '' : (!checkedOut ? 'checkout' : '');
  const statusLine = !checkedIn
    ? 'You have not checked in today.'
    : (!checkedOut
        ? `Checked in at ${time(data.checkInTime)} — on the clock.`
        : `Checked in ${time(data.checkInTime)} · out ${time(data.checkOutTime)}.`);

  root.innerHTML = `
    <div class="self-hero">
      <div>
        <h2>Hi, ${escapeHtml(data.employeeName.split(' ')[0])} 👋</h2>
        <p>${escapeHtml(data.role)} · ${escapeHtml(data.departmentName)}</p>
      </div>
      <div class="self-check">
        <span class="muted-light" data-testid="self-attendance-status">${escapeHtml(statusLine)}</span>
        <button class="btn-check ${actionClass}" type="button" id="self-check-btn" data-testid="self-check-btn"
          ${checkedOut ? 'disabled' : ''}>${actionLabel}</button>
      </div>
    </div>

    <div class="stat-grid">
      ${statCard({ label: 'Open Tasks', value: data.openTasks, iconName: 'briefcase', tone: 'warning' })}
      ${statCard({ label: 'Completed Tasks', value: data.doneTasks, iconName: 'userCheck', tone: 'success' })}
      ${statCard({ label: 'Pending Leaves', value: data.pendingLeaves, iconName: 'plane', tone: 'info' })}
      ${statCard({ label: 'Approved Leaves', value: data.approvedLeaves, iconName: 'calendarCheck', tone: 'success' })}
      ${statCard({ label: 'Leave Left', value: data.leaveRemaining, iconName: 'plane', tone: 'info', hint: `of ${data.leaveQuota} days this year` })}
    </div>

    <div class="section-head"><h3>Your recent tasks</h3><a class="btn btn-secondary btn-sm" href="#/tasks" data-testid="view-tasks-btn">View all</a></div>
    <div class="card">
      ${data.recentTasks.length === 0 ? `
        <div class="empty-state"><span class="empty-icon">${icon('briefcase', 22)}</span><h3>No tasks yet</h3><p>Tasks assigned to you will show up here.</p></div>` : `
        <div class="table-scroll">
          <table class="data-table" data-testid="self-tasks-table">
            <thead><tr><th>Task</th><th>Priority</th><th>Due</th><th>Status</th></tr></thead>
            <tbody>
              ${data.recentTasks.map((t) => `
                <tr data-testid="self-task-row">
                  <td><span class="name">${escapeHtml(t.title)}</span></td>
                  <td>${priorityBadge(t.priority)}</td>
                  <td>${t.dueDate ? date(t.dueDate) : '—'}</td>
                  <td>${statusBadge(t.status)}</td>
                </tr>`).join('')}
            </tbody>
          </table>
        </div>`}
    </div>

    <div class="section-head"><h3>Latest announcements</h3><a class="btn btn-secondary btn-sm" href="#/announcements">View all</a></div>
    <div class="ann-grid">
      ${data.recentAnnouncements.length === 0 ? `
        <div class="card"><div class="empty-state"><span class="empty-icon">${icon('megaphone', 22)}</span><h3>Nothing yet</h3><p>Company announcements will appear here.</p></div></div>` :
        data.recentAnnouncements.map((a) => `
          <div class="ann-card" data-testid="self-announcement">
            <div class="ann-top">
              <span class="ann-icon">${icon('megaphone', 18)}</span>
              <div style="flex:1;">
                <div class="ann-title">${escapeHtml(a.title)}</div>
                <p class="ann-body">${escapeHtml(a.body)}</p>
                <div class="ann-meta">${icon('users', 12)} ${escapeHtml(a.authorName)} · ${dateTime(a.createdAt)}</div>
              </div>
            </div>
          </div>`).join('')}
    </div>`;

  const btn = root.querySelector('#self-check-btn');
  if (btn && !checkedOut) {
    btn.addEventListener('click', async () => {
      btn.disabled = true;
      try {
        const path = checkedIn ? '/attendance/check-out' : '/attendance/check-in';
        await api(path, { method: 'POST', body: {} });
        toast(checkedIn ? 'Checked out — have a great evening!' : 'Checked in — have a productive day!');
        renderEmployee(root);
      } catch (error) {
        toast(error.message, 'error');
        btn.disabled = false;
      }
    });
  }
}

function priorityBadge(priority) {
  const map = { HIGH: 'danger', MEDIUM: 'warning', LOW: 'success' };
  return badge(priority.charAt(0) + priority.slice(1).toLowerCase(), map[priority] || 'neutral');
}

function statusBadge(status) {
  if (status === 'DONE') return badge('Done', 'success');
  if (status === 'IN_PROGRESS') return badge('In progress', 'info');
  return badge('To do', 'neutral');
}
