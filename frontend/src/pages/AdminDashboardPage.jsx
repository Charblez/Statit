import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getPendingCategories, adminSearchUsers, adminGrantAdmin } from '../api';

export default function AdminDashboardPage({ currentUser }) {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const [userQuery, setUserQuery] = useState('');
  const [users, setUsers] = useState([]);
  const [userLoading, setUserLoading] = useState(false);
  const [userError, setUserError] = useState('');
  const [userMessage, setUserMessage] = useState('');

  const runUserSearch = async (q) => {
    setUserLoading(true);
    setUserError('');
    try {
      const data = await adminSearchUsers(q);
      setUsers(Array.isArray(data) ? data : []);
    } catch (err) {
      setUserError(err.message);
      setUsers([]);
    } finally {
      setUserLoading(false);
    }
  };

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    runUserSearch(userQuery.trim());
  };

  const handleGrant = async (username) => {
    setUserError('');
    setUserMessage('');
    try {
      await adminGrantAdmin(username);
      setUserMessage(`Granted admin to ${username}.`);
      await runUserSearch(userQuery.trim());
    } catch (err) {
      setUserError(err.message);
    }
  };

  useEffect(() => {
    if (!currentUser?.admin) return;
    setLoading(true);
    getPendingCategories(0, 100)
      .then((data) => setCategories(data.categories || []))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
    runUserSearch('');
  }, [currentUser]);

  if (!currentUser?.admin) {
    return (
      <div className="page">
        <div className="empty-state">
          <h3>Admins only</h3>
          <p>You do not have access to this page.</p>
        </div>
      </div>
    );
  }

  if (loading) return <div className="page"><div className="loading">Loading pending categories...</div></div>;

  return (
    <div className="page">
      <h1 className="page-title">Admin Dashboard</h1>

      <div className="panel" style={{ marginBottom: 24 }}>
        <div className="panel-title">User Management</div>
        <form onSubmit={handleSearchSubmit} style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
          <input
            className="input"
            placeholder="Search by username"
            value={userQuery}
            onChange={(e) => setUserQuery(e.target.value)}
          />
          <button className="btn btn-primary btn-sm" type="submit">Search</button>
        </form>

        {userError && <div className="error-banner">{userError}</div>}
        {userMessage && (
          <div className="error-banner" style={{ background: 'var(--success, #1e7e34)' }}>{userMessage}</div>
        )}

        {userLoading ? (
          <div className="loading">Loading users...</div>
        ) : users.length === 0 ? (
          <p style={{ color: 'var(--text-muted)' }}>No users found.</p>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table className="score-table">
              <thead>
                <tr>
                  <th>Username</th>
                  <th>Email</th>
                  <th>Admin</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.userId}>
                    <td style={{ fontWeight: 600 }}>{u.username}</td>
                    <td>{u.email || '-'}</td>
                    <td>{u.admin ? 'Yes' : 'No'}</td>
                    <td>
                      {u.admin ? (
                        <span className="tag">Admin</span>
                      ) : (
                        <button className="btn btn-ghost btn-sm" onClick={() => handleGrant(u.username)}>
                          Grant admin
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <h2 className="page-title" style={{ marginBottom: 0, fontSize: '1.5rem' }}>Pending Categories</h2>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {categories.length === 0 ? (
        <div className="empty-state">
          <h3>Queue empty</h3>
          <p>No categories awaiting approval.</p>
        </div>
      ) : (
        <div className="card-grid">
          {categories.map((cat) => (
            <div
              key={cat.categoryId}
              className="category-card"
              onClick={() => navigate(`/admin/category/${cat.categoryId}`)}
            >
              <h3>{cat.name}</h3>
              {cat.description && <p className="card-meta">{cat.description}</p>}
              <p className="card-meta">
                Unit: {cat.units} &middot; {cat.sortOrder ? 'Higher is better' : 'Lower is better'}
              </p>
              <p className="card-meta">
                Range: {cat.lowerLimit} – {cat.upperLimit}
              </p>
              {cat.tags && cat.tags.length > 0 && (
                <div className="card-tags">
                  {cat.tags.map((t) => (
                    <span key={t} className="tag">{t}</span>
                  ))}
                </div>
              )}
              <span className="tag" style={{ marginTop: 8 }}>Pending</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
