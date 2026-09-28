import { api } from './api.js';

const form = document.getElementById('login-form');
const errorBox = document.getElementById('login-error');
const submitButton = document.getElementById('login-submit');

// Already signed in? Straight to the app.
try {
  await api('/auth/me');
  window.location.href = '/app.html';
} catch (_) { /* not signed in — show the form */ }

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  errorBox.hidden = true;

  const username = form.username.value.trim();
  const password = form.password.value;
  if (!username || !password) {
    errorBox.textContent = 'Enter both a username and a password.';
    errorBox.hidden = false;
    return;
  }

  submitButton.disabled = true;
  submitButton.textContent = 'Signing in…';
  try {
    await api('/auth/login', { method: 'POST', body: { username, password } });
    window.location.href = '/app.html';
  } catch (error) {
    errorBox.textContent = error.message || 'Unable to sign in. Try again.';
    errorBox.hidden = false;
    submitButton.disabled = false;
    submitButton.textContent = 'Sign in';
  }
});
