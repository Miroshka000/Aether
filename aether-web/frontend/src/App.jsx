import { useState, useEffect, useCallback } from 'react'
import { BrowserRouter, Routes, Route, Navigate, Link, useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

const api = {
    token: localStorage.getItem('token'),

    async login(username, password) {
        const res = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        })
        if (!res.ok) throw new Error('Invalid credentials')
        const data = await res.json()
        this.token = data.accessToken
        localStorage.setItem('token', data.accessToken)
        return data
    },

    logout() {
        this.token = null
        localStorage.removeItem('token')
    },

    async get(path) {
        const res = await fetch(`/api${path}`, {
            headers: { 'Authorization': `Bearer ${this.token}` }
        })
        if (res.status === 401) {
            this.logout()
            window.location.reload()
            throw new Error('Unauthorized')
        }
        if (!res.ok) throw new Error('Request failed')
        return res.json()
    }
}

function LanguageSwitcher() {
    const { i18n } = useTranslation()

    const changeLanguage = (lang) => {
        i18n.changeLanguage(lang)
        localStorage.setItem('language', lang)
    }

    return (
        <div className="language-switcher">
            <button
                className={`lang-btn ${i18n.language === 'en' ? 'active' : ''}`}
                onClick={() => changeLanguage('en')}
            >
                EN
            </button>
            <button
                className={`lang-btn ${i18n.language === 'ru' ? 'active' : ''}`}
                onClick={() => changeLanguage('ru')}
            >
                RU
            </button>
        </div>
    )
}

function Login({ onLogin }) {
    const { t } = useTranslation()
    const [username, setUsername] = useState('')
    const [password, setPassword] = useState('')
    const [error, setError] = useState('')
    const [loading, setLoading] = useState(false)

    const handleSubmit = async (e) => {
        e.preventDefault()
        setLoading(true)
        setError('')
        try {
            await api.login(username, password)
            onLogin()
        } catch (err) {
            setError(t('login.invalidCredentials'))
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="login-container">
            <div className="login-box">
                <div className="login-logo">
                    <div className="login-logo-icon">⚡</div>
                    <h1>Aether</h1>
                </div>
                <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 24 }}>
                    <LanguageSwitcher />
                </div>
                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label>{t('login.username')}</label>
                        <input
                            type="text"
                            value={username}
                            onChange={e => setUsername(e.target.value)}
                            placeholder="admin"
                            autoComplete="username"
                        />
                    </div>
                    <div className="form-group">
                        <label>{t('login.password')}</label>
                        <input
                            type="password"
                            value={password}
                            onChange={e => setPassword(e.target.value)}
                            placeholder="••••••••"
                            autoComplete="current-password"
                        />
                    </div>
                    {error && <p className="error-message">{error}</p>}
                    <button type="submit" className="btn btn-primary login-btn" disabled={loading}>
                        {loading ? t('login.signingIn') : t('login.signIn')}
                    </button>
                </form>
            </div>
        </div>
    )
}

function Sidebar({ onLogout }) {
    const { t } = useTranslation()
    const location = useLocation()

    const navSections = [
        {
            title: t('nav.overview'),
            items: [
                { path: '/', label: t('nav.home'), icon: '🏠' }
            ]
        },
        {
            title: t('nav.management'),
            items: [
                { path: '/servers', label: t('nav.servers'), icon: '🖥️' },
                { path: '/players', label: t('nav.players'), icon: '👤' },
                { path: '/portals', label: t('nav.portals'), icon: '🌀' },
                { path: '/events', label: t('nav.events'), icon: '📡' }
            ]
        },
        {
            title: t('nav.configuration'),
            items: [
                { path: '/load-balancer', label: t('nav.loadBalancer'), icon: '⚖️' },
                { path: '/settings', label: t('nav.settings'), icon: '⚙️' }
            ]
        }
    ]

    return (
        <aside className="sidebar">
            <div className="logo">
                <div className="logo-icon">⚡</div>
                <h1>Aether</h1>
            </div>

            {navSections.map(section => (
                <div key={section.title} className="nav-section">
                    <div className="nav-section-title">{section.title}</div>
                    <nav className="nav-menu">
                        {section.items.map(item => (
                            <Link
                                key={item.path}
                                to={item.path}
                                className={`nav-item ${location.pathname === item.path ? 'active' : ''}`}
                            >
                                <span className="nav-icon">{item.icon}</span>
                                <span>{item.label}</span>
                            </Link>
                        ))}
                    </nav>
                </div>
            ))}

            <div className="nav-section" style={{ marginTop: 'auto', paddingBottom: 20 }}>
                <div style={{ padding: '0 8px', marginBottom: 12 }}>
                    <LanguageSwitcher />
                </div>
                <button className="nav-item" onClick={onLogout} style={{ width: '100%', border: 'none', background: 'none', textAlign: 'left' }}>
                    <span className="nav-icon">🚪</span>
                    <span>{t('nav.logout')}</span>
                </button>
            </div>
        </aside>
    )
}

function StatCard({ icon, iconColor, title, value, subtitle, change, changeType, cardColor }) {
    return (
        <div className={`stat-card ${cardColor || ''}`}>
            <div className="stat-header">
                <div className={`stat-icon ${iconColor || 'blue'}`}>{icon}</div>
                <span className="stat-title">{title}</span>
            </div>
            <div className="stat-value">{value}</div>
            {subtitle && (
                <div className="stat-subtitle">
                    {change && <span className={`stat-change ${changeType || 'up'}`}>{change}</span>}
                    {subtitle}
                </div>
            )}
        </div>
    )
}

function Dashboard() {
    const { t } = useTranslation()
    const [data, setData] = useState(null)
    const [servers, setServers] = useState([])
    const [loading, setLoading] = useState(true)

    const loadData = useCallback(async () => {
        try {
            const [overview, serversData] = await Promise.all([
                api.get('/dashboard/overview'),
                api.get('/dashboard/servers')
            ])
            setData(overview)
            setServers(serversData.servers || [])
        } catch (err) {
            console.error('Failed to load data:', err)
        } finally {
            setLoading(false)
        }
    }, [])

    useEffect(() => {
        loadData()
        const interval = setInterval(loadData, 5000)
        return () => clearInterval(interval)
    }, [loadData])

    if (loading) {
        return <div className="main-content loading">{t('common.loading')}</div>
    }

    const mockData = data || {
        globalOnline: 0,
        globalMaxPlayers: 0,
        serverCount: 0,
        averageTps: 20.0,
        totalMemory: 0,
        usedMemory: 0,
        totalTraffic: 0
    }

    return (
        <div className="main-content">
            <div className="header">
                <div className="header-left">
                    <h2>{t('dashboard.title')}</h2>
                    <p className="header-subtitle">{t('dashboard.subtitle')}</p>
                </div>
                <div className="header-right">
                    <div className="header-badge">
                        <span className="header-badge-dot"></span>
                        <span>{t('common.version')}</span>
                    </div>
                    <div className="header-badge">
                        ⭐ {servers.length} {t('common.nodes')}
                    </div>
                </div>
            </div>

            <div className="section">
                <div className="section-title">{t('dashboard.networkUsage')}</div>
                <div className="stats-grid">
                    <StatCard icon="🖥️" iconColor="blue" title={t('dashboard.totalServers')} value={mockData.serverCount} cardColor="blue" />
                    <StatCard icon="💾" iconColor="cyan" title={t('dashboard.totalMemory')} value={`${Math.round((mockData.usedMemory || 545) / 1024 * 100) / 100} MiB`} cardColor="cyan" />
                    <StatCard icon="📊" iconColor="green" title={t('dashboard.averageTps')} value={`${mockData.averageTps?.toFixed(1) || '20.0'}`} cardColor="green" />
                    <StatCard icon="🔄" iconColor="purple" title={t('dashboard.activeProcess')} value="Master-0" cardColor="purple" />
                </div>
            </div>

            <div className="section">
                <div className="section-title">{t('dashboard.bandwidth')}</div>
                <div className="bandwidth-grid">
                    <StatCard icon="📅" iconColor="blue" title={t('dashboard.today')} value="103.65 GiB" subtitle={t('dashboard.vsYesterday')} change="+40.17 GiB" changeType="down" />
                    <StatCard icon="📆" iconColor="cyan" title={t('dashboard.last7days')} value="835.03 GiB" subtitle={t('dashboard.vsLastWeek')} change="-68.20 GiB" changeType="down" />
                    <StatCard icon="📊" iconColor="green" title={t('dashboard.last30days')} value="4.95 TiB" subtitle={t('dashboard.vsLastMonth')} change="+3.05 TiB" changeType="up" />
                    <StatCard icon="📈" iconColor="purple" title={t('dashboard.currentYear')} value="10.31 TiB" subtitle={t('dashboard.vsLastYear')} change="+10.31 TiB" changeType="up" />
                </div>
            </div>

            <div className="section">
                <div className="section-title">{t('dashboard.system')}</div>
                <div className="stats-grid">
                    <StatCard icon="👥" iconColor="blue" title={t('dashboard.totalOnline')} value={mockData.globalOnline || 0} />
                    <StatCard icon="📡" iconColor="cyan" title={t('dashboard.totalTraffic')} value="10.80 TiB" />
                    <StatCard icon="💻" iconColor="orange" title={t('dashboard.ramUsage')} value={`${(mockData.usedMemory || 4.61).toFixed(2)} GiB / ${(mockData.totalMemory || 15.62).toFixed(2)} GiB`} />
                </div>
            </div>

            <div className="section servers-section">
                <div className="section-title">{t('nav.servers')} ({servers.length})</div>
                <div className="servers-grid">
                    {servers.length > 0 ? servers.map(server => (
                        <div key={server.name} className="server-card">
                            <div className="server-header">
                                <span className="server-name">{server.name}</span>
                                <div className={`server-status ${server.available ? 'online' : 'offline'}`}>
                                    <span className={`status-dot ${server.available ? 'online' : 'offline'}`}></span>
                                    {server.available ? t('servers.online') : t('servers.offline')}
                                </div>
                            </div>
                            <div className="server-stats">
                                <div className="server-stat">
                                    <div className="server-stat-value">{server.online || 0}</div>
                                    <div className="server-stat-label">{t('servers.players')}</div>
                                </div>
                                <div className="server-stat">
                                    <div className="server-stat-value">{server.maxPlayers || 100}</div>
                                    <div className="server-stat-label">{t('servers.max')}</div>
                                </div>
                                <div className="server-stat">
                                    <div className="server-stat-value" style={{ color: (server.tps || 20) >= 18 ? 'var(--accent-green)' : (server.tps || 20) >= 15 ? 'var(--accent-yellow)' : 'var(--accent-red)' }}>
                                        {(server.tps || 20).toFixed(1)}
                                    </div>
                                    <div className="server-stat-label">{t('servers.tps')}</div>
                                </div>
                            </div>
                        </div>
                    )) : (
                        <div className="server-card">
                            <div className="server-header">
                                <span className="server-name">{t('servers.noServers')}</span>
                            </div>
                            <p style={{ color: 'var(--text-muted)', fontSize: 13 }}>{t('servers.waiting')}</p>
                        </div>
                    )}
                </div>
            </div>
        </div>
    )
}

function Servers() {
    const { t } = useTranslation()
    const [servers, setServers] = useState([])
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        const load = async () => {
            try {
                const data = await api.get('/dashboard/servers')
                setServers(data.servers || [])
            } catch (err) { console.error(err) }
            finally { setLoading(false) }
        }
        load()
        const interval = setInterval(load, 5000)
        return () => clearInterval(interval)
    }, [])

    if (loading) return <div className="main-content loading">{t('common.loading')}</div>

    return (
        <div className="main-content">
            <div className="header">
                <div className="header-left">
                    <h2>{t('servers.title')}</h2>
                    <p className="header-subtitle">{t('servers.subtitle')}</p>
                </div>
            </div>
            <div className="servers-grid">
                {servers.map(server => (
                    <div key={server.name} className="server-card">
                        <div className="server-header">
                            <span className="server-name">{server.name}</span>
                            <div className={`server-status ${server.available ? 'online' : 'offline'}`}>
                                <span className={`status-dot ${server.available ? 'online' : 'offline'}`}></span>
                                {server.available ? t('servers.online') : t('servers.offline')}
                            </div>
                        </div>
                        <div className="server-stats">
                            <div className="server-stat">
                                <div className="server-stat-value">{server.online || 0}</div>
                                <div className="server-stat-label">{t('servers.players')}</div>
                            </div>
                            <div className="server-stat">
                                <div className="server-stat-value">{server.maxPlayers || 100}</div>
                                <div className="server-stat-label">{t('servers.max')}</div>
                            </div>
                            <div className="server-stat">
                                <div className="server-stat-value">{(server.tps || 20).toFixed(1)}</div>
                                <div className="server-stat-label">{t('servers.tps')}</div>
                            </div>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    )
}

function Players() {
    const { t } = useTranslation()
    const [players, setPlayers] = useState([])
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        const load = async () => {
            try {
                const data = await api.get('/dashboard/players')
                setPlayers(data.players || [])
            } catch (err) { console.error(err) }
            finally { setLoading(false) }
        }
        load()
        const interval = setInterval(load, 5000)
        return () => clearInterval(interval)
    }, [])

    if (loading) return <div className="main-content loading">{t('common.loading')}</div>

    return (
        <div className="main-content">
            <div className="header">
                <div className="header-left">
                    <h2>{t('players.title')}</h2>
                    <p className="header-subtitle">{t('players.subtitle')}</p>
                </div>
                <div className="header-right">
                    <div className="header-badge">👥 {players.length} {t('players.online')}</div>
                </div>
            </div>
            {players.length > 0 ? (
                <div className="servers-grid">
                    {players.map(player => (
                        <div key={player.uuid || player.name} className="server-card">
                            <div className="server-header">
                                <span className="server-name">{player.name}</span>
                                <div className="server-status online">
                                    <span className="status-dot online"></span>
                                    {player.server || 'Unknown'}
                                </div>
                            </div>
                            <div className="server-stats">
                                <div className="server-stat">
                                    <div className="server-stat-value">{player.ping || 0}</div>
                                    <div className="server-stat-label">{t('players.ping')}</div>
                                </div>
                                <div className="server-stat">
                                    <div className="server-stat-value">{player.playtime || '0h'}</div>
                                    <div className="server-stat-label">{t('players.playtime')}</div>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            ) : (
                <div className="page-placeholder">
                    <div className="page-placeholder-icon">👤</div>
                    <p>{t('players.noPlayers')}</p>
                </div>
            )}
        </div>
    )
}

function Portals() {
    const { t } = useTranslation()
    const [portals, setPortals] = useState([])
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        const load = async () => {
            try {
                const data = await api.get('/portals')
                setPortals(data.portals || data || [])
            } catch (err) { console.error(err) }
            finally { setLoading(false) }
        }
        load()
    }, [])

    if (loading) return <div className="main-content loading">{t('common.loading')}</div>

    return (
        <div className="main-content">
            <div className="header">
                <div className="header-left">
                    <h2>{t('portals.title')}</h2>
                    <p className="header-subtitle">{t('portals.subtitle')}</p>
                </div>
                <div className="header-right">
                    <button className="btn btn-primary">+ {t('portals.addPortal')}</button>
                </div>
            </div>
            {portals.length > 0 ? (
                <div className="servers-grid">
                    {portals.map((portal, i) => (
                        <div key={portal.name || i} className="server-card">
                            <div className="server-header">
                                <span className="server-name">🌀 {portal.name}</span>
                                <div className={`server-status ${portal.enabled !== false ? 'online' : 'offline'}`}>
                                    {portal.enabled !== false ? t('portals.active') : t('portals.disabled')}
                                </div>
                            </div>
                            <div className="server-stats">
                                <div className="server-stat">
                                    <div className="server-stat-value">{portal.sourceServer || '-'}</div>
                                    <div className="server-stat-label">{t('portals.from')}</div>
                                </div>
                                <div className="server-stat">
                                    <div className="server-stat-value">{portal.targetServer || '-'}</div>
                                    <div className="server-stat-label">{t('portals.to')}</div>
                                </div>
                                <div className="server-stat">
                                    <div className="server-stat-value">{portal.type || 'REGION'}</div>
                                    <div className="server-stat-label">{t('portals.type')}</div>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            ) : (
                <div className="page-placeholder">
                    <div className="page-placeholder-icon">🌀</div>
                    <p>{t('portals.noPortals')}</p>
                </div>
            )}
        </div>
    )
}

function Events() {
    const { t } = useTranslation()
    return (
        <div className="main-content">
            <div className="header">
                <div className="header-left">
                    <h2>{t('events.title')}</h2>
                    <p className="header-subtitle">{t('events.subtitle')}</p>
                </div>
            </div>
            <div className="stats-grid">
                <StatCard icon="📨" iconColor="blue" title={t('events.eventsToday')} value="1,234" />
                <StatCard icon="📡" iconColor="green" title={t('events.activeSubscriptions')} value="45" />
                <StatCard icon="⚡" iconColor="yellow" title={t('events.avgLatency')} value="2.3 ms" />
            </div>
            <div className="section" style={{ marginTop: 32 }}>
                <div className="section-title">{t('events.eventTypes')}</div>
                <div className="servers-grid">
                    {['PlayerChat', 'PlayerAchievement', 'StaffAlert', 'VIPReward'].map(type => (
                        <div key={type} className="server-card">
                            <div className="server-header">
                                <span className="server-name">{type}</span>
                                <div className="server-status online">{t('portals.active')}</div>
                            </div>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    )
}

function LoadBalancer() {
    const { t } = useTranslation()
    return (
        <div className="main-content">
            <div className="header">
                <div className="header-left">
                    <h2>{t('loadBalancer.title')}</h2>
                    <p className="header-subtitle">{t('loadBalancer.subtitle')}</p>
                </div>
            </div>
            <div className="stats-grid">
                <StatCard icon="⚖️" iconColor="blue" title={t('loadBalancer.strategy')} value="LEAST_CONN" />
                <StatCard icon="🎯" iconColor="green" title={t('loadBalancer.vipPriority')} value={t('loadBalancer.enabled')} />
                <StatCard icon="📊" iconColor="cyan" title={t('loadBalancer.serverGroups')} value="3" />
            </div>
            <div className="section" style={{ marginTop: 32 }}>
                <div className="section-title">{t('loadBalancer.strategies')}</div>
                <div className="servers-grid">
                    {[
                        { name: 'ROUND_ROBIN', desc: t('loadBalancer.roundRobin') },
                        { name: 'LEAST_CONNECTIONS', desc: t('loadBalancer.leastConnections') },
                        { name: 'LEAST_TPS_LOAD', desc: t('loadBalancer.leastTpsLoad') },
                        { name: 'PRIORITY_QUEUE', desc: t('loadBalancer.priorityQueue') }
                    ].map(s => (
                        <div key={s.name} className="server-card">
                            <div className="server-header">
                                <span className="server-name">{s.name}</span>
                            </div>
                            <p style={{ color: 'var(--text-muted)', fontSize: 13 }}>{s.desc}</p>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    )
}

function Settings() {
    const { t } = useTranslation()
    return (
        <div className="main-content">
            <div className="header">
                <div className="header-left">
                    <h2>{t('settings.title')}</h2>
                    <p className="header-subtitle">{t('settings.subtitle')}</p>
                </div>
            </div>
            <div className="servers-grid">
                <div className="server-card">
                    <div className="server-header">
                        <span className="server-name">🔐 {t('settings.security')}</span>
                    </div>
                    <p style={{ color: 'var(--text-muted)', fontSize: 13 }}>{t('settings.securityDesc')}</p>
                </div>
                <div className="server-card">
                    <div className="server-header">
                        <span className="server-name">🌐 {t('settings.network')}</span>
                    </div>
                    <p style={{ color: 'var(--text-muted)', fontSize: 13 }}>{t('settings.networkDesc')}</p>
                </div>
                <div className="server-card">
                    <div className="server-header">
                        <span className="server-name">📊 {t('settings.metrics')}</span>
                    </div>
                    <p style={{ color: 'var(--text-muted)', fontSize: 13 }}>{t('settings.metricsDesc')}</p>
                </div>
            </div>
        </div>
    )
}

export default function App() {
    const [authenticated, setAuthenticated] = useState(!!api.token)

    const handleLogout = () => {
        api.logout()
        setAuthenticated(false)
    }

    if (!authenticated) {
        return <Login onLogin={() => setAuthenticated(true)} />
    }

    return (
        <BrowserRouter>
            <div className="app">
                <Sidebar onLogout={handleLogout} />
                <Routes>
                    <Route path="/" element={<Dashboard />} />
                    <Route path="/servers" element={<Servers />} />
                    <Route path="/players" element={<Players />} />
                    <Route path="/portals" element={<Portals />} />
                    <Route path="/events" element={<Events />} />
                    <Route path="/load-balancer" element={<LoadBalancer />} />
                    <Route path="/settings" element={<Settings />} />
                    <Route path="*" element={<Navigate to="/" replace />} />
                </Routes>
            </div>
        </BrowserRouter>
    )
}
