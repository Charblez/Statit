import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createCategory } from '../api';
import { cropImageFileToSquare } from '../utils/imageCrop';

export default function CreateCategoryPage({ currentUser }) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [units, setUnits] = useState('');
  const [tags, setTags] = useState('');
  const [sortOrder, setSortOrder] = useState(true);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const [lowerLimit, setLowerLimit] = useState('');
  const [upperLimit, setUpperLimit] = useState('');
  const [imageData, setImageData] = useState('');

  const handleImageChange = async (e) => {
    const file = e.target.files?.[0];
    if (!file) {
      setImageData('');
      return;
    }

    try {
      setError('');
      const croppedImage = await cropImageFileToSquare(file);
      setImageData(croppedImage);
    } catch (err) {
      setImageData('');
      setError(err.message || 'Failed to process image.');
    }
  };

const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    setLoading(true);

    try {
      if (name.length > 70) {
        setError('Category name must be 70 characters or less.');
        setLoading(false);
        return;
      }

        if (units.length > 30) {
        setError('Units must be 30 characters or less.');
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

      const created = await createCategory({
        name,
        description: description || null,
        units,
        tags: tagList,
        sort_order: sortOrder,
        founding_username: currentUser.username,
        lower_limit: parseFloat(lowerLimit), // <-- Changed
        upper_limit: parseFloat(upperLimit), // <-- Changed
        image_data: imageData || null,
      });

      sessionStorage.removeItem('categoriesCache');

      if (created.live) {
        navigate(`/category/${created.categoryId}`);
        return;
      }

      if (currentUser.admin) {
        navigate('/admin');
        return;
      }

      setMessage(created.message || 'Category submitted for admin approval.');
      setName('');
      setDescription('');
      setUnits('');
      setTags('');
      setLowerLimit('');
      setUpperLimit('');
      setImageData('');
      setSortOrder(true);
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
        {message && (
          <div className="status-banner">{message}</div>
        )}

        <div className="panel">
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label>Category Name</label>
              <input
                className="input"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Push Ups (max 70 chars)"
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
                placeholder="e.g. WPM, kg, seconds (max 30 chars)"
                maxLength={30}
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
              <label>Category Image</label>
              <input
                className="input"
                type="file"
                accept="image/*"
                onChange={handleImageChange}
              />
              {imageData && (
                <img className="image-upload-preview" src={imageData} alt="Category preview" />
              )}
            </div>

            <div className="form-group">
              <label>Lower Limit (Required)</label>
              <input
                className="input"
                type="number"
                step="any" /* Allows negatives and decimals */
                value={lowerLimit}
                onChange={(e) => setLowerLimit(e.target.value)}
                placeholder="e.g. -1000"
                required
              />
            </div>

            <div className="form-group">
              <label>Upper Limit (Required)</label>
              <input
                className="input"
                type="number"
                step="any"
                value={upperLimit}
                onChange={(e) => setUpperLimit(e.target.value)}
                placeholder="e.g. 1000"
                required
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
              {loading ? 'Submitting...' : 'Submit for Approval'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
