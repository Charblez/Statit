import { useState, useEffect } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { getCategories } from '../api';

const categoryMatchesSearch = (categoryName, query) => {
  const name = categoryName.toLowerCase();
  const letters = query.toLowerCase().trim();
  let nameIndex = 0;

  for (const letter of letters) {
    nameIndex = name.indexOf(letter, nameIndex);
    if (nameIndex === -1) return false;
    nameIndex += 1;
  }

  return true;
};

const onlyPublicCategories = (categories) =>
  (Array.isArray(categories) ? categories : []).filter((category) => category.live !== false);

const getCategoryImage = (category) => category?.imageData || category?.image_data || '';

export default function HomePage() {
  const [categories, setCategories] = useState(() => {
    const cached = sessionStorage.getItem('categoriesCache');
    return cached ? onlyPublicCategories(JSON.parse(cached)) : [];
  });
  const [loading, setLoading] = useState(() => !sessionStorage.getItem('categoriesCache'));
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const searchQuery = (searchParams.get('search') || '').trim();
  const visibleCategories = searchQuery
    ? categories.filter((category) => categoryMatchesSearch(category.name || '', searchQuery))
    : categories;

  useEffect(() => {
    getCategories(0, 100)
      .then((data) => {
        const list = onlyPublicCategories(data.categories);
        setCategories(list);
        sessionStorage.setItem('categoriesCache', JSON.stringify(list));
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="page"><div className="loading">Loading categories...</div></div>;

  return (
    <div className="page">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <h1 className="page-title" style={{ marginBottom: 0 }}>
          {searchQuery ? `Categories matching "${searchQuery}"` : 'Categories'}
        </h1>
        <Link to="/create">
          <button className="btn btn-primary">+ New Category</button>
        </Link>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {visibleCategories.length === 0 ? (
        <div className="empty-state">
          <h3>{searchQuery ? 'No matching categories' : 'No categories yet'}</h3>
          <p>{searchQuery ? 'Try another search.' : 'Check back later for ranking categories.'}</p>
        </div>
      ) : (
        <div className="card-grid">
          {visibleCategories.map((cat) => {
            const image = getCategoryImage(cat);

            return (
              <div
                key={cat.categoryId}
                className={`category-card ${image ? 'has-image' : ''}`}
                style={image ? { '--category-image': `url("${image}")` } : undefined}
                onClick={() => navigate(`/category/${cat.categoryId}`)}
              >
                <h3>{cat.name}</h3>
                {cat.description && (
                  <p className="card-meta">{cat.description}</p>
                )}
                <p className="card-meta">
                  Unit: {cat.units} &middot; {cat.sortOrder ? 'Higher is better' : 'Lower is better'}
                </p>
                {cat.tags && cat.tags.length > 0 && (
                  <div className="card-tags">
                    {cat.tags.map((t) => (
                      <span key={t} className="tag">{t}</span>
                    ))}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
