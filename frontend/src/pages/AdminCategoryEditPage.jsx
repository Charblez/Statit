import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  adminGetCategory,
  adminUpdateCategory,
  adminApproveCategory,
  adminDeleteCategory,
} from '../api';
import { cropImageFileToSquare } from '../utils/imageCrop';

const getCategoryImage = (category) => category?.imageData || category?.image_data || '';

export default function AdminCategoryEditPage({ currentUser }) {
  const { categoryId } = useParams();
  const navigate = useNavigate();

  const [category, setCategory] = useState(null);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [units, setUnits] = useState('');
  const [tags, setTags] = useState('');
  const [sortOrder, setSortOrder] = useState(true);
  const [lowerLimit, setLowerLimit] = useState('');
  const [upperLimit, setUpperLimit] = useState('');
  const [imageData, setImageData] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  useEffect(() => {
    if (!currentUser?.admin) return;
    setLoading(true);
    adminGetCategory(categoryId)
      .then((cat) => {
        setCategory(cat);
        setName(cat.name || '');
        setDescription(cat.description || '');
        setUnits(cat.units || '');
        setTags((cat.tags || []).join(', '));
        setSortOrder(Boolean(cat.sortOrder));
        setLowerLimit(cat.lowerLimit ?? '');
        setUpperLimit(cat.upperLimit ?? '');
        setImageData(getCategoryImage(cat));
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [categoryId, currentUser]);

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

  if (loading) return <div className="page"><div className="loading">Loading...</div></div>;
  if (!category) return <div className="page"><div className="error-banner">{error || 'Category not found'}</div></div>;

  const buildPayload = () => {
    const tagList = tags.split(',').map((t) => t.trim()).filter(Boolean);
    return {
      name,
      description: description || null,
      units,
      tags: tagList,
      sort_order: sortOrder,
      founding_username: null,
      lower_limit: parseFloat(lowerLimit),
      upper_limit: parseFloat(upperLimit),
      image_data: imageData || null,
    };
  };

  const handleImageChange = async (e) => {
    const file = e.target.files?.[0];
    if (!file) {
      return;
    }

    try {
      setError('');
      const croppedImage = await cropImageFileToSquare(file);
      setImageData(croppedImage);
    } catch (err) {
      setError(err.message || 'Failed to process image.');
    }
  };

  const handleSave = async () => {
    setSaving(true);
    setError('');
    setMessage('');
    try {
      await adminUpdateCategory(categoryId, buildPayload());
      setMessage('Category updated.');
      sessionStorage.removeItem('categoriesCache');
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleApprove = async () => {
    if (!window.confirm('Are you sure?')) return;
    setSaving(true);
    setError('');
    setMessage('');
    try {
      await adminUpdateCategory(categoryId, buildPayload());
      await adminApproveCategory(categoryId);
      sessionStorage.removeItem('categoriesCache');
      navigate('/admin');
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!window.confirm('Are you sure?')) return;
    setSaving(true);
    setError('');
    try {
      await adminDeleteCategory(categoryId);
      sessionStorage.removeItem('categoriesCache');
      navigate('/admin');
    } catch (err) {
      setError(err.message);
      setSaving(false);
    }
  };

  return (
    <div className="page">
      <div className="create-form-wrapper">
        <h1 className="page-title">
          {category.live ? 'Edit Category' : 'Review Pending Category'}
        </h1>

        {error && <div className="error-banner">{error}</div>}
        {message && <div className="status-banner">{message}</div>}

        <div className="panel">
          <div className="form-group">
            <label>Category Name</label>
            <input className="input" value={name} onChange={(e) => setName(e.target.value)} maxLength={70} />
          </div>

          <div className="form-group">
            <label>Description</label>
            <input className="input" value={description} onChange={(e) => setDescription(e.target.value)} />
          </div>

          <div className="form-group">
            <label>Unit of Measurement</label>
            <input className="input" value={units} onChange={(e) => setUnits(e.target.value)} maxLength={30} />
          </div>

          <div className="form-group">
            <label>Tags (comma separated)</label>
            <input className="input" value={tags} onChange={(e) => setTags(e.target.value)} />
          </div>

          <div className="form-group">
            <label>Category Image</label>
            <input className="input" type="file" accept="image/*" onChange={handleImageChange} />
            {imageData && (
              <img className="image-upload-preview" src={imageData} alt="Category preview" />
            )}
          </div>

          <div className="form-group">
            <label>Lower Limit</label>
            <input className="input" type="number" step="any" value={lowerLimit} onChange={(e) => setLowerLimit(e.target.value)} />
          </div>

          <div className="form-group">
            <label>Upper Limit</label>
            <input className="input" type="number" step="any" value={upperLimit} onChange={(e) => setUpperLimit(e.target.value)} />
          </div>

          <div className="form-group">
            <label>Sort Order</label>
            <div className="sort-toggle">
              <button type="button" className={`btn btn-ghost ${sortOrder ? 'selected' : ''}`} onClick={() => setSortOrder(true)}>Higher is better</button>
              <button type="button" className={`btn btn-ghost ${!sortOrder ? 'selected' : ''}`} onClick={() => setSortOrder(false)}>Lower is better</button>
            </div>
          </div>

          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            <button className="btn btn-ghost" onClick={handleSave} disabled={saving}>Save Changes</button>
            {!category.live && (
              <button className="btn btn-primary" onClick={handleApprove} disabled={saving}>Save & Approve</button>
            )}
            <button className="btn btn-ghost" style={{ marginLeft: 'auto', color: 'crimson' }} onClick={handleDelete} disabled={saving}>Delete</button>
          </div>
        </div>
      </div>
    </div>
  );
}
