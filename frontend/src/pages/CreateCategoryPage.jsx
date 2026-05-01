import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createCategory } from '../api';

export default function CreateCategoryPage({ currentUser }) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [units, setUnits] = useState('');
  const [tags, setTags] = useState('');
  const [sortOrder, setSortOrder] = useState(true);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const [lowerLimit, setLowerLimit] = useState('');
  const [upperLimit, setUpperLimit] = useState('');

const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      if (name.length > 70) {
        setError('Category name must be 70 characters or less.');
        setLoading(false);
        return;
      }

      const tagList = tags
        .split(',')
        .map((t) => t.trim())
        .filter(Boolean);

      const overlongTags = tagList.filter((t) => t.length > 20);
      if (overlongTags.length > 0) {
        setError('Each tag must be 20 characters or less.');
        setLoading(false);
        return; 
      }

      await createCategory({
        name,
        description: description || null,
        units,
        tags: tagList,
        sort_order: sortOrder,
        founding_username: currentUser.username,
        lower_limit: lowerLimit !== '' ? parseFloat(lowerLimit) : null,
        upper_limit: upperLimit !== '' ? parseFloat(upperLimit) : null,
      });

      navigate('/');
    } catch (err) {
      setError(err.message || 'Failed to create category');
    } finally {
      setLoading(false);
    }
  };

  if (!currentUser) {
    return (
      <div className="page">
        <div className="empty-state">
          <h3>Login required</h3>
          <p>You must be logged in to create a category.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="page">
      <div className="create-form-wrapper">
        <h1 className="page-title">Create Category</h1>

        {error && <div className="error-banner">{error}</div>}

        <div className="panel">
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label>Category Name</label>
              <input
                className="input"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Typing Speed"
                maxLength={70}
                required
              />
            </div>

            <div className="form-group">
              <label>Description</label>
              <input
                className="input"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="What is this category about?"
              />
            </div>

            <div className="form-group">
              <label>Unit of Measurement</label>
              <input
                className="input"
                value={units}
                onChange={(e) => setUnits(e.target.value)}
                placeholder="e.g. WPM, kg, seconds"
                required
              />
            </div>

            <div className="form-group">
              <label>Tags (comma separated)</label>
              <input
                className="input"
                value={tags}
                onChange={(e) => setTags(e.target.value)}
                placeholder="e.g. fitness, speed (max 20 chars each)"
              />
            </div>

            <div className="form-group">
              <label>Lower Limit (Optional)</label>
              <input
                className="input"
                type="number"
                step="any" /* Allows negatives and decimals */
                value={lowerLimit}
                onChange={(e) => setLowerLimit(e.target.value)}
                placeholder="e.g. -100"
              />
            </div>

            <div className="form-group">
              <label>Upper Limit (Optional)</label>
              <input
                className="input"
                type="number"
                step="any"
                value={upperLimit}
                onChange={(e) => setUpperLimit(e.target.value)}
                placeholder="e.g. 1000"
              />
            </div>

            <div className="form-group">
              <label>Sort Order</label>
              <div className="sort-toggle">
                <button
                  type="button"
                  className={`btn btn-ghost ${sortOrder ? 'selected' : ''}`}
                  onClick={() => setSortOrder(true)}
                >
                  Higher is better
                </button>
                <button
                  type="button"
                  className={`btn btn-ghost ${!sortOrder ? 'selected' : ''}`}
                  onClick={() => setSortOrder(false)}
                >
                  Lower is better
                </button>
              </div>
            </div>


            <button type="submit" className="btn btn-primary btn-full" disabled={loading}>
              {loading ? 'Creating...' : 'Create Category'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
