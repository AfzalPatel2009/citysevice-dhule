import { useEffect, useMemo, useState } from 'react'
import {
  RecaptchaVerifier, signInWithPhoneNumber, onAuthStateChanged,
  signOut, getIdTokenResult
} from 'firebase/auth'
import {
  collection, doc, onSnapshot, query, orderBy, limit, updateDoc,
  getDocs, where, serverTimestamp
} from 'firebase/firestore'
import {
  Activity, ArrowDownRight, ArrowUpRight, Bell, Check, CheckCircle2,
  ChevronDown, ClipboardList, Clock3, LayoutDashboard, LogOut, Menu,
  Search, ShieldCheck, ShoppingBag, Store, Users, X, XCircle, RefreshCw,
  Phone, LockKeyhole, MapPin, CalendarDays, CircleUserRound, LoaderCircle
} from 'lucide-react'
import {
  Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis
} from 'recharts'
import { auth, db, usersCollection, requestsCollection } from './firebase'

const nav = [
  { id: 'overview', label: 'Overview', icon: LayoutDashboard },
  { id: 'retailers', label: 'Retailers', icon: Store },
  { id: 'customers', label: 'Customers', icon: Users },
  { id: 'requests', label: 'Service requests', icon: ClipboardList },
]
const fmtDate = value => {
  if (!value) return '—'
  const date = value?.toDate ? value.toDate() : new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })
}
const getName = u => u.name || u.fullName || u.displayName || u.shopName || 'Unnamed user'
const getPhone = u => u.phoneNumber || u.mobile || u.phone || '—'
const getStatus = u => String(u.status || (u.active === true ? 'active' : u.active === false ? 'inactive' : 'pending')).toLowerCase()

function App() {
  const [user, setUser] = useState(null)
  const [authReady, setAuthReady] = useState(false)
  const [adminAllowed, setAdminAllowed] = useState(false)
  const [authError, setAuthError] = useState('')
  const [phone, setPhone] = useState('+91')
  const [otp, setOtp] = useState('')
  const [confirmation, setConfirmation] = useState(null)
  const [sending, setSending] = useState(false)
  const [verifying, setVerifying] = useState(false)
  const [captcha, setCaptcha] = useState(null)
  const [section, setSection] = useState('overview')
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [users, setUsers] = useState([])
  const [requests, setRequests] = useState([])
  const [dataError, setDataError] = useState('')
  const [search, setSearch] = useState('')
  const [busyId, setBusyId] = useState('')
  const [toast, setToast] = useState('')

  useEffect(() => onAuthStateChanged(auth, async current => {
    setUser(current)
    setAuthReady(true)
    setAdminAllowed(false)
    if (current) {
      try {
        const token = await getIdTokenResult(current, true)
        if (token.claims.admin === true) setAdminAllowed(true)
        else {
          setAuthError('Is account ko admin access nahi mila hai. Admin custom claim assign karna hoga.')
          await signOut(auth)
        }
      } catch (e) {
        setAuthError(e.message || 'Admin verification failed.')
        await signOut(auth)
      }
    }
  }), [])

  useEffect(() => {
    if (!user || !adminAllowed) return
    const stopUsers = onSnapshot(collection(db, usersCollection), snap => {
      setUsers(snap.docs.map(d => ({ id: d.id, ...d.data() })))
      setDataError('')
    }, e => setDataError(e.message))
    const stopRequests = onSnapshot(collection(db, requestsCollection), snap => {
      setRequests(snap.docs.map(d => ({ id: d.id, ...d.data() })))
    }, e => setDataError(e.message))
    return () => { stopUsers(); stopRequests() }
  }, [user, adminAllowed])

  useEffect(() => {
    if (!toast) return
    const t = setTimeout(() => setToast(''), 3200)
    return () => clearTimeout(t)
  }, [toast])

  const retailers = useMemo(() => users.filter(u => u.role === 'retailer'), [users])
  const customers = useMemo(() => users.filter(u => u.role === 'customer' || u.role === 'user'), [users])
  const pendingRetailers = retailers.filter(u => ['pending', 'inactive'].includes(getStatus(u)) && u.active !== true)
  const pendingRequests = requests.filter(r => ['pending', 'new', 'requested'].includes(String(r.status || 'pending').toLowerCase()))
  const filtered = useMemo(() => {
    const list = section === 'retailers' ? retailers : section === 'customers' ? customers : requests
    const q = search.trim().toLowerCase()
    if (!q) return list
    return list.filter(x => [getName(x), getPhone(x), x.id, x.service, x.address, x.status].filter(Boolean).join(' ').toLowerCase().includes(q))
  }, [section, retailers, customers, requests, search])

  async function sendOtp(e) {
    e.preventDefault()
    setAuthError('')
    if (!/^\+\d{10,15}$/.test(phone.replace(/\s/g, ''))) {
      setAuthError('Country code ke saath valid mobile number likho. Example: +919876543210')
      return
    }
    try {
      setSending(true)
      let verifier = captcha
      if (!verifier) {
        verifier = new RecaptchaVerifier(auth, 'recaptcha-container', { size: 'normal' })
        setCaptcha(verifier)
      }
      const result = await signInWithPhoneNumber(auth, phone.replace(/\s/g, ''), verifier)
      setConfirmation(result)
      setToast('OTP bhej diya gaya hai.')
    } catch (e) {
      setAuthError(e.message || 'OTP send nahi ho paya.')
      if (captcha) { try { captcha.clear() } catch {} ; setCaptcha(null) }
    } finally { setSending(false) }
  }

  async function verifyOtp(e) {
    e.preventDefault()
    if (!confirmation || otp.trim().length < 6) return
    try {
      setVerifying(true)
      const result = await confirmation.confirm(otp.trim())
      const token = await getIdTokenResult(result.user, true)
      if (token.claims.admin !== true) {
        await signOut(auth)
        setAuthError('Login ho gaya, lekin is number ko admin permission nahi di gayi hai.')
      } else {
        setAdminAllowed(true)
        setAuthError('')
      }
    } catch (e) { setAuthError(e.message || 'OTP verify nahi hua.') }
    finally { setVerifying(false) }
  }

  async function changeRetailer(id, approve) {
    try {
      setBusyId(id)
      await updateDoc(doc(db, usersCollection, id), {
        active: approve,
        status: approve ? 'approved' : 'rejected',
        updatedAt: serverTimestamp()
      })
      setToast(approve ? 'Retailer approved.' : 'Retailer rejected.')
    } catch (e) { setToast(`Update failed: ${e.message}`) }
    finally { setBusyId('') }
  }

  async function changeRequest(id, status) {
    try {
      setBusyId(id)
      await updateDoc(doc(db, requestsCollection, id), { status, updatedAt: serverTimestamp() })
      setToast(`Request marked ${status}.`)
    } catch (e) { setToast(`Update failed: ${e.message}`) }
    finally { setBusyId('') }
  }

  if (!authReady) return <div className="loading-screen"><div className="loader-mark">C</div><LoaderCircle className="spin" size={22}/><span>Secure dashboard loading…</span></div>
  if (!user || !adminAllowed) return <Login phone={phone} setPhone={setPhone} otp={otp} setOtp={setOtp} confirmation={confirmation} sendOtp={sendOtp} verifyOtp={verifyOtp} sending={sending} verifying={verifying} error={authError} onReset={() => { setConfirmation(null); setOtp(''); setAuthError('') }} />

  return <div className="app-shell">
    {sidebarOpen && <div className="mobile-scrim" onClick={() => setSidebarOpen(false)} />}
    <aside className={`sidebar ${sidebarOpen ? 'open' : ''}`}>
      <div className="brand"><div className="brand-icon">C</div><div><strong>CitySeva</strong><span>DHULE · ADMIN</span></div><button className="icon-btn mobile-close" onClick={() => setSidebarOpen(false)}><X size={18}/></button></div>
      <div className="workspace"><span className="workspace-dot"/><div><b>CitySeva Dhule</b><small>Administration</small></div><ChevronDown size={15}/></div>
      <div className="nav-caption">WORKSPACE</div>
      <nav>{nav.map(item => <button key={item.id} className={`nav-link ${section === item.id ? 'active' : ''}`} onClick={() => { setSection(item.id); setSearch(''); setSidebarOpen(false) }}><item.icon size={18}/><span>{item.label}</span>{item.id === 'retailers' && pendingRetailers.length > 0 && <em>{pendingRetailers.length}</em>}{item.id === 'requests' && pendingRequests.length > 0 && <em>{pendingRequests.length}</em>}</button>)}</nav>
      <div className="sidebar-bottom"><div className="secure-note"><ShieldCheck size={17}/><div><b>Secure workspace</b><small>Admin verified session</small></div></div><button className="nav-link logout" onClick={() => signOut(auth)}><LogOut size={18}/><span>Sign out</span></button></div>
    </aside>
    <main className="main-area">
      <header className="topbar"><button className="icon-btn mobile-menu" onClick={() => setSidebarOpen(true)}><Menu/></button><div className="breadcrumb">Workspace <span>/</span> <b>{nav.find(n => n.id === section)?.label || 'Overview'}</b></div><div className="top-actions"><span className="live-indicator"><i/> Live data</span><button className="icon-btn notification" title="Notifications"><Bell size={18}/></button><div className="avatar">A</div><div className="admin-meta"><b>Administrator</b><small>{user.phoneNumber || user.email}</small></div></div></header>
      <div className="content">
        {dataError && <div className="error-banner"><XCircle size={17}/>{dataError}</div>}
        {section === 'overview' && <Overview users={users} retailers={retailers} customers={customers} requests={requests} pendingRetailers={pendingRetailers} pendingRequests={pendingRequests} setSection={setSection}/>}
        {section !== 'overview' && <Management section={section} items={filtered} search={search} setSearch={setSearch} busyId={busyId} onRetailer={changeRetailer} onRequest={changeRequest}/>}
        <footer>© {new Date().getFullYear()} CitySeva Dhule <span>•</span> Admin Console <span className="footer-right"><ShieldCheck size={13}/> Protected by Firebase Authentication</span></footer>
      </div>
    </main>
    {toast && <div className="toast"><CheckCircle2 size={17}/>{toast}</div>}
  </div>
}

function Login({ phone, setPhone, otp, setOtp, confirmation, sendOtp, verifyOtp, sending, verifying, error, onReset }) {
  return <div className="login-page"><div className="login-decoration deco-one"/><div className="login-decoration deco-two"/>
    <div className="login-brand"><div className="brand-icon">C</div><div><strong>CitySeva</strong><span>DHULE · ADMIN</span></div></div>
    <div className="login-card"><div className="login-icon"><ShieldCheck size={25}/></div><div className="eyebrow">PRIVATE ADMINISTRATOR ACCESS</div><h1>Welcome back.</h1><p>Sign in to manage your CitySeva operations securely.</p>
      {!confirmation ? <form onSubmit={sendOtp}><label>Admin mobile number</label><div className="phone-field"><Phone size={17}/><input value={phone} onChange={e => setPhone(e.target.value)} placeholder="+919876543210" autoComplete="tel" /></div><div id="recaptcha-container" className="recaptcha-slot"/><button className="primary-btn" disabled={sending}>{sending ? <LoaderCircle className="spin" size={18}/> : null}Send secure OTP <span>→</span></button></form>
      : <form onSubmit={verifyOtp}><label>6-digit verification code</label><div className="otp-field"><LockKeyhole size={17}/><input value={otp} onChange={e => setOtp(e.target.value.replace(/\D/g,'').slice(0,6))} placeholder="Enter OTP" inputMode="numeric" autoComplete="one-time-code"/></div><button className="primary-btn" disabled={verifying || otp.length !== 6}>{verifying ? <LoaderCircle className="spin" size={18}/> : null}Verify & continue <span>→</span></button><button type="button" className="text-btn" onClick={onReset}>Change mobile number</button></form>}
      {error && <div className="login-error"><XCircle size={16}/>{error}</div>}
      <div className="login-foot"><ShieldCheck size={15}/> Only authorized administrators can enter</div>
    </div><div className="login-bottom">CITYSEVA DHULE <span>·</span> OPERATIONS CONSOLE</div>
  </div>
}

function Overview({ users, retailers, customers, requests, pendingRetailers, pendingRequests, setSection }) {
  const approved = retailers.filter(r => r.active === true || getStatus(r) === 'approved').length
  const chart = useMemo(() => {
    const now = new Date()
    return Array.from({ length: 7 }, (_, i) => {
      const d = new Date(now); d.setDate(now.getDate() - (6 - i))
      const count = requests.filter(r => {
        const v = r.createdAt?.toDate ? r.createdAt.toDate() : r.createdAt ? new Date(r.createdAt) : null
        return v && v.toDateString() === d.toDateString()
      }).length
      return { day: d.toLocaleDateString('en-IN', { weekday: 'short' }), requests: count }
    })
  }, [requests])
  const cards = [
    { label: 'Total customers', value: customers.length, icon: Users, tone: 'blue', note: 'Registered accounts' },
    { label: 'Total retailers', value: retailers.length, icon: Store, tone: 'violet', note: `${approved} approved` },
    { label: 'Service requests', value: requests.length, icon: ShoppingBag, tone: 'green', note: 'All-time requests' },
    { label: 'Pending action', value: pendingRetailers.length + pendingRequests.length, icon: Clock3, tone: 'amber', note: 'Needs your attention' },
  ]
  return <><div className="page-heading"><div><div className="eyebrow">OVERVIEW</div><h1>Good day, Admin <span>✦</span></h1><p>Here’s what’s happening across your CitySeva network.</p></div><div className="date-chip"><CalendarDays size={16}/>{new Date().toLocaleDateString('en-IN', { weekday: 'short', day: '2-digit', month: 'short', year: 'numeric' })}</div></div>
    <div className="stat-grid">{cards.map(c => <div className="stat-card" key={c.label}><div className={`stat-icon ${c.tone}`}><c.icon size={20}/></div><div className="stat-label">{c.label}</div><div className="stat-value">{c.value.toLocaleString('en-IN')}</div><div className="stat-note">{c.note}</div></div>)}</div>
    <div className="overview-grid"><section className="panel chart-panel"><div className="panel-head"><div><h2>Request activity</h2><p>Daily service requests · last 7 days</p></div><span className="panel-tag"><Activity size={14}/> Live</span></div><div className="chart-wrap"><ResponsiveContainer width="100%" height="100%"><AreaChart data={chart} margin={{ top: 12, right: 10, left: -18, bottom: 0 }}><defs><linearGradient id="requestGradient" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="#5b8def" stopOpacity={0.24}/><stop offset="100%" stopColor="#5b8def" stopOpacity={0.01}/></linearGradient></defs><CartesianGrid stroke="#eef1f6" vertical={false}/><XAxis dataKey="day" axisLine={false} tickLine={false} tick={{ fill: '#98a2b3', fontSize: 12 }}/><YAxis allowDecimals={false} axisLine={false} tickLine={false} tick={{ fill: '#98a2b3', fontSize: 11 }}/><Tooltip contentStyle={{ borderRadius: 12, border: '1px solid #edf0f5', boxShadow: '0 8px 24px #10182812' }}/><Area type="monotone" dataKey="requests" stroke="#477de1" strokeWidth={2.5} fill="url(#requestGradient)"/></AreaChart></ResponsiveContainer></div></section>
      <section className="panel attention-panel"><div className="panel-head"><div><h2>Needs attention</h2><p>Items waiting for your action</p></div><span className="attention-count">{pendingRetailers.length + pendingRequests.length}</span></div>
        <button className="attention-row" onClick={() => setSection('retailers')}><span className="attention-icon amber"><Store size={18}/></span><span><b>Retailer approvals</b><small>Review new partner applications</small></span><strong>{pendingRetailers.length}</strong></button>
        <button className="attention-row" onClick={() => setSection('requests')}><span className="attention-icon blue"><ClipboardList size={18}/></span><span><b>Service requests</b><small>Review new customer requests</small></span><strong>{pendingRequests.length}</strong></button>
        <div className="attention-tip"><ShieldCheck size={16}/><span>All changes are restricted to verified admin accounts.</span></div>
      </section></div>
    <section className="panel quick-panel"><div className="panel-head"><div><h2>Quick overview</h2><p>Current platform snapshot</p></div></div><div className="quick-grid"><div><span>Retailer approval rate</span><b>{retailers.length ? Math.round(approved / retailers.length * 100) : 0}%</b></div><div><span>Pending retailers</span><b>{pendingRetailers.length}</b></div><div><span>Open requests</span><b>{pendingRequests.length}</b></div><div><span>Total registered users</span><b>{users.length}</b></div></div></section>
  </>
}

function Management({ section, items, search, setSearch, busyId, onRetailer, onRequest }) {
  const isRetailer = section === 'retailers', isCustomer = section === 'customers'
  const title = isRetailer ? 'Retailer management' : isCustomer ? 'Customer directory' : 'Service requests'
  const subtitle = isRetailer ? 'Review applications and manage your service partners.' : isCustomer ? 'View registered customers and their contact details.' : 'Track incoming service requests from customers.'
  return <><div className="page-heading"><div><div className="eyebrow">MANAGEMENT</div><h1>{title}</h1><p>{subtitle}</p></div><div className="total-chip">{items.length} records</div></div>
    <section className="panel table-panel"><div className="table-toolbar"><div><h2>{isRetailer ? 'All retailers' : isCustomer ? 'All customers' : 'All requests'}</h2><p>Live data from your Firebase database</p></div><div className="search-box"><Search size={16}/><input placeholder="Search records…" value={search} onChange={e => setSearch(e.target.value)}/></div></div>
      <div className="table-scroll"><table><thead><tr>{isRetailer ? <><th>RETAILER</th><th>PHONE</th><th>STATUS</th><th>JOINED</th><th>ACTION</th></> : isCustomer ? <><th>CUSTOMER</th><th>PHONE</th><th>ADDRESS</th><th>JOINED</th></> : <><th>REQUEST</th><th>CUSTOMER / PHONE</th><th>SERVICE</th><th>ADDRESS</th><th>STATUS</th><th>ACTION</th></>}</tr></thead><tbody>
        {items.map(x => <tr key={x.id}>{isRetailer ? <><td><div className="person-cell"><div className="table-avatar violet">{getName(x).slice(0,1).toUpperCase()}</div><div><b>{getName(x)}</b><small>{x.shopName || `ID: ${x.id.slice(0,9)}`}</small></div></div></td><td>{getPhone(x)}</td><td><Status status={getStatus(x)}/></td><td>{fmtDate(x.createdAt)}</td><td><div className="action-group">{x.active === true || getStatus(x) === 'approved' ? <button className="small-btn danger" disabled={busyId === x.id} onClick={() => onRetailer(x.id, false)}>Deactivate</button> : <><button className="small-btn approve" disabled={busyId === x.id} onClick={() => onRetailer(x.id, true)}><Check size={14}/> Approve</button><button className="small-btn danger" disabled={busyId === x.id} onClick={() => onRetailer(x.id, false)}><X size={14}/> Reject</button></>}</div></td></> : isCustomer ? <><td><div className="person-cell"><div className="table-avatar blue">{getName(x).slice(0,1).toUpperCase()}</div><div><b>{getName(x)}</b><small>ID: {x.id.slice(0,10)}</small></div></div></td><td>{getPhone(x)}</td><td className="muted-cell">{x.address || x.location || '—'}</td><td>{fmtDate(x.createdAt)}</td></> : <><td><b className="request-id">#{x.id.slice(0,8)}</b><small className="cell-sub">{fmtDate(x.createdAt)}</small></td><td><div className="person-cell"><div className="table-avatar blue">{getName(x).slice(0,1).toUpperCase()}</div><div><b>{getName(x)}</b><small>{getPhone(x)}</small></div></div></td><td>{x.service || x.serviceName || x.category || '—'}</td><td className="muted-cell">{x.address || x.location || '—'}</td><td><Status status={getStatus(x)}/></td><td><select className="status-select" value={x.status || 'pending'} disabled={busyId === x.id} onChange={e => onRequest(x.id, e.target.value)}><option value="pending">Pending</option><option value="accepted">Accepted</option><option value="in_progress">In progress</option><option value="completed">Completed</option><option value="cancelled">Cancelled</option></select></td></>}</tr>)}
        {!items.length && <tr><td colSpan="6"><div className="empty-state"><div><Search size={22}/></div><b>No records found</b><span>Records will appear here when they are available in Firestore.</span></div></td></tr>}
      </tbody></table></div>
    </section>
  </>
}
function Status({ status }) {
  const s = status || 'pending'
  const cls = ['approved','active','completed','accepted'].includes(s) ? 'success' : ['rejected','inactive','cancelled'].includes(s) ? 'danger' : s === 'in_progress' ? 'info' : 'warning'
  return <span className={`status-pill ${cls}`}><i/>{s.replaceAll('_',' ')}</span>
}
