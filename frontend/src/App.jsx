import { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Link, useParams, useNavigate, Navigate } from 'react-router-dom';

// --- HELPERS ---
const getStorage = (key, defaultValue) => {
  const saved = localStorage.getItem(key);
  return saved ? JSON.parse(saved) : defaultValue;
};

const ADMIN_USERNAMES = [
  "admin_Coleslaw",
  "admin_Fries_Pikachu",
  "admin_Toast",
  "admin_Chicken"
];

const isAdmin = (user) => user && ADMIN_USERNAMES.includes(user.username);

// --- SHARED STYLES ---
const pageWrapperStyle = {
  display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
  minHeight: '100vh', width: '100vw', fontFamily: 'sans-serif', padding: '20px',
  paddingTop: '100px', boxSizing: 'border-box', textAlign: 'center', position: 'relative',
  backgroundColor: '#fdfdfd'
};

const topNavStyle = {
  position: 'fixed', top: 0, left: 0, width: '100vw', height: '75px', 
  backgroundColor: '#fff', boxShadow: '0 2px 10px rgba(0,0,0,0.1)', 
  display: 'flex', justifyContent: 'space-between', alignItems: 'center', 
  padding: '0 30px', boxSizing: 'border-box', zIndex: 1000
};

const logoStyle = { height: '50px', width: 'auto', cursor: 'pointer' };
const navLinksStyle = { display: 'flex', gap: '15px', alignItems: 'center' };
const backLinkStyle = { position: 'absolute', top: '100px', left: '20px', textDecoration: 'none', color: '#8b5cf6', fontWeight: 'bold', fontSize: '1.1rem', zIndex: 10 };
const inputStyle = { padding: '12px', borderRadius: '8px', border: '1px solid #ccc', width: '280px', marginBottom: '15px', fontSize: '1rem', boxSizing: 'border-box' };

const mainButtonStyle = { 
  padding: '15px 20px', cursor: 'pointer', borderRadius: '8px', border: '2px solid #8b5cf6',
  backgroundColor: '#fff', fontSize: '1.4rem', width: '380px', fontWeight: 'bold', transition: '0.2s', color: '#333'
};

const smallButtonStyle = {
  padding: '10px 18px', cursor: 'pointer', borderRadius: '6px', border: '2px solid #8b5cf6',
  backgroundColor: '#fff', fontSize: '1rem', fontWeight: 'bold', color: '#333', transition: '0.2s'
};

const funTitleStyle = {
  fontSize: '3.2rem', marginBottom: '40px', color: '#2c3e50',
  fontFamily: '"Comic Sans MS", "Chalkboard SE", "Marker Felt", cursive'
};

// --- MAIN APP ---
export default function App() {
  const defaultPresets = [
    { id: '1', name: "Wealth", description: "Total net worth", better: "large", unit: "$", type: "global", status: "approved", min: 0, max: 1000000000000 },
    { id: '2', name: "Health", description: "Overall health score", better: "large", unit: "pts", type: "global", status: "approved", min: 0, max: 100 },
    { id: '3', name: "Speed", description: "100m sprint time", better: "small", unit: "sec", type: "global", status: "approved", min: 5, max: 60 }
  ];

  const [categories, setCategories] = useState(() => getStorage('categories', defaultPresets));
  const [allStats, setAllStats] = useState(() => getStorage('allStats', {}));
  const [users, setUsers] = useState(() => getStorage('users', []));
  const [currentUser, setCurrentUser] = useState(null);

  useEffect(() => {
    localStorage.setItem('categories', JSON.stringify(categories));
    localStorage.setItem('allStats', JSON.stringify(allStats));
    localStorage.setItem('users', JSON.stringify(users));
  }, [categories, allStats, users]);

  return (
    <Router>
      <AppContent 
        categories={categories} setCategories={setCategories}
        allStats={allStats} setAllStats={setAllStats}
        users={users} setUsers={setUsers}
        currentUser={currentUser} setCurrentUser={setCurrentUser}
      />
    </Router>
  );
}

// --- APP CONTENT ---
const AppContent = ({ categories, setCategories, allStats, setAllStats, users, setUsers, currentUser, setCurrentUser }) => {
  const logout = () => setCurrentUser(null);
  const userIsAdmin = isAdmin(currentUser);

  return (
    <>
      <div style={topNavStyle}>
        <Link to="/">
          <img src="/logo.webp" alt="Global Ranking Logo" style={logoStyle} />
        </Link>

        <div style={navLinksStyle}>
          {userIsAdmin && (
            <Link to="/admin">
              <button className="nav-button" style={{ color: '#d97706' }}>Admin Panel</button>
            </Link>
          )}

          <Link to="/global">
            <button className="nav-button">Global Categories</button>
          </Link>
          
          {currentUser && (
            <Link to="/profile">
              <button className="nav-button">Profile</button>
            </Link>
          )}

          {currentUser ? (
            <button onClick={logout} className="nav-button">Log out</button>
          ) : (
            <Link to="/login">
              <button className="nav-button">Login</button>
            </Link>
          )}
        </div>
      </div>

      <Routes>
        <Route path="/" element={currentUser ? <Home /> : <Navigate to="/login" />} />
        <Route path="/login" element={currentUser ? <Navigate to="/" /> : <AuthPage mode="login" users={users} setCurrentUser={setCurrentUser} />} />
        <Route path="/signup" element={currentUser ? <Navigate to="/" /> : <AuthPage mode="signup" users={users} setUsers={setUsers} setCurrentUser={setCurrentUser} />} />
        <Route path="/profile" element={currentUser ? <ProfilePage currentUser={currentUser} setUsers={setUsers} setCurrentUser={setCurrentUser} /> : <Navigate to="/login" />} />
        
        {/* Admin Route */}
        <Route path="/admin" element={userIsAdmin ? <AdminPage categories={categories} setCategories={setCategories} /> : <Navigate to="/" />} />

        {/* Unlocked Routes */}
        <Route path="/global" element={<CategoryList title="Global Categories" categories={categories.filter(c => c.type === 'global' && c.status !== 'pending')} />} />
        <Route path="/created" element={currentUser ? <CategoryList title="Your Created Categories" categories={categories.filter(c => c.type !== 'global')} currentUser={currentUser} /> : <Navigate to="/login" />} />
        <Route path="/create" element={currentUser ? <CreateCategory setCategories={setCategories} currentUser={currentUser} /> : <Navigate to="/login" />} />
        <Route path="/ranking/:categoryId" element={<RankingPage categories={categories} allStats={allStats} setAllStats={setAllStats} currentUser={currentUser} />} />
      </Routes>
    </>
  );
};

// --- HOME PAGE ---
const Home = () => (
  <div style={pageWrapperStyle}>
    <h1 style={funTitleStyle}>WELCOME TO GLOBAL RANKING SYSTEM! 🏆</h1>
    
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px', marginBottom: '40px' }}>
      <Link to="/global"><button style={mainButtonStyle}>Global Category</button></Link>
      <Link to="/create"><button style={mainButtonStyle}>Create Your Own</button></Link>
      <Link to="/created"><button style={mainButtonStyle}>See Created Categories</button></Link>
    </div>
  </div>
);

// --- PROFILE PAGE ---
const ProfilePage = ({ currentUser, setUsers, setCurrentUser }) => {
  const toggleAnonymous = () => {
    const updatedUser = { ...currentUser, isAnonymous: !currentUser.isAnonymous };
    setCurrentUser(updatedUser);
    setUsers(prevUsers => prevUsers.map(u => u.username === updatedUser.username ? updatedUser : u));
  };

  return (
    <div style={pageWrapperStyle}>
      <Link to="/" style={backLinkStyle}>← Back to Main</Link>
      <h2 style={{ fontSize: '2.5rem', marginBottom: '30px' }}>Your Profile</h2>
      <div style={{ backgroundColor: '#fff', padding: '40px', borderRadius: '20px', boxShadow: '0 4px 15px rgba(0,0,0,0.1)', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '20px', minWidth: '350px' }}>
        <p style={{ fontSize: '1.4rem', margin: 0 }}><strong>Username:</strong> {currentUser.username}</p>
        {isAdmin(currentUser) && <span style={{ backgroundColor: '#d97706', color: '#fff', padding: '5px 10px', borderRadius: '5px', fontWeight: 'bold' }}>Admin</span>}
        <div style={{ borderTop: '1px solid #eee', width: '100%', margin: '10px 0' }}></div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
          <span style={{ fontSize: '1.2rem', fontWeight: 'bold' }}>Anonymous Mode:</span>
          <button 
            onClick={toggleAnonymous}
            style={{ ...smallButtonStyle, backgroundColor: currentUser.isAnonymous ? '#8b5cf6' : '#fff', color: currentUser.isAnonymous ? '#fff' : '#333', width: '80px' }}
          >
            {currentUser.isAnonymous ? 'ON' : 'OFF'}
          </button>
        </div>
        <p style={{ color: '#666', fontSize: '1rem', maxWidth: '280px', margin: 0 }}>
          {currentUser.isAnonymous ? "Your new entries will be hidden and appear as 'Anonymous'." : "Your new entries will publicly display your username."}
        </p>
      </div>
    </div>
  );
};

// --- ADMIN PAGE ---
const AdminPage = ({ categories, setCategories }) => {
  const pendingCategories = categories.filter(c => c.status === 'pending');

  const handleApprove = (id, updatedData) => {
    setCategories(prev => prev.map(cat => 
      cat.id === id ? { ...cat, ...updatedData, status: 'approved' } : cat
    ));
    alert("Category Approved!");
  };

  return (
    <div style={pageWrapperStyle}>
      <Link to="/" style={backLinkStyle}>← Back to Main</Link>
      <h2 style={{ fontSize: '2.5rem', marginBottom: '30px', color: '#d97706' }}>Admin Dashboard</h2>
      
      {pendingCategories.length === 0 ? (
        <p style={{ fontSize: '1.2rem' }}>No pending categories to review!</p>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px', width: '100%', maxWidth: '600px' }}>
          {pendingCategories.map(cat => (
            <AdminReviewCard key={cat.id} category={cat} onApprove={handleApprove} />
          ))}
        </div>
      )}
    </div>
  );
};

const AdminReviewCard = ({ category, onApprove }) => {
  const [editName, setEditName] = useState(category.name);
  const [editDesc, setEditDesc] = useState(category.description || '');
  const [editMin, setEditMin] = useState(category.min);
  const [editMax, setEditMax] = useState(category.max);

  return (
    <div style={{ backgroundColor: '#fff', padding: '20px', borderRadius: '15px', boxShadow: '0 4px 15px rgba(0,0,0,0.1)', textAlign: 'left', display: 'flex', flexDirection: 'column', gap: '10px' }}>
      <p style={{ margin: 0, fontWeight: 'bold', color: '#666' }}>Created by: {category.creator || 'Unknown'}</p>
      <input style={inputStyle} value={editName} onChange={e => setEditName(e.target.value)} placeholder="Category Name" />
      <input style={inputStyle} value={editDesc} onChange={e => setEditDesc(e.target.value)} placeholder="Description" />
      <div style={{ display: 'flex', gap: '10px' }}>
        <input style={{...inputStyle, width: '135px'}} type="number" value={editMin} onChange={e => setEditMin(e.target.value)} placeholder="Min" />
        <input style={{...inputStyle, width: '135px'}} type="number" value={editMax} onChange={e => setEditMax(e.target.value)} placeholder="Max" />
      </div>
      <p style={{ margin: 0, fontSize: '0.9rem', color: '#888' }}>Unit: {category.unit} | Better: {category.better}</p>
      
      <button 
        onClick={() => onApprove(category.id, { name: editName, description: editDesc, min: Number(editMin), max: Number(editMax) })}
        style={{ ...mainButtonStyle, width: '100%', backgroundColor: '#4CAF50', color: 'white', border: 'none', fontSize: '1.1rem', marginTop: '10px' }}
      >
        Approve Category
      </button>
    </div>
  );
};

// --- LOGIN / SIGNUP PAGE ---
const AuthPage = ({ mode, users, setUsers, setCurrentUser }) => {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const navigate = useNavigate();

  const handleAuth = (e) => {
    e.preventDefault();
    if (mode === 'signup') {
      if (users.find(u => u.username === username)) return alert("Username already taken!");
      const newUser = { username, password, isAnonymous: false };
      setUsers([...users, newUser]);
      setCurrentUser(newUser);
      navigate('/');
    } else {
      const user = users.find(u => u.username === username && u.password === password);
      if (user) {
        setCurrentUser(user);
        navigate('/');
      } else {
        alert("Incorrect username or password. Please try again.");
      }
    }
  };

  return (
    <div style={pageWrapperStyle}>
      <h1 style={{ ...funTitleStyle, marginBottom: '10px' }}>GLOBAL RANKING SYSTEM 🏆</h1>
      <h2 style={{ fontSize: '1.8rem', marginBottom: '40px', color: '#666' }}>
        {mode === 'login' ? 'Log in to continue' : 'Create an account'}
      </h2>

      <form onSubmit={handleAuth} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', backgroundColor: '#fff', padding: '40px', borderRadius: '20px', boxShadow: '0 4px 15px rgba(0,0,0,0.1)', marginBottom: '20px' }}>
        <input style={inputStyle} placeholder="Username" value={username} onChange={e => setUsername(e.target.value)} required />
        <input style={inputStyle} type="password" placeholder="Password" value={password} onChange={e => setPassword(e.target.value)} required />
        <button type="submit" style={{ ...mainButtonStyle, width: '280px', fontSize: '1.2rem', backgroundColor: '#8b5cf6', color: 'white' }}>
          {mode === 'login' ? 'Log in' : 'Create account'}
        </button>
      </form>

      {mode === 'login' ? (
        <p style={{ color: 'black' }}>Don't have an account? <Link to="/signup" style={{ color: '#8b5cf6', fontWeight: 'bold' }}>Create one here</Link></p>
      ) : (
        <p style={{ color: 'black' }}>Already have an account? <Link to="/login" style={{ color: '#8b5cf6', fontWeight: 'bold' }}>Log in here</Link></p>
      )}
    </div>
  );
};

// --- CATEGORY LIST PAGE ---
const CategoryList = ({ title, categories, currentUser }) => {
  // If we are looking at "Created Categories", filter to only show ones this user made or that are approved.
  const displayCategories = categories.filter(c => {
    if (c.status === 'approved') return true;
    if (c.status === 'pending' && currentUser && c.creator === currentUser.username) return true;
    return false;
  });

  return (
    <div style={pageWrapperStyle}>
      <Link to="/" style={backLinkStyle}>← Back to Main</Link>
      <h2 style={{ fontSize: '2rem', marginBottom: '30px' }}>{title}</h2>
      
      {displayCategories.length === 0 ? (
        <div style={{ fontSize: '1.2rem' }}>
          <p>No categories here yet.</p>
          {title.includes("Created") && <Link to="/create" style={{ color: '#8b5cf6' }}>Create one now!</Link>}
        </div>
      ) : (
        <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'center', gap: '20px', maxWidth: '900px' }}>
          {displayCategories.map(cat => (
            <Link key={cat.id} to={`/ranking/${cat.id}`} style={{ 
              width: '180px', height: '180px', border: '2px solid #8b5cf6', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', 
              textDecoration: 'none', color: 'black', borderRadius: '15px', backgroundColor: '#fff', boxShadow: '0 4px 10px rgba(0,0,0,0.05)', position: 'relative', padding: '10px', boxSizing: 'border-box'
            }}>
              {cat.status === 'pending' && <span style={{ position: 'absolute', top: '10px', fontSize: '0.8rem', backgroundColor: '#f59e0b', color: 'white', padding: '3px 8px', borderRadius: '10px' }}>Pending Admin Approval</span>}
              <span style={{ fontWeight: 'bold', fontSize: '1.1rem', marginTop: cat.status === 'pending' ? '15px' : '0' }}>{cat.name}</span>
              <span style={{ fontSize: '0.85rem', color: '#666', marginTop: '5px' }}>Min: {cat.min} | Max: {cat.max}</span>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
};

// --- CREATE CATEGORY PAGE ---
const CreateCategory = ({ setCategories, currentUser }) => {
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [better, setBetter] = useState("large");
  const [unit, setUnit] = useState("");
  const [min, setMin] = useState("");
  const [max, setMax] = useState("");
  const navigate = useNavigate();

  const handleCreate = (e) => {
    e.preventDefault();
    if (!name || !unit || min === "" || max === "") return;
    
    const minVal = Number(min);
    const maxVal = Number(max);

    if (maxVal <= minVal) {
      alert("Error: The maximum value MUST be greater than the minimum value!");
      return;
    }

    const newCategory = { 
      id: Date.now().toString(),
      name, 
      description,
      better, 
      unit, 
      type: "user",
      min: minVal,
      max: maxVal,
      creator: currentUser.username,
      status: isAdmin(currentUser) ? "approved" : "pending" 
    };

    setCategories(prev => [...prev, newCategory]);
    alert(isAdmin(currentUser) ? "Category created and approved!" : "Category created! Waiting for Admin approval.");
    navigate('/created');
  };

  return (
    <div style={pageWrapperStyle}>
      <Link to="/" style={backLinkStyle}>← Back</Link>
      <h2 style={{ fontSize: '2rem' }}>Design Your Ranking</h2>
      <form onSubmit={handleCreate} style={{ display: 'flex', flexDirection: 'column', gap: '20px', alignItems: 'center', backgroundColor: '#fff', padding: '40px', borderRadius: '20px', boxShadow: '0 10px 25px rgba(0,0,0,0.1)', width: '400px' }}>
        
        <div style={{ textAlign: 'left', width: '100%' }}>
          <label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>Category Name</label>
          <input style={{...inputStyle, width: '100%'}} placeholder="e.g. Typing Speed" value={name} onChange={e => setName(e.target.value)} required />
        </div>

        <div style={{ textAlign: 'left', width: '100%' }}>
          <label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>Description</label>
          <input style={{...inputStyle, width: '100%'}} placeholder="e.g. Words per minute on a standard test" value={description} onChange={e => setDescription(e.target.value)} required />
        </div>

        <div style={{ display: 'flex', gap: '15px', width: '100%' }}>
          <div style={{ textAlign: 'left', flex: 1 }}>
            <label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>Min Value</label>
            <input type="number" style={{...inputStyle, width: '100%'}} placeholder="0" value={min} onChange={e => setMin(e.target.value)} required />
          </div>
          <div style={{ textAlign: 'left', flex: 1 }}>
            <label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>Max Value</label>
            <input type="number" style={{...inputStyle, width: '100%'}} placeholder="300" value={max} onChange={e => setMax(e.target.value)} required />
          </div>
        </div>

        <div style={{ textAlign: 'left', width: '100%' }}>
          <label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>Measurement Unit</label>
          <input style={{...inputStyle, width: '100%'}} placeholder="e.g. WPM" value={unit} onChange={e => setUnit(e.target.value)} required />
        </div>

        <div style={{ width: '100%' }}>
          <p style={{ fontWeight: 'bold', marginBottom: '10px' }}>Which is better?</p>
          <div style={{ display: 'flex', gap: '10px' }}>
            <button type="button" onClick={() => setBetter('large')} style={{ ...mainButtonStyle, flex: 1, fontSize: '1rem', padding: '10px 15px', backgroundColor: better === 'large' ? '#8b5cf6' : '#fff', color: better === 'large' ? '#fff' : '#000' }}>Large Number</button>
            <button type="button" onClick={() => setBetter('small')} style={{ ...mainButtonStyle, flex: 1, fontSize: '1rem', padding: '10px 15px', backgroundColor: better === 'small' ? '#8b5cf6' : '#fff', color: better === 'small' ? '#fff' : '#000' }}>Small Number</button>
          </div>
        </div>

        <button type="submit" style={{ ...mainButtonStyle, width: '100%', backgroundColor: '#4CAF50', color: 'white', border: 'none', marginTop: '10px' }}>
          {isAdmin(currentUser) ? "Create Category" : "Submit for Approval"}
        </button>
      </form>
    </div>
  );
};

// --- RANKING PAGE ---
const RankingPage = ({ categories, allStats, setAllStats, currentUser }) => {
  const { categoryId } = useParams();
  const catInfo = categories.find(c => c.id === categoryId) || categories.find(c => c.name === categoryId); 
  
  // Guard clause if someone tries to access a pending category directly
  if (!catInfo || (catInfo.status === 'pending' && currentUser?.username !== catInfo.creator && !isAdmin(currentUser))) {
    return <div style={pageWrapperStyle}><h2>Category not found or pending approval.</h2><Link to="/">Go back</Link></div>;
  }

  const currentStats = allStats[catInfo.id] || [];
  
  const [val, setVal] = useState("");
  const [gender, setGender] = useState("Male");
  const [region, setRegion] = useState("North America");
  
  const [viewMode, setViewMode] = useState("Global"); 
  const displayName = currentUser?.isAnonymous ? "Anonymous" : currentUser?.username;

  const addEntry = (e) => {
    e.preventDefault();
    if (!val) return;

    const numVal = parseFloat(val);
    
    // Validation for min/max
    if (numVal < catInfo.min || numVal > catInfo.max) {
      alert(`Invalid! Your score must be between ${catInfo.min} and ${catInfo.max}.`);
      return;
    }
    
    const newEntry = { name: displayName, value: numVal, gender, region };
    const updated = [...currentStats, newEntry]; 
    
    setAllStats(prev => ({ ...prev, [catInfo.id]: updated }));
    setVal(""); 
  };

  const getRankDisplay = (i) => i === 0 ? "1st 🥇" : i === 1 ? "2nd 🥈" : i === 2 ? "3rd 🥉" : `${i + 1}th`;

  const renderTable = (title, statsToRender) => {
    const sorted = [...statsToRender]
      .sort((a, b) => catInfo.better === "large" ? b.value - a.value : a.value - b.value)
      .slice(0, 100);

    return (
      <div key={title} style={{ width: '100%', maxWidth: '400px', maxHeight: '500px', overflowY: 'auto', border: '1px solid #eee', borderRadius: '12px', backgroundColor: '#fff', boxShadow: '0 5px 15px rgba(0,0,0,0.05)', display: 'flex', flexDirection: 'column' }}>
        <h3 style={{ margin: '15px 0', fontSize: '1.4rem', color: '#8b5cf6' }}>{title} Table</h3>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '1.1rem' }}>
          <thead style={{ position: 'sticky', top: 0, backgroundColor: '#8b5cf6', color: 'white', zIndex: 1 }}>
            <tr><th style={{ padding: '12px' }}>Rank</th><th>{catInfo.unit || 'Score'}</th><th>Name</th></tr>
          </thead>
          <tbody>
            {sorted.length === 0 ? (
              <tr><td colSpan="3" style={{ padding: '20px', color: '#666' }}>No entries yet.</td></tr>
            ) : (
              sorted.map((stat, i) => (
                <tr key={i} style={{ backgroundColor: i < 3 ? '#fff9e6' : '#fff', borderBottom: '1px solid #eee' }}>
                  <td style={{ padding: '12px', fontWeight: i < 3 ? 'bold' : 'normal' }}>{getRankDisplay(i)}</td>
                  <td>{stat.value ?? "-"}</td>
                  <td style={{ fontWeight: i < 3 ? 'bold' : 'normal', fontStyle: stat.name === 'Anonymous' ? 'italic' : 'normal' }}>{stat.name ?? "-"}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    );
  };

  return (
    <div style={{ ...pageWrapperStyle, justifyContent: 'flex-start', paddingTop: '80px' }}>
      <Link to={catInfo.type === 'global' ? "/global" : "/created"} style={backLinkStyle}>← Back</Link>
      
      <div style={{ marginBottom: '20px' }}>
        <h2 style={{ textTransform: 'capitalize', fontSize: '2.5rem', margin: '0' }}>{catInfo.name} Rankings</h2>
        {catInfo.description && <p style={{ color: '#666', fontSize: '1.2rem', marginTop: '5px' }}>{catInfo.description}</p>}
        {catInfo.status === 'pending' && <p style={{ color: '#d97706', fontWeight: 'bold' }}>⚠️ Pending Admin Approval - Stats entered now may be wiped if rejected.</p>}
      </div>
      
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '30px', justifyContent: 'center', marginBottom: '40px' }}>
        
        {currentUser ? (
          <form onSubmit={addEntry} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '15px', backgroundColor: '#fff', padding: '25px', borderRadius: '15px', boxShadow: '0 4px 15px rgba(0,0,0,0.1)' }}>
            <h3 style={{ margin: 0, fontSize: '1.3rem' }}>Submit Your Stat</h3>
            <p style={{ margin: 0, fontSize: '0.9rem', color: '#888' }}>Allowed Range: {catInfo.min} to {catInfo.max}</p>
            
            <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
              <input type="number" value={val} onChange={e => setVal(e.target.value)} placeholder="0" style={{ width: '100px', height: '60px', textAlign: 'center', border: '2px solid #8b5cf6', fontSize: '24px', borderRadius: '10px' }} required />
              <span style={{ fontSize: '1.2rem', fontWeight: 'bold' }}>{catInfo.unit}</span>
            </div>

            <select style={{ ...inputStyle, marginBottom: '0', width: '220px' }} value={gender} onChange={e => setGender(e.target.value)}>
              <option value="Male">Male</option>
              <option value="Female">Female</option>
            </select>

            <select style={{ ...inputStyle, marginBottom: '0', width: '220px' }} value={region} onChange={e => setRegion(e.target.value)}>
              <option value="North America">North America</option>
              <option value="South America">South America</option>
              <option value="Europe">Europe</option>
              <option value="Africa">Africa</option>
              <option value="Asia">Asia</option>
              <option value="Australia/Oceania">Australia/Oceania</option>
              <option value="Antarctica">Antarctica</option>
            </select>

            <input style={{ ...inputStyle, width: '220px', textAlign: 'center', backgroundColor: '#f3f4f6', color: '#666', cursor: 'not-allowed', marginBottom: 0 }} value={displayName} readOnly title="Change this in your Profile" />
            
            <button type="submit" style={{ ...mainButtonStyle, width: '220px', fontSize: '1.1rem', backgroundColor: '#8b5cf6', color: 'white' }}>Add Entry</button>
          </form>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '15px', backgroundColor: '#fff', padding: '25px', borderRadius: '15px', boxShadow: '0 4px 15px rgba(0,0,0,0.1)', width: '270px', textAlign: 'center' }}>
            <h3 style={{ margin: 0, fontSize: '1.3rem' }}>Submit Your Stat</h3>
            <p style={{ color: '#666', margin: 0 }}>You must be logged in to add your score to the leaderboard.</p>
            <Link to="/login">
              <button style={{ ...mainButtonStyle, width: '220px', fontSize: '1.1rem', backgroundColor: '#8b5cf6', color: 'white', marginTop: '10px' }}>Log in to submit</button>
            </Link>
          </div>
        )}

        {/* VIEW MODE SELECTOR */}
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '15px', backgroundColor: '#fff', padding: '25px', borderRadius: '15px', boxShadow: '0 4px 15px rgba(0,0,0,0.1)' }}>
          <h3 style={{ margin: 0, fontSize: '1.3rem' }}>View Leaderboards</h3>
          <select style={{ ...inputStyle, border: '2px solid #8b5cf6', fontSize: '1.2rem', width: '220px', padding: '15px' }} value={viewMode} onChange={e => setViewMode(e.target.value)}>
            <option value="Global">🌎 Global Ranking</option>
            <option value="Gender">🚻 By Gender</option>
            <option value="Region">🗺️ By Region</option>
          </select>
          <p style={{ color: '#666', maxWidth: '200px', fontSize: '0.95rem' }}>
            Select a filter to dynamically split the leaderboards below!
          </p>
        </div>

      </div>

      {/* DYNAMIC TABLES CONTAINER */}
      <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'center', gap: '25px', width: '100%', maxWidth: '1400px' }}>
        {viewMode === 'Global' && renderTable('Global', currentStats)}
        
        {viewMode === 'Gender' && [
          renderTable('Male', currentStats.filter(s => s.gender === 'Male')),
          renderTable('Female', currentStats.filter(s => s.gender === 'Female'))
        ]}
        
        {viewMode === 'Region' && [
          renderTable('North America', currentStats.filter(s => s.region === 'North America')),
          renderTable('South America', currentStats.filter(s => s.region === 'South America')),
          renderTable('Europe', currentStats.filter(s => s.region === 'Europe')),
          renderTable('Africa', currentStats.filter(s => s.region === 'Africa')),
          renderTable('Asia', currentStats.filter(s => s.region === 'Asia')),
          renderTable('Australia/Oceania', currentStats.filter(s => s.region === 'Australia/Oceania')),
          renderTable('Antarctica', currentStats.filter(s => s.region === 'Antarctica'))
        ]}
      </div>
    </div>
  );
};