import { useState, useCallback, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import HomePage from './pages/HomePage';
import CategoryPage from './pages/CategoryPage';
import CreateCategoryPage from './pages/CreateCategoryPage';
import AuthPage from './pages/AuthPage';
import ProfilePage from './pages/ProfilePage';
import AdminDashboardPage from './pages/AdminDashboardPage';
import AdminCategoryEditPage from './pages/AdminCategoryEditPage';
import { getCategories, getUser } from './api';

const onlyPublicCategories = (categories) =>
  (Array.isArray(categories) ? categories : []).filter((category) => category.live !== false);

function Header({ currentUser, darkMode, onToggleDark }) {
  const location = useLocation();
  const navigate = useNavigate();
  const [searchValue, setSearchValue] = useState('');
  const [categories, setCategories] = useState(() => {
    const cached = sessionStorage.getItem('categoriesCache');
    return cached ? onlyPublicCategories(JSON.parse(cached)) : [];
  });
  const [searchFocused, setSearchFocused] = useState(false);

  useEffect(() => {
    if (categories.length > 0) return;

    getCategories(0, 100)
      .then((data) => {
        const list = onlyPublicCategories(data.categories);
        setCategories(list);
        sessionStorage.setItem('categoriesCache', JSON.stringify(list));
      })
      .catch(() => {
        setCategories([]);
      });
  }, [categories.length]);

  useEffect(() => {
    if (location.pathname !== '/') return;
    const searchParams = new URLSearchParams(location.search);
    const nextSearchValue = searchParams.get('search') || '';
    let cancelled = false;
    queueMicrotask(() => {
      if (!cancelled) setSearchValue(nextSearchValue);
    });
    return () => {
      cancelled = true;
    };
  }, [location.pathname, location.search]);

  const isActive = (path) => {
    if (path === '/' && location.pathname === '/') return true;
    if (path !== '/' && location.pathname.startsWith(path)) return true;
    return false;
  };

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

  const normalizedSearch = searchValue.trim();
  const searchResults = normalizedSearch
    ? categories
        .filter((category) => categoryMatchesSearch(category.name || '', normalizedSearch))
        .slice(0, 6)
    : [];

  const handleSearchSubmit = (event) => {
    event.preventDefault();
    const query = searchValue.trim();
    setSearchFocused(false);
    navigate(query ? `/?search=${encodeURIComponent(query)}` : '/');
  };

  const handleSearchChange = (event) => {
    const value = event.target.value;
    const query = value.trim();
    setSearchValue(value);

    if (location.pathname === '/') {
      navigate(query ? `/?search=${encodeURIComponent(query)}` : '/', { replace: true });
    }
  };

  const handleSearchResultClick = (categoryId) => {
    setSearchValue('');
    setSearchFocused(false);
    navigate(`/category/${categoryId}`);
  };

  return (
    <header className="header">
      <div className="header-left">
        <Link to="/" className="header-logo">
          <img src="/logo.webp" alt="Logo" />
          <span>StatIt</span>
        </Link>
        
        {/* LIGHT/DARK TOGGLE MOVED TO HEADER */}
        <label className="dark-toggle">
          <span>{darkMode ? 'Dark' : 'Light'}</span>
          <div className={`dark-toggle-track ${darkMode ? 'on' : ''}`} onClick={onToggleDark}>
            <div className="dark-toggle-thumb" />
          </div>
        </label>
      </div>

      <form className="header-search" onSubmit={handleSearchSubmit}>
        <input
          className="header-search-input"
          type="search"
          placeholder="Search"
          value={searchValue}
          onChange={handleSearchChange}
          onFocus={() => setSearchFocused(true)}
          onBlur={() => setTimeout(() => setSearchFocused(false), 120)}
          aria-label="Search categories"
        />
        {searchFocused && normalizedSearch && (
          <div className="search-results">
            {searchResults.length > 0 ? (
              searchResults.map((category) => (
                <button
                  key={category.categoryId}
                  className="search-result-item"
                  type="button"
                  onMouseDown={(event) => event.preventDefault()}
                  onClick={() => handleSearchResultClick(category.categoryId)}
                >
                  <span>{category.name}</span>
                  {category.units && <span className="search-result-meta">{category.units}</span>}
                </button>
              ))
            ) : (
              <div className="search-empty">No categories found</div>
            )}
          </div>
        )}
      </form>

      <nav className="header-nav">
        <Link to="/" className={`nav-link ${isActive('/') && !isActive('/category') && !isActive('/create') && !isActive('/profile') ? 'active' : ''}`}>
          Home
        </Link>

        {currentUser && (
          <Link to="/profile" className={`nav-link ${isActive('/profile') ? 'active' : ''}`}>
            Profile
          </Link>
        )}

        {currentUser?.admin && (
          <Link to="/admin" className={`nav-link ${isActive('/admin') ? 'active' : ''}`}>
            Admin
          </Link>
        )}

        {!currentUser && (
          <Link to="/login" className={`nav-link ${isActive('/login') || isActive('/signup') ? 'active' : ''}`}>
            Login
          </Link>
        )}
      </nav>
    </header>
  );
}

function AppContent() {
  const navigate = useNavigate();

  const [currentUser, setCurrentUser] = useState(() => {
    const saved = localStorage.getItem('currentUser');
    return saved ? JSON.parse(saved) : null;
  });

  const [darkMode, setDarkMode] = useState(() => {
    const saved = localStorage.getItem('darkMode');
    return saved === 'true';
  });

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', darkMode ? 'dark' : 'light');
    localStorage.setItem('darkMode', darkMode);
  }, [darkMode]);

  const persistCurrentUser = useCallback((userData) => {
    setCurrentUser(userData);
    if (userData) {
      localStorage.setItem('currentUser', JSON.stringify(userData));
    } else {
      localStorage.removeItem('currentUser');
    }
  }, []);

  const refreshCurrentUserByUsername = useCallback(async (username) => {
    if (!username) return;

    try {
      const freshUser = await getUser(username);
      persistCurrentUser(freshUser);
    } catch {
      persistCurrentUser(null);
    }
  }, [persistCurrentUser]);

  const currentUsername = currentUser?.username;

  useEffect(() => {
    if (!currentUsername) return;
    let cancelled = false;
    queueMicrotask(() => {
      if (!cancelled) refreshCurrentUserByUsername(currentUsername);
    });
    return () => {
      cancelled = true;
    };
  }, [currentUsername, refreshCurrentUserByUsername]);

  useEffect(() => {
    const handleFocus = () => refreshCurrentUserByUsername(currentUsername);
    window.addEventListener('focus', handleFocus);
    return () => window.removeEventListener('focus', handleFocus);
  }, [currentUsername, refreshCurrentUserByUsername]);

  const handleLogin = (userData) => {
    persistCurrentUser(userData);
  };

  const handleLogout = () => {
    persistCurrentUser(null);
    navigate('/login');
  };

  const toggleDark = () => setDarkMode((prev) => !prev);

  return (
    <>
      <Header currentUser={currentUser} darkMode={darkMode} onToggleDark={toggleDark} />

      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/category/:categoryId" element={<CategoryPage currentUser={currentUser} />} />
        <Route path="/create" element={currentUser ? <CreateCategoryPage currentUser={currentUser} /> : <Navigate to="/login" />} />
        <Route path="/profile" element={currentUser ? <ProfilePage currentUser={currentUser} onLogout={handleLogout} /> : <Navigate to="/login" />} />
        <Route path="/admin" element={currentUser?.admin ? <AdminDashboardPage currentUser={currentUser} /> : <Navigate to="/" />} />
        <Route path="/admin/category/:categoryId" element={currentUser?.admin ? <AdminCategoryEditPage currentUser={currentUser} /> : <Navigate to="/" />} />
        <Route path="/login" element={currentUser ? <Navigate to="/" /> : <AuthPage mode="login" onLogin={handleLogin} />} />
        <Route path="/signup" element={currentUser ? <Navigate to="/" /> : <AuthPage mode="signup" onLogin={handleLogin} />} />
      </Routes>
    </>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AppContent />
    </BrowserRouter>
  );
}
