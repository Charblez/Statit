//const BASE = '/api/v1';
const BASE = 'https://statit-backend.bluemeadow-174af2a3.eastus.azurecontainerapps.io/api/v1';

function getAdminUsername() {
  try {
    const saved = localStorage.getItem('currentUser');
    if (!saved) return null;
    const parsed = JSON.parse(saved);
    return parsed && parsed.admin ? parsed.username : null;
  } catch {
    return null;
  }
}

function adminHeaders() {
  const username = getAdminUsername();
  return username ? { 'X-Admin-Username': username } : {};
}

async function request(path, options = {}) {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options,
  });
  if (!res.ok) {
    let message = `Request failed: ${res.status}`;
    try {
      const body = await res.json();
      if (body.error) message = body.error;
      else if (body.message) message = body.message;
    } catch {
      // response wasn't JSON, use default message
    }
    throw new Error(message);
  }
  return res.json();
}

// --- Users ---

export function createUser({ username, email, passwordHash, birthday, demographics }) {
  return request('/users', {
    method: 'POST',
    body: JSON.stringify({ username, email, passwordHash, birthday, demographics }),
  });
}

export function getUser(username) {
  return request(`/users/${encodeURIComponent(username)}`);
}

export function getAllUsers() {
  return request('/users');
}

export function updateUser(userId, data) {
  return request(`/users/${userId}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export function deleteUser(userId) {
  return request(`/users/${userId}`, { method: 'DELETE' });
}

// --- Categories ---

export function getCategories(page = 0, size = 50) {
  return request(`/categories?page=${page}&size=${size}`);
}

export function getCategory(categoryId) {
  return request(`/categories/${categoryId}`);
}

export function createCategory({ name, description, units, tags, sort_order, founding_username, lower_limit, upper_limit }) {
  return request('/categories', {
    method: 'POST',
    body: JSON.stringify({ 
      name, 
      description, 
      units, 
      tags, 
      sort_order, 
      founding_username, 
      lower_limit, 
      upper_limit  
    }),
  });
}

// --- Admin (require admin user logged in) ---

export function getPendingCategories(page = 0, size = 50) {
  return request(`/admin/categories/pending?page=${page}&size=${size}`, {
    headers: adminHeaders(),
  });
}

export function adminUpdateCategory(categoryId, data) {
  return request(`/admin/categories/${categoryId}`, {
    method: 'PUT',
    headers: adminHeaders(),
    body: JSON.stringify(data),
  });
}

export function adminApproveCategory(categoryId) {
  return request(`/admin/categories/${categoryId}/approve`, {
    method: 'POST',
    headers: adminHeaders(),
  });
}

export function adminDeleteCategory(categoryId) {
  return request(`/admin/categories/${categoryId}`, {
    method: 'DELETE',
    headers: adminHeaders(),
  });
}

export function adminDeleteScore(scoreId) {
  return request(`/admin/scores/${scoreId}`, {
    method: 'DELETE',
    headers: adminHeaders(),
  });
}

export function adminSearchUsers(query = '') {
  const q = query ? `?query=${encodeURIComponent(query)}` : '';
  return request(`/admin/users${q}`, { headers: adminHeaders() });
}

export function adminGrantAdmin(username) {
  return request(`/admin/users/${encodeURIComponent(username)}/grant-admin`, {
    method: 'POST',
    headers: adminHeaders(),
  });
}

// --- Scores ---

export function submitScore({ user_id, category_id, score, tags, anonymous }) {
  return request('/scores', {
    method: 'POST',
    body: JSON.stringify({ user_id, category_id, score, tags, anonymous }),
  });
}

export function getScore(scoreId) {
  return request(`/scores/${scoreId}`);
}

export function getScoreInfo(scoreId) {
  return request(`/scores/${scoreId}/info`);
}

export function getUserScores(username, page = 0, size = 25) {
  return request(`/scores/user/${encodeURIComponent(username)}?page=${page}&size=${size}`);
}

// --- Leaderboards ---

export function getTopScores(categoryId, page = 0, size = 25) {
  return request(`/leaderboards/${categoryId}/top?page=${page}&size=${size}`);
}

export function getFilteredScores(categoryId, tags, page = 0, size = 25) {
  return request(`/leaderboards/${categoryId}/filtered?page=${page}&size=${size}`, {
    method: 'POST',
    body: JSON.stringify({ tags }),
  });
}

export function getLeaderboardSnapshot(categoryId, page = 0, size = 25) {
  return request(`/leaderboards/${categoryId}/snapshot?page=${page}&size=${size}`);
}

export function getBaselines(categoryId) {
  return request(`/leaderboards/${categoryId}/baselines`);
}
