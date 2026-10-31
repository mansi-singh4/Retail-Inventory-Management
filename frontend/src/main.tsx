import React, { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { Boxes, Package, Warehouse, Truck, ShoppingCart, RefreshCw, LogIn, Plus } from 'lucide-react';
import './styles.css';

type Row = Record<string, any>;
type Role = 'ADMIN' | 'MANAGER' | 'STAFF';
type User = { id: number; email: string; role: Role };
const roles: Role[] = ['ADMIN', 'MANAGER', 'STAFF'];
const api = import.meta.env.VITE_API_URL || 'http://localhost:8090';
const tokenKey = 'rims-token';
const userKey = 'rims-user';

async function request(path: string, options: RequestInit = {}) {
  const headers = new Headers(options.headers);
  headers.set('Content-Type', 'application/json');
  const token = localStorage.getItem(tokenKey);
  if (token) headers.set('Authorization', `Bearer ${token}`);
  const response = await fetch(api + path, { ...options, headers });
  if (!response.ok) throw new Error(await response.text() || `${response.status} request failed`);
  return response.status === 204 ? null : response.json();
}

const sections = ['Dashboard', 'Categories', 'Products', 'Warehouses', 'Inventory', 'Suppliers', 'Purchase orders', 'Customer orders', 'Users'];
const sectionRoutes: Record<string, string> = { Dashboard: '/dashboard', Categories: '/categories', Products: '/products', Warehouses: '/warehouses', Inventory: '/inventory', Suppliers: '/suppliers', 'Purchase orders': '/purchase-orders', 'Customer orders': '/customer-orders', Users: '/users' };
const routeSections = Object.fromEntries(Object.entries(sectionRoutes).map(([section, route]) => [route, section]));
const sectionRoles: Record<string, Role[]> = {
  Dashboard: roles,
  Categories: roles,
  Products: roles,
  Warehouses: roles,
  Inventory: roles,
  Suppliers: roles,
  'Purchase orders': roles,
  'Customer orders': roles,
  Users: ['ADMIN']
};
const endpoints: Record<string, string> = { Categories: '/api/categories', Products: '/api/products', Warehouses: '/api/inventory/warehouses', Inventory: '/api/inventory/stock', Suppliers: '/api/suppliers', 'Purchase orders': '/api/purchase-orders', 'Customer orders': '/api/orders' };
const endpointRoles: Record<string, Role[]> = { Categories: roles, Products: roles, Warehouses: roles, Inventory: roles, Suppliers: roles, 'Purchase orders': roles, 'Customer orders': roles };
const templates: Record<string, Row> = { Categories: { name: '', description: '' }, Products: { sku: '', name: '', price: '', category: '' }, Warehouses: { name: '', location: '' }, Inventory: { productId: '', warehouseId: '', quantity: '' }, Suppliers: { name: '', email: '', phone: '' }, 'Purchase orders': { supplierId: '', productId: '', warehouseId: '', quantity: '' }, 'Customer orders': { customerEmail: '', productId: '', warehouseId: '', quantity: '' } };
const hiddenWarehouseRecords: ReadonlyArray<readonly [string, string]> = [
  ['rbac warehouse', 'test'],
  ['manager639264311479379750', 'test'],
  ['final warehouse', 'test'],
  ['admin warehouse', 'test'],
  ['manager warehouse', 'test']
];

function filterWarehouseRecords(rows: Row[]) {
  return rows.filter(row => {
    const name = String(row.name ?? '').trim().toLowerCase();
    const location = String(row.location ?? '').trim().toLowerCase();
    return !hiddenWarehouseRecords.some(([hiddenName, hiddenLocation]) => name === hiddenName && location === hiddenLocation);
  });
}

function decodeToken(token: string): Partial<User> | null {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const decoded = JSON.parse(atob(payload.padEnd(payload.length + (4 - payload.length % 4) % 4, '=')));
    return { email: decoded.sub, role: decoded.roles?.[0] || decoded.role };
  } catch { return null; }
}

function readUser(): User | null {
  const token = localStorage.getItem(tokenKey);
  if (!token) return null;
  try {
    const stored = JSON.parse(localStorage.getItem(userKey) || 'null') as User | null;
    const decoded = decodeToken(token);
    if (!decoded || !roles.includes(decoded.role as Role)) return null;
    return { id: stored?.id || decoded.id || 0, email: decoded.email || stored?.email || '', role: decoded.role as Role };
  } catch { return null; }
}

function sectionFromLocation() { return routeSections[window.location.hash.replace(/^#/, '')] || 'Dashboard'; }
function canAccess(section: string, role?: Role) { return Boolean(role && sectionRoles[section]?.includes(role)); }
function canWrite(section: string, role?: Role) {
  if (!role) return false;
  if (['Products', 'Categories', 'Warehouses', 'Inventory', 'Suppliers', 'Purchase orders'].includes(section)) return role === 'ADMIN' || role === 'MANAGER';
  if (section === 'Customer orders') return role === 'ADMIN' || role === 'STAFF';
  return false;
}

function App() {
  const initialUser = readUser();
  const [user, setUser] = useState<User | null>(initialUser);
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [active, setActive] = useState(sectionFromLocation);
  const [auth, setAuth] = useState({ email: '', password: '' });
  const [data, setData] = useState<Record<string, Row[]>>({});
  const [users, setUsers] = useState<User[]>([]);
  const [form, setForm] = useState<Row>({});
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const role = user?.role;
  const signedIn = Boolean(user);

  const load = async () => {
    if (!user) return;
    try {
      const readable = Object.entries(endpoints).filter(([key]) => endpointRoles[key].includes(user.role));
      const values = await Promise.all(readable.map(async ([key, path]) => [key, await request(path)] as const));
      setData(Object.fromEntries(values));
      if (user.role === 'ADMIN') setUsers(await request('/api/auth/users'));
      setError('');
    } catch (e) { setError(e instanceof Error ? e.message : 'Unable to load live data'); }
  };

  useEffect(() => {
    const syncRoute = () => setActive(sectionFromLocation());
    window.addEventListener('hashchange', syncRoute);
    syncRoute();
    return () => window.removeEventListener('hashchange', syncRoute);
  }, []);

  useEffect(() => { setForm(templates[active] ? { ...templates[active] } : {}); load(); }, [user, active]);

  const navigate = (section: string) => { window.location.hash = sectionRoutes[section]; };
  const authSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    try {
      const result = await request(`/api/auth/${mode}`, { method: 'POST', body: JSON.stringify(auth) });
      if (mode === 'login') {
        localStorage.setItem(tokenKey, result.token);
        const nextUser: User = { id: result.id, email: result.email, role: result.role };
        localStorage.setItem(userKey, JSON.stringify(nextUser));
        setUser(nextUser);
      } else { setMode('login'); setMessage('Registration complete. Sign in to continue.'); }
    } catch (e) { setError(e instanceof Error ? e.message : 'Authentication failed'); }
  };
  const signOut = () => { localStorage.removeItem(tokenKey); localStorage.removeItem(userKey); setUser(null); navigate('Dashboard'); };
  const save = async (event: React.FormEvent) => {
    event.preventDefault();
    try {
      const body = { ...form, price: form.price === '' ? undefined : Number(form.price), quantity: form.quantity === '' ? undefined : Number(form.quantity) };
      const path = active === 'Customer orders' ? '/api/orders/customer' : endpoints[active];
      await request(path, { method: 'POST', body: JSON.stringify(body) });
      setMessage(`${active} saved`); await load();
    } catch (e) { setError(e instanceof Error ? e.message : 'Save failed'); }
  };
  const act = async (path: string, label: string) => {
    try { await request(path, { method: 'POST' }); setMessage(label); await load(); }
    catch (e) { setError(e instanceof Error ? e.message : 'Action failed'); }
  };

  if (!user) return <main className="auth-shell"><div className="auth-panel"><div className="brand"><Boxes size={28} /><span>Stockroom</span></div><h1>{mode === 'login' ? 'Welcome back.' : 'Create workspace access.'}</h1><p>Run the retail inventory demo with live service data.</p><form onSubmit={authSubmit}><label>Email<input type="email" required value={auth.email} onChange={e => setAuth({ ...auth, email: e.target.value })} /></label><label>Password<input type="password" minLength={8} required value={auth.password} onChange={e => setAuth({ ...auth, password: e.target.value })} /></label><button className="primary"><LogIn size={16} /> {mode === 'login' ? 'Sign in' : 'Register'}</button></form><button className="link" onClick={() => setMode(mode === 'login' ? 'register' : 'login')}>{mode === 'login' ? 'Need an account? Register' : 'Already registered? Sign in'}</button>{error && <div className="notice">{error}</div>}</div></main>;

  const visibleSections = sections.filter(section => canAccess(section, role));
  const rows = active === 'Warehouses' ? filterWarehouseRecords(data.Warehouses || []) : data[active] || [];
  return <main><header><div className="brand"><Boxes size={28} /><span>Stockroom</span></div><div className="header-actions"><span className="role-badge">{user.email} · {user.role}</span><button onClick={load}><RefreshCw size={16} /> Refresh</button><button onClick={signOut}>Sign out</button></div></header><section className="hero"><p className="eyebrow">OPERATIONS CONSOLE</p><h1>Inventory at a glance.</h1><p>Products, stock positions, suppliers, and orders in one calm workspace.</p></section>{(error || message) && <div className={error ? 'notice' : 'success'}>{error || message}</div>}<nav className="tabs">{visibleSections.map(section => <button className={active === section ? 'tab active' : 'tab'} onClick={() => navigate(section)} key={section}>{section}</button>)}</nav>{!canAccess(active, role) ? <section className="forbidden"><h2>403</h2><p>You do not have permission to access this page.</p><button onClick={() => navigate('Dashboard')}>Back to dashboard</button></section> : active === 'Dashboard' ? <Dashboard data={data} /> : active === 'Users' ? <UserManagement users={users} onChanged={load} /> : <section className="workspace"><div className="section-heading"><div><p className="eyebrow">LIVE DATA</p><h2>{active}</h2></div><span className="count">{rows.length} records</span></div>{active !== 'Inventory' && canWrite(active, role) && <form className="form-grid" onSubmit={save}>{Object.keys(templates[active]).map(key => <label key={key}>{key.replace(/[A-Z]/g, c => ` ${c}`).replace(/^./, c => c.toUpperCase())}<input required={key !== 'description' && key !== 'category' && key !== 'phone'} type={key === 'price' || key === 'quantity' || key.endsWith('Id') ? 'number' : key === 'email' ? 'email' : 'text'} value={form[key] ?? ''} onChange={e => setForm({ ...form, [key]: e.target.value })} /></label>)}<button className="primary"><Plus size={16} /> Add</button></form>}{active === 'Inventory' && canWrite(active, role) && <InventoryActions onError={setError} onMessage={setMessage} onComplete={load} />}<Records rows={rows} kind={active} action={canWrite(active, role) ? act : undefined} /></section>}</main>;
}

function InventoryActions({ onError, onMessage, onComplete }: { onError: (value: string) => void; onMessage: (value: string) => void; onComplete: () => Promise<void> }) {
  const [stockIn, setStockIn] = useState<Row>({ productId: '', warehouseId: '', quantity: '' });
  const [transfer, setTransfer] = useState<Row>({ productId: '', fromWarehouseId: '', toWarehouseId: '', quantity: '' });
  const submit = async (event: React.FormEvent, path: string, values: Row, label: string) => { event.preventDefault(); try { const body = Object.fromEntries(Object.entries(values).map(([key, value]) => [key, Number(value)])); await request(path, { method: 'POST', body: JSON.stringify(body) }); onMessage(label); await onComplete(); } catch (error) { onError(error instanceof Error ? error.message : 'Inventory action failed'); } };
  const fields = (values: Row, setValues: React.Dispatch<React.SetStateAction<Row>>) => <>{Object.keys(values).map(key => <label key={key}>{key.replace(/[A-Z]/g, c => ` ${c}`).replace(/^./, c => c.toUpperCase())}<input required type="number" value={values[key]} onChange={e => setValues({ ...values, [key]: e.target.value })} /></label>)}</>;
  return <div className="form-grid"><form onSubmit={e => submit(e, '/api/inventory/stock-in', stockIn, 'Stock added')}>{fields(stockIn, setStockIn)}<button className="primary"><Plus size={16} /> Stock in</button></form><form onSubmit={e => submit(e, '/api/inventory/transfer', transfer, 'Stock transferred')}>{fields(transfer, setTransfer)}<button className="primary"><Truck size={16} /> Transfer</button></form></div>;
}

function UserManagement({ users, onChanged }: { users: User[]; onChanged: () => Promise<void> }) {
  const [error, setError] = useState('');
  const changeRole = async (id: number, role: Role) => { try { await request(`/api/auth/users/${id}/role`, { method: 'PUT', body: JSON.stringify({ role }) }); setError(''); await onChanged(); } catch (e) { setError(e instanceof Error ? e.message : 'Role update failed'); } };
  return <section className="workspace"><div className="section-heading"><div><p className="eyebrow">ACCESS CONTROL</p><h2>Users</h2></div><span className="count">{users.length} users</span></div>{error && <div className="notice">{error}</div>}<div className="table">{users.map(account => <div className="row user-row" key={account.id}><strong>{account.email}</strong><select aria-label={`Role for ${account.email}`} value={account.role} onChange={e => changeRole(account.id, e.target.value as Role)}>{roles.map(role => <option key={role} value={role}>{role}</option>)}</select></div>)}</div></section>;
}

function Dashboard({ data }: { data: Record<string, Row[]> }) { return <><section className="metrics"><Metric icon={<Package />} label="Products" value={data.Products?.length || 0} /><Metric icon={<Warehouse />} label="Stock lines" value={data.Inventory?.length || 0} /><Metric icon={<ShoppingCart />} label="Orders" value={data['Customer orders']?.length || 0} /><Metric icon={<Truck />} label="Suppliers" value={data.Suppliers?.length || 0} /></section><section className="content"><Records rows={(data.Products || []).slice(0, 6)} kind="Products" /><Records rows={(data['Customer orders'] || []).slice(-6).reverse()} kind="Customer orders" /></section></>; }
function Records({ rows, kind, action }: { rows: Row[]; kind: string; action?: (path: string, label: string) => void }) { return <div className="table">{rows.length ? rows.map(row => <div className="row" key={row.id || JSON.stringify(row)}><strong>{row.name || row.sku || row.customerEmail || `#${row.id}`}</strong><span>{row.status || row.location || row.category || (row.available !== undefined ? `Available ${row.available} / Reserved ${row.reserved}` : row.email || '')}</span>{row.id && action && (kind === 'Purchase orders' || kind === 'Customer orders') && <span className="row-actions"><button onClick={() => action(`/api/${kind === 'Purchase orders' ? 'purchase-orders' : 'orders'}/${row.id}/${kind === 'Purchase orders' ? 'receive' : 'fulfill'}`, 'Updated')}>{kind === 'Purchase orders' ? 'Receive' : 'Fulfill'}</button>{kind === 'Customer orders' && <button onClick={() => action(`/api/orders/${row.id}/cancel`, 'Order cancelled')}>Cancel</button>}</span>}</div>) : <div className="empty">No records yet. Add the first one above.</div>}</div>; }
function Metric({ icon, label, value }: { icon: React.ReactNode; label: string; value: number }) { return <div className="metric">{icon}<div><small>{label}</small><strong>{value}</strong></div></div>; }

createRoot(document.getElementById('root')!).render(<App />);
