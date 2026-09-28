/**
 * Typed-ish fetch layer for the StaffHub REST API.
 * All paths are relative to /api; sessions ride the JSESSIONID cookie.
 */
export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

export async function api(path, { method = 'GET', body } = {}) {
  const options = { method, headers: {} };
  if (body !== undefined) {
    options.headers['Content-Type'] = 'application/json';
    options.body = JSON.stringify(body);
  }

  const response = await fetch('/api' + path, options);

  // Session expired mid-use: bounce to the login page (except for the
  // auth endpoints themselves, which report 401 inline).
  if (response.status === 401 && path !== '/auth/login' && path !== '/auth/me') {
    window.location.href = '/index.html';
    throw new ApiError(401, 'Session expired');
  }

  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try {
      const data = await response.json();
      if (data && data.message) message = data.message;
    } catch (_) { /* non-JSON error body */ }
    throw new ApiError(response.status, message);
  }

  if (response.status === 204) return null;
  return response.json();
}
