import { useState, useEffect } from 'react';
import { getUserScores } from '../api';

export default function ProfilePage({ currentUser, onLogout }) {
  const [scores, setScores] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);

  useEffect(() => {
    if (!currentUser) return;
    let cancelled = false;

    queueMicrotask(() => {
      if (cancelled) return;
      setLoading(true);
      getUserScores(currentUser.username, page, 25)
        .then((data) => {
          if (cancelled) return;
          const allScores = data || [];

          const seen = new Map();
          allScores.forEach((s) => {
            const existing = seen.get(s.categoryId);
            if (!existing || new Date(s.submittedAt) > new Date(existing.submittedAt)) {
              seen.set(s.categoryId, s);
            }
          });
          const deduped = Array.from(seen.values()).sort(
            (a, b) => new Date(b.submittedAt) - new Date(a.submittedAt)
          );

          setScores(deduped);
          setHasMore(allScores.length === 25);
        })
        .catch(() => {
          if (!cancelled) setScores([]);
        })
        .finally(() => {
          if (!cancelled) setLoading(false);
        });
    });

    return () => {
      cancelled = true;
    };
  }, [currentUser, page]);

  if (!currentUser) {
    return (
      <div className="page">
        <div className="empty-state">
          <h3>Not logged in</h3>
          <p>Please log in to view your profile.</p>
        </div>
      </div>
    );
  }

  const demographics = currentUser.demographics || {};

  return (
    <div className="page">
      <h1 className="page-title">Profile</h1>

      <div className="profile-layout">
        <div className="panel">
          <div className="panel-title">Account Info{currentUser.admin ? ' (Admin)' : ''}</div>
          <div className="profile-info-item">
            <span className="profile-info-label">Username</span>
            <span>{currentUser.username}</span>
          </div>
          <div className="profile-info-item">
            <span className="profile-info-label">Email</span>
            <span>{currentUser.email || '-'}</span>
          </div>
          <div className="profile-info-item">
            <span className="profile-info-label">Birthday</span>
            <span>{currentUser.birthday ? new Date(currentUser.birthday).toLocaleDateString() : '-'}</span>
          </div>
          <div className="profile-info-item">
            <span className="profile-info-label">Member Since</span>
            <span>{currentUser.createdAt ? new Date(currentUser.createdAt).toLocaleDateString() : '-'}</span>
          </div>

          {Object.keys(demographics).length > 0 && (
            <>
              <div className="panel-title" style={{ marginTop: 20 }}>Demographics</div>
              {Object.entries(demographics).map(([key, value]) => (
                <div className="profile-info-item" key={key}>
                  <span className="profile-info-label" style={{ textTransform: 'capitalize' }}>{key}</span>
                  <span>{value}</span>
                </div>
              ))}
            </>
          )}

          <button className="btn btn-ghost btn-full" type="button" onClick={onLogout} style={{ marginTop: 20 }}>
            Log out
          </button>
        </div>

        <div className="panel">
          <div className="panel-title">Score History (Most Recent Per Category)</div>

          {loading ? (
            <div className="loading">Loading scores...</div>
          ) : scores.length === 0 ? (
            <div className="empty-state" style={{ padding: '24px 0' }}>
              <p>No scores submitted yet.</p>
            </div>
          ) : (
            <>
              <div style={{ overflowX: 'auto' }}>
                <table className="score-table">
                  <thead>
                    <tr>
                      <th>Category</th>
                      <th>Score</th>
                      <th>Anonymous</th>
                      <th>Submitted</th>
                    </tr>
                  </thead>
                  <tbody>
                    {scores.map((s) => (
                      <tr key={s.scoreId}>
                        <td style={{ fontWeight: 600 }}>{s.categoryName || '-'}</td>
                        <td className="score-cell">{s.score}</td>
                        <td>{s.anonymous ? 'Yes' : 'No'}</td>
                        <td style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                          {s.submittedAt ? new Date(s.submittedAt).toLocaleString() : '-'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="pagination">
                <button className="btn btn-ghost btn-sm" disabled={page === 0} onClick={() => setPage(page - 1)}>Prev</button>
                <span className="page-info">Page {page + 1}</span>
                <button className="btn btn-ghost btn-sm" disabled={!hasMore} onClick={() => setPage(page + 1)}>Next</button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
