import { api } from './api.js';
import { escapeHtml, icon, initials, dateTime, toast } from './ui.js';
import * as dashboard from './views/dashboard.js';
import * as employees from './views/employees.js';
import * as departments from './views/departments.js';
import * as attendance from './views/attendance.js';
import * as leaves from './views/leaves.js';
import * as payroll from './views/payroll.js';
import * as tasks from './views/tasks.js';
import * as announcements from './views/announcements.js';
import * as accounts from './views/accounts.js';

// key -> { view, roles, icon, title/subtitle per audience }
const NAV = [
  { key: 'dashboard', icon: 'grid', roles: ['ADMIN', 'HR', 'EMPLOYEE'], view: dashboard,
    label: 'Dashboard', empLabel: 'My Dashboard',
    title: 'Dashboard', subtitle: 'Headcount, attendance and payroll at a glance',
    empTitle: 'My Dashboard', empSubtitle: 'Your tasks, leave, attendance and payslips' },
  { key: 'employees', icon: 'users', roles: ['ADMIN', 'HR'], view: employees,
    label: 'Employees', title: 'Employees', subtitle: 'Directory, logins and department assignments' },
  { key: 'departments', icon: 'building', roles: ['ADMIN'], view: departments,
    label: 'Departments', title: 'Departments', subtitle: 'Teams and their headcount' },
  { key: 'accounts', icon: 'shield', roles: ['ADMIN'], view: accounts,
    label: 'Accounts', title: 'Staff Accounts', subtitle: 'Create HR logins and manage access' },
  { key: 'tasks', icon: 'briefcase', roles: ['ADMIN', 'HR', 'EMPLOYEE'], view: tasks,
    label: 'Tasks', empLabel: 'My Tasks',
    title: 'Tasks', subtitle: 'Assign work and track progress',
    empTitle: 'My Tasks', empSubtitle: 'Your assigned and personal tasks' },
  { key: 'attendance', icon: 'calendarCheck', roles: ['ADMIN', 'HR', 'EMPLOYEE'], view: attendance,
    label: 'Attendance', empLabel: 'My Attendance',
    title: 'Attendance', subtitle: 'Daily check-in / check-out log',
    empTitle: 'My Attendance', empSubtitle: 'Check in, check out and view your history' },
  { key: 'leaves', icon: 'plane', roles: ['ADMIN', 'HR', 'EMPLOYEE'], view: leaves,
    label: 'Leave Requests', empLabel: 'My Leave',
    title: 'Leave Requests', subtitle: 'Approve or reject time-off requests',
    empTitle: 'My Leave', empSubtitle: 'Apply for time off and track approvals' },
  { key: 'payroll', icon: 'wallet', roles: ['ADMIN', 'HR', 'EMPLOYEE'], view: payroll,
    label: 'Payroll', empLabel: 'My Payslips',
    title: 'Payroll', subtitle: 'Monthly payslips, adjustments and payouts',
    empTitle: 'My Payslips', empSubtitle: 'Your monthly payslips and net pay' },
  { key: 'announcements', icon: 'megaphone', roles: ['ADMIN', 'HR', 'EMPLOYEE'], view: announcements,
    label: 'Announcements', title: 'Announcements', subtitle: 'Company-wide notices' },
];

let me = null;
let notifTimer = null;

function routesFor(role) {
  return NAV.filter((item) => item.roles.includes(role));
}

function meta(item) {
  const emp = me.role === 'EMPLOYEE';
  return {
    label: emp && item.empLabel ? item.empLabel : item.label,
    title: emp && item.empTitle ? item.empTitle : item.title,
    subtitle: emp && item.empSubtitle ? item.empSubtitle : item.subtitle,
  };
}

async function boot() {
  try {
    me = await api('/auth/me');
  } catch (_) {
    window.location.href = '/index.html';
    return;
  }

  const displayName = me.fullName || me.username;
  const roleLabel = me.role === 'ADMIN' ? 'Administrator' : me.role === 'HR' ? 'HR Manager' : 'Employee';
  document.getElementById('user-name').textContent = displayName;
  document.getElementById('user-role').textContent = roleLabel;
  document.getElementById('user-initials').textContent = initials(displayName);
  document.getElementById('user-avatar-top').textContent = initials(displayName);
  document.getElementById('today-label').textContent = new Date().toLocaleDateString('en-US', {
    weekday: 'short', month: 'short', day: 'numeric', year: 'numeric',
  });

  buildNav();

  document.getElementById('logout-btn').addEventListener('click', async () => {
    try {
      await api('/auth/logout', { method: 'POST' });
    } finally {
      window.location.href = '/index.html';
    }
  });

  const sidebar = document.getElementById('sidebar');
  const backdrop = document.getElementById('sidebar-backdrop');
  const openSidebar = (open) => {
    sidebar.classList.toggle('open', open);
    backdrop.hidden = !open;
  };
  document.getElementById('menu-btn').addEventListener('click', () => openSidebar(!sidebar.classList.contains('open')));
  backdrop.addEventListener('click', () => openSidebar(false));
  window.closeSidebar = () => openSidebar(false);

  setupNotifications();

  window.addEventListener('hashchange', render);
  const first = routesFor(me.role)[0].key;
  const currentKey = (window.location.hash || '').replace(/^#\//, '').split('?')[0];
  if (!routesFor(me.role).some((item) => item.key === currentKey)) {
    window.location.replace('#/' + first);
  }
  render();
}

function buildNav() {
  const nav = document.getElementById('nav');
  nav.innerHTML = routesFor(me.role).map((item) => {
    const m = meta(item);
    return `<a class="nav-item" href="#/${item.key}" data-view="${item.key}" data-testid="nav-${item.key}">
      ${icon(item.icon, 20)}<span>${escapeHtml(m.label)}</span>
    </a>`;
  }).join('');
}

function currentRoute() {
  const key = (window.location.hash || '').replace(/^#\//, '').split('?')[0];
  const available = routesFor(me.role);
  const item = available.find((route) => route.key === key) || available[0];
  return item;
}

function render() {
  const item = currentRoute();
  document.querySelectorAll('.nav-item').forEach((node) => {
    node.classList.toggle('active', node.dataset.view === item.key);
  });
  if (typeof window.closeSidebar === 'function') window.closeSidebar();

  const m = meta(item);
  document.getElementById('view-title').textContent = m.title;
  document.getElementById('view-subtitle').textContent = m.subtitle;

  const root = document.getElementById('view-root');
  root.innerHTML = '<div class="view-loading"><span class="spinner"></span>Loading…</div>';

  item.view.render(root, { me }).catch((error) => {
    if (error && error.status === 401) {
      window.location.href = '/index.html';
      return;
    }
    console.error('View failed:', error);
    root.innerHTML = `
      <div class="error-banner">
        <h3>Something went wrong</h3>
        <p>${escapeHtml((error && error.message) || 'Unexpected error while loading this view.')}</p>
        <button class="btn btn-secondary" type="button" id="retry-view-btn" data-testid="retry-view-btn">Retry</button>
      </div>`;
    document.getElementById('retry-view-btn').addEventListener('click', render);
  });
}

/* ---------------------------------------------------------- notifications */

function setupNotifications() {
  const btn = document.getElementById('notif-btn');
  const panel = document.getElementById('notif-panel');

  btn.addEventListener('click', (event) => {
    event.stopPropagation();
    const open = panel.hidden;
    panel.hidden = !open;
    if (open) loadNotifications(true);
  });
  document.addEventListener('click', (event) => {
    if (!document.getElementById('notif').contains(event.target)) panel.hidden = true;
  });

  loadNotifications(false);
  notifTimer = setInterval(() => loadNotifications(false), 30000);
}

async function loadNotifications(renderList) {
  let data;
  try {
    data = await api('/notifications');
  } catch (_) {
    return;
  }
  const badge = document.getElementById('notif-badge');
  badge.textContent = data.unread > 9 ? '9+' : String(data.unread);
  badge.hidden = data.unread === 0;

  if (!renderList) return;
  const panel = document.getElementById('notif-panel');
  const items = data.items || [];
  const list = items.length === 0
    ? '<div class="notif-empty">You are all caught up.</div>'
    : items.map((n) => `
      <div class="notif-item ${n.read ? '' : 'unread'}" data-testid="notif-item">
        <span class="notif-dot"></span>
        <div class="notif-copy">
          <span class="notif-title">${escapeHtml(n.title)}</span>
          <span class="notif-msg">${escapeHtml(n.message)}</span>
          <span class="notif-time">${dateTime(n.createdAt)}</span>
        </div>
      </div>`).join('');

  panel.innerHTML = `
    <div class="notif-head">
      <span>Notifications</span>
      ${data.unread > 0 ? '<button class="link-btn" type="button" id="notif-read-all" data-testid="notif-read-all-btn">Mark all read</button>' : ''}
    </div>
    <div class="notif-list">${list}</div>`;

  const readAll = panel.querySelector('#notif-read-all');
  if (readAll) {
    readAll.addEventListener('click', async () => {
      try {
        await api('/notifications/read-all', { method: 'PUT' });
        loadNotifications(true);
      } catch (error) {
        toast(error.message, 'error');
      }
    });
  }
}

boot();
