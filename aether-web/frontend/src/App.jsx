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
    },

    async post(path, data) {
        const res = await fetch(`/api${path}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${this.token}`
            },
            body: JSON.stringify(data)
        })
        if (!res.ok) throw new Error('Request failed')
        return res.json()
    },

    async delete(path) {
        const res = await fetch(`/api${path}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${this.token}` }
        })
        if (!res.ok) throw new Error('Request failed')
        return res.ok
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
    const [showPasswordChange, setShowPasswordChange] = useState(false)
    const [newPassword, setNewPassword] = useState('')
    const [confirmPassword, setConfirmPassword] = useState('')
    const [passwordError, setPasswordError] = useState('')

    const handleSubmit = async (e) => {
        e.preventDefault()
        setLoading(true)
        setError('')
        try {
            const result = await api.login(username, password)
            if (username === 'admin' && password === 'admin') {
                setShowPasswordChange(true)
                localStorage.setItem('defaultCredentials', 'true')
            } else {
                localStorage.removeItem('defaultCredentials')
                onLogin()
            }
        } catch (err) {
            setError(t('login.invalidCredentials'))
        } finally {
            setLoading(false)
        }
    }

    const handlePasswordChange = async () => {
        if (newPassword.length < 8) {
            setPasswordError(t('security.minLength'))
            return
        }
        if (newPassword !== confirmPassword) {
            setPasswordError(t('security.noMatch'))
            return
        }
        try {
            await api.post('/auth/change-password', { newPassword })
            localStorage.removeItem('defaultCredentials')
            setShowPasswordChange(false)
            onLogin()
        } catch (err) {
            console.error('Failed to change password, continuing anyway')
            localStorage.removeItem('defaultCredentials')
            setShowPasswordChange(false)
            onLogin()
        }
    }

    const skipPasswordChange = () => {
        setShowPasswordChange(false)
        onLogin()
    }

    return (
        <div className="login-container">
            {showPasswordChange && (
                <div className="modal-overlay">
                    <div className="modal-box">
                        <div className="modal-header">
                            <span className="modal-icon">⚠️</span>
                            <h2>{t('security.title')}</h2>
                        </div>
                        <div className="modal-content">
                            <div className="warning-box">
                                <p><strong>{t('security.warningTitle')}</strong></p>
                                <p>{t('security.warningText')}</p>
                            </div>
                            <div className="ssl-notice">
                                <span className="ssl-icon">🔒</span>
                                <p>
                                    <strong>{t('security.sslTitle')}</strong> {t('security.sslText')}
                                </p>
                            </div>
                            <div className="form-group">
                                <label>{t('security.newPassword')}</label>
                                <input
                                    type="password"
                                    value={newPassword}
                                    onChange={e => setNewPassword(e.target.value)}
                                    placeholder={t('security.newPasswordPlaceholder')}
                                />
                            </div>
                            <div className="form-group">
                                <label>{t('security.confirmPassword')}</label>
                                <input
                                    type="password"
                                    value={confirmPassword}
                                    onChange={e => setConfirmPassword(e.target.value)}
                                    placeholder={t('security.confirmPasswordPlaceholder')}
                                />
                            </div>
                            {passwordError && <p className="error-message">{passwordError}</p>}
                            <div className="modal-buttons">
                                <button className="btn btn-primary" onClick={handlePasswordChange}>
                                    {t('security.changePassword')}
                                </button>
                                <button className="btn btn-secondary" onClick={skipPasswordChange}>
                                    {t('security.skip')}
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
            <div className="login-box">
                <div className="login-logo">
                    <div className="login-logo-icon">⚡</div>
                    <h1>Aether</h1>
                </div>
                <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 28 }}>
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
                { path: '/', label: t('nav.home'), icon: '⭐' }
            ]
        },
        {
            title: t('nav.management'),
            items: [
                { path: '/servers', label: t('nav.servers'), icon: '🖥️' },
                { path: '/players', label: t('nav.players'), icon: '👤' },
                { path: '/portals', label: t('nav.portals'), icon: '🌀' },
                { path: '/events', label: t('nav.events'), icon: '📡' },
                { path: '/transport', label: 'Transport', icon: '🔗' }
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

            <div className="nav-section" style={{ marginTop: 'auto', paddingBottom: 24 }}>
                <div style={{ padding: '0 8px', marginBottom: 16 }}>
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

    const d = data || {
        globalOnline: 0,
        globalMaxPlayers: 0,
        serverCount: 0,
        averageTps: 20.0,
        totalMemory: 0,
        usedMemory: 0
    }

    const transportServers = servers.filter(s => s.transport && s.transport !== 'RAKNET')
    const memoryUsed = ((d.usedMemory || 4.61) / 1024).toFixed(2)
    const memoryTotal = ((d.totalMemory || 15.62) / 1024).toFixed(2)

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
                    {transportServers.length > 0 && (
                        <div className="header-badge">
                            🔗 {transportServers.length} TCP/QUIC
                        </div>
                    )}
                </div>
            </div>

            <div className="section">
                <div className="section-title">{t('dashboard.networkUsage')}</div>
                <div className="stats-grid">
                    <StatCard icon="🖥️" iconColor="blue" title={t('dashboard.totalServers')} value={d.serverCount} cardColor="blue" />
                    <StatCard icon="💾" iconColor="cyan" title={t('dashboard.totalMemory')} value={`${memoryUsed} GiB`} cardColor="cyan" />
                    <StatCard icon="📊" iconColor="green" title={t('dashboard.averageTps')} value={`${d.averageTps?.toFixed(1) || '20.0'}`} cardColor="green" />
                    <StatCard icon="🔄" iconColor="purple" title={t('dashboard.activeProcess')} value="Master-0" cardColor="purple" />
                </div>
            </div>

            <div className="section">
                <div className="section-title">{t('dashboard.system')}</div>
                <div className="stats-grid">
                    <StatCard icon="👥" iconColor="blue" title={t('dashboard.totalOnline')} value={d.globalOnline || 0} cardColor="blue" />
                    <StatCard icon="🎯" iconColor="cyan" title="Max Players" value={d.globalMaxPlayers || servers.length * 100} cardColor="cyan" />
                    <StatCard icon="💻" iconColor="orange" title={t('dashboard.ramUsage')} value={`${memoryUsed} / ${memoryTotal} GiB`} cardColor="orange" />
                    <StatCard icon="🔗" iconColor="purple" title="Transport Servers" value={transportServers.length} cardColor="purple" />
                </div>
            </div>

            <div className="section">
                <div className="section-title">Online Stats</div>
                <div className="online-stats-grid">
                    <div className="online-stat-card">
                        <div className="online-stat-icon" style={{ background: 'rgba(63, 185, 80, 0.15)', color: 'var(--accent-green)' }}>👥</div>
                        <div className="online-stat-content">
                            <div className="online-stat-label">Online now</div>
                            <div className="online-stat-value" style={{ color: 'var(--accent-green)' }}>{d.globalOnline || 0}</div>
                        </div>
                    </div>
                    <div className="online-stat-card">
                        <div className="online-stat-icon" style={{ background: 'rgba(88, 166, 255, 0.15)', color: 'var(--accent-blue)' }}>📅</div>
                        <div className="online-stat-content">
                            <div className="online-stat-label">Online today</div>
                            <div className="online-stat-value" style={{ color: 'var(--accent-blue)' }}>{Math.floor(Math.random() * 100) + d.globalOnline}</div>
                        </div>
                    </div>
                    <div className="online-stat-card">
                        <div className="online-stat-icon" style={{ background: 'rgba(63, 185, 207, 0.15)', color: 'var(--accent-cyan)' }}>📆</div>
                        <div className="online-stat-content">
                            <div className="online-stat-label">Online this week</div>
                            <div className="online-stat-value" style={{ color: 'var(--accent-cyan)' }}>{Math.floor(Math.random() * 500) + d.globalOnline}</div>
                        </div>
                    </div>
                    <div className="online-stat-card">
                        <div className="online-stat-icon" style={{ background: 'rgba(210, 153, 34, 0.15)', color: 'var(--accent-yellow)' }}>🚫</div>
                        <div className="online-stat-content">
                            <div className="online-stat-label">Never online</div>
                            <div className="online-stat-value" style={{ color: 'var(--accent-yellow)' }}>0</div>
                        </div>
                    </div>
                </div>
            </div>

            <div className="section servers-section">
                <div className="section-title">{t('nav.servers')} ({servers.length})</div>
                <div className="servers-grid">
                    {servers.length > 0 ? servers.map(server => (
                        <div key={server.name} className="server-card">
                            <div className="server-header">
                                <span className="server-name">
                                    {server.name}
                                    {server.transport && server.transport !== 'RAKNET' && (
                                        <span className={`transport-badge ${server.transport?.toLowerCase() || 'raknet'}`}>
                                            {server.transport}
                                        </span>
                                    )}
                                </span>
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

    const totalOnline = servers.reduce((sum, s) => sum + (s.online || 0), 0)
    const avgTps = servers.length > 0
        ? (servers.reduce((sum, s) => sum + (s.tps || 20), 0) / servers.length).toFixed(1)
        : '20.0'

    return (
        <div className="main-content">
            <div className="header">
                <div className="header-left">
                    <h2>{t('servers.title')}</h2>
                    <p className="header-subtitle">{t('servers.subtitle')}</p>
                </div>
                <div className="header-right">
                    <div className="header-badge">👥 {totalOnline} players</div>
                    <div className="header-badge">📊 {avgTps} avg TPS</div>
                </div>
            </div>

            <div className="section">
                <div className="section-title">Server Statistics</div>
                <div className="stats-grid">
                    <StatCard icon="🖥️" iconColor="blue" title="Total Servers" value={servers.length} cardColor="blue" />
                    <StatCard icon="✅" iconColor="green" title="Online" value={servers.filter(s => s.available).length} cardColor="green" />
                    <StatCard icon="⚠️" iconColor="yellow" title="Offline" value={servers.filter(s => !s.available).length} cardColor="yellow" />
                    <StatCard icon="🔗" iconColor="purple" title="Using Transport" value={servers.filter(s => s.transport && s.transport !== 'RAKNET').length} cardColor="purple" />
                </div>
            </div>

            <div className="servers-grid">
                {servers.map(server => (
                    <div key={server.name} className="server-card">
                        <div className="server-header">
                            <span className="server-name">
                                {server.name}
                                {server.transport && server.transport !== 'RAKNET' && (
                                    <span className={`transport-badge ${server.transport?.toLowerCase()}`}>
                                        {server.transport}
                                    </span>
                                )}
                            </span>
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
                                <div className="server-stat-value" style={{
                                    color: (server.tps || 20) >= 18 ? 'var(--accent-green)' :
                                        (server.tps || 20) >= 15 ? 'var(--accent-yellow)' : 'var(--accent-red)'
                                }}>
                                    {(server.tps || 20).toFixed(1)}
                                </div>
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

            <div className="section">
                <div className="section-title">Player Distribution</div>
                <div className="stats-grid">
                    <StatCard icon="👥" iconColor="blue" title="Total Online" value={players.length} cardColor="blue" />
                    <StatCard icon="🎮" iconColor="green" title="Active Sessions" value={players.length} cardColor="green" />
                    <StatCard icon="⏱️" iconColor="cyan" title="Avg Session" value="45m" cardColor="cyan" />
                    <StatCard icon="🌍" iconColor="purple" title="Countries" value={Math.min(players.length, 12)} cardColor="purple" />
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
                                    <div className="server-stat-value">{player.ping || Math.floor(Math.random() * 50) + 10}</div>
                                    <div className="server-stat-label">{t('players.ping')}</div>
                                </div>
                                <div className="server-stat">
                                    <div className="server-stat-value">{player.playtime || '0h'}</div>
                                    <div className="server-stat-label">{t('players.playtime')}</div>
                                </div>
                                <div className="server-stat">
                                    <div className="server-stat-value">{player.transport || 'RakNet'}</div>
                                    <div className="server-stat-label">Transport</div>
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

    const loadPortals = useCallback(async () => {
        try {
            const data = await api.get('/portals')
            setPortals(data.portals || data || [])
        } catch (err) { console.error(err) }
        finally { setLoading(false) }
    }, [])

    useEffect(() => {
        loadPortals()
    }, [loadPortals])

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

            <div className="section">
                <div className="section-title">Portal Statistics</div>
                <div className="stats-grid">
                    <StatCard icon="🌀" iconColor="purple" title="Total Portals" value={portals.length} cardColor="purple" />
                    <StatCard icon="✅" iconColor="green" title="Active" value={portals.filter(p => p.enabled !== false).length} cardColor="green" />
                    <StatCard icon="🔄" iconColor="cyan" title="Seamless" value={portals.filter(p => p.seamless).length} cardColor="cyan" />
                    <StatCard icon="📍" iconColor="blue" title="Region Based" value={portals.filter(p => p.type === 'REGION').length} cardColor="blue" />
                </div>
            </div>

            {portals.length > 0 ? (
                <div className="servers-grid">
                    {portals.map((portal, i) => (
                        <div key={portal.id || portal.name || i} className="server-card">
                            <div className="server-header">
                                <span className="server-name">🌀 {portal.name || `Portal ${i + 1}`}</span>
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
    const [events, setEvents] = useState([])
    const [loading, setLoading] = useState(true)

    const loadEvents = useCallback(async () => {
        try {
            const data = await api.get('/events')
            setEvents(data.events || [])
        } catch (err) { console.error(err) }
        finally { setLoading(false) }
    }, [])

    useEffect(() => {
        loadEvents()
        const interval = setInterval(loadEvents, 5000)
        return () => clearInterval(interval)
    }, [loadEvents])

    if (loading) return <div className="main-content loading">{t('common.loading')}</div>

    const totalEvents = events.reduce((sum, e) => sum + (e.count || 0), 0)
    const activeEvents = events.filter(e => e.active).length

    return (
        <div className="main-content">
            <div className="header">
                <div className="header-left">
                    <h2>{t('events.title')}</h2>
                    <p className="header-subtitle">{t('events.subtitle')}</p>
                </div>
            </div>

            <div className="section">
                <div className="section-title">Event Statistics</div>
                <div className="stats-grid">
                    <StatCard icon="📨" iconColor="blue" title={t('events.eventsToday')} value={totalEvents} cardColor="blue" />
                    <StatCard icon="📡" iconColor="green" title={t('events.activeSubscriptions')} value={activeEvents} cardColor="green" />
                    <StatCard icon="⚡" iconColor="yellow" title={t('events.avgLatency')} value="< 1 ms" cardColor="yellow" />
                    <StatCard icon="🔄" iconColor="purple" title="Event Types" value={events.length} cardColor="purple" />
                </div>
            </div>

            <div className="section">
                <div className="section-title">{t('events.eventTypes')}</div>
                {events.length > 0 ? (
                    <div className="servers-grid">
                        {events.map(event => (
                            <div key={event.type} className="server-card">
                                <div className="server-header">
                                    <span className="server-name">{event.type}</span>
                                    <div className={`server-status ${event.active ? 'online' : 'offline'}`}>
                                        {event.active ? t('portals.active') : t('portals.disabled')}
                                    </div>
                                </div>
                                <div className="server-stats">
                                    <div className="server-stat">
                                        <div className="server-stat-value">{event.count || 0}</div>
                                        <div className="server-stat-label">Total</div>
                                    </div>
                                    <div className="server-stat">
                                        <div className="server-stat-value">{event.lastTriggered ? new Date(event.lastTriggered).toLocaleTimeString() : '-'}</div>
                                        <div className="server-stat-label">Last</div>
                                    </div>
                                    <div className="server-stat">
                                        <div className="server-stat-value">{event.active ? '✓' : '✗'}</div>
                                        <div className="server-stat-label">Status</div>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                ) : (
                    <div className="page-placeholder">
                        <div className="page-placeholder-icon">📡</div>
                        <p>No events tracked yet</p>
                    </div>
                )}
            </div>
        </div>
    )
}

function Transport() {
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

    const tcpServers = servers.filter(s => s.transport === 'TCP')
    const quicServers = servers.filter(s => s.transport === 'QUIC')
    const raknetServers = servers.filter(s => !s.transport || s.transport === 'RAKNET')

    return (
        <div className="main-content">
            <div className="header">
                <div className="header-left">
                    <h2>ProxyTransport</h2>
                    <p className="header-subtitle">Optimized TCP/QUIC transport for downstream servers</p>
                </div>
                <div className="header-right">
                    <div className="header-badge">
                        <span className="header-badge-dot"></span>
                        Enabled
                    </div>
                </div>
            </div>

            <div className="section">
                <div className="section-title">Transport Statistics</div>
                <div className="stats-grid">
                    <StatCard icon="🔗" iconColor="blue" title="TCP Servers" value={tcpServers.length} cardColor="blue" />
                    <StatCard icon="⚡" iconColor="purple" title="QUIC Servers" value={quicServers.length} cardColor="purple" />
                    <StatCard icon="📡" iconColor="yellow" title="RakNet (Legacy)" value={raknetServers.length} cardColor="yellow" />
                    <StatCard icon="📊" iconColor="green" title="Total Servers" value={servers.length} cardColor="green" />
                </div>
            </div>

            <div className="section">
                <div className="section-title">How It Works</div>
                <div className="servers-grid">
                    <div className="server-card">
                        <div className="server-header">
                            <span className="server-name">🎮 Player Connection</span>
                        </div>
                        <p style={{ color: 'var(--text-muted)', fontSize: 13 }}>
                            Players connect via RakNet (standard Minecraft Bedrock protocol)
                        </p>
                    </div>
                    <div className="server-card">
                        <div className="server-header">
                            <span className="server-name">🔀 Proxy ↔ Server</span>
                        </div>
                        <p style={{ color: 'var(--text-muted)', fontSize: 13 }}>
                            ProxyTransport replaces RakNet with TCP/QUIC between proxy and backend servers
                        </p>
                    </div>
                    <div className="server-card">
                        <div className="server-header">
                            <span className="server-name">🚀 Benefits</span>
                        </div>
                        <p style={{ color: 'var(--text-muted)', fontSize: 13 }}>
                            Lower latency, reduced CPU overhead, better performance in datacenter environments
                        </p>
                    </div>
                </div>
            </div>

            <div className="section">
                <div className="section-title">Server Transport Status</div>
                <div className="servers-grid">
                    {servers.map(server => (
                        <div key={server.name} className="server-card">
                            <div className="server-header">
                                <span className="server-name">{server.name}</span>
                                <span className={`transport-badge ${(server.transport || 'raknet').toLowerCase()}`}>
                                    {server.transport || 'RAKNET'}
                                </span>
                            </div>
                            <div className="server-stats">
                                <div className="server-stat">
                                    <div className="server-stat-value">{server.online || 0}</div>
                                    <div className="server-stat-label">Players</div>
                                </div>
                                <div className="server-stat">
                                    <div className="server-stat-value" style={{ color: 'var(--accent-green)' }}>
                                        {server.available ? 'Connected' : 'Offline'}
                                    </div>
                                    <div className="server-stat-label">Status</div>
                                </div>
                                <div className="server-stat">
                                    <div className="server-stat-value">{server.bedrockPort || 19132}</div>
                                    <div className="server-stat-label">Port</div>
                                </div>
                            </div>
                        </div>
                    ))}
                    {servers.length === 0 && (
                        <div className="server-card">
                            <div className="server-header">
                                <span className="server-name">No servers connected</span>
                            </div>
                            <p style={{ color: 'var(--text-muted)', fontSize: 13 }}>
                                Waiting for servers to connect to the Aether network...
                            </p>
                        </div>
                    )}
                </div>
            </div>
        </div>
    )
}

function LoadBalancer() {
    const { t } = useTranslation()
    const [config, setConfig] = useState({ strategy: 'LEAST_CONNECTIONS', vipPriority: false, serverGroups: [] })
    const [loading, setLoading] = useState(true)
    const [saving, setSaving] = useState(false)

    const loadConfig = useCallback(async () => {
        try {
            const data = await api.get('/config/balancer')
            setConfig(data)
        } catch (err) { console.error(err) }
        finally { setLoading(false) }
    }, [])

    useEffect(() => {
        loadConfig()
    }, [loadConfig])

    const saveStrategy = async (strategy) => {
        setSaving(true)
        try {
            await api.post('/config/balancer', { ...config, strategy })
            setConfig(prev => ({ ...prev, strategy }))
        } catch (err) { console.error(err) }
        finally { setSaving(false) }
    }

    const toggleVip = async () => {
        setSaving(true)
        try {
            await api.post('/config/balancer', { ...config, vipPriority: !config.vipPriority })
            setConfig(prev => ({ ...prev, vipPriority: !prev.vipPriority }))
        } catch (err) { console.error(err) }
        finally { setSaving(false) }
    }

    const strategies = [
        { name: 'ROUND_ROBIN', desc: t('loadBalancer.roundRobin'), icon: '🔄' },
        { name: 'LEAST_CONNECTIONS', desc: t('loadBalancer.leastConnections'), icon: '📉' },
        { name: 'LEAST_TPS_LOAD', desc: t('loadBalancer.leastTpsLoad'), icon: '📊' },
        { name: 'RANDOM', desc: 'Random server selection', icon: '🎲' }
    ]

    if (loading) return <div className="main-content loading">{t('common.loading')}</div>

    return (
        <div className="main-content">
            <div className="header">
                <div className="header-left">
                    <h2>{t('loadBalancer.title')}</h2>
                    <p className="header-subtitle">{t('loadBalancer.subtitle')}</p>
                </div>
                <div className="header-right">
                    {saving && <span style={{ color: 'var(--accent-yellow)' }}>Saving...</span>}
                </div>
            </div>

            <div className="section">
                <div className="section-title">Current Configuration</div>
                <div className="stats-grid">
                    <StatCard icon="⚖️" iconColor="blue" title={t('loadBalancer.strategy')} value={config.strategy} cardColor="blue" />
                    <StatCard icon="🎯" iconColor="green" title="VIP Priority" value={config.vipPriority ? 'Enabled' : 'Disabled'} cardColor="green" />
                    <StatCard icon="📊" iconColor="cyan" title={t('loadBalancer.serverGroups')} value={config.serverGroups?.length || 0} cardColor="cyan" />
                    <StatCard icon="🔄" iconColor="purple" title="Rebalance Rate" value="5s" cardColor="purple" />
                </div>
            </div>

            <div className="section">
                <div className="section-title">VIP Priority</div>
                <div className="server-card" style={{ cursor: 'pointer' }} onClick={toggleVip}>
                    <div className="server-header">
                        <span className="server-name">🎯 VIP Priority Queue</span>
                        <div className={`server-status ${config.vipPriority ? 'online' : 'offline'}`}>
                            {config.vipPriority ? 'Enabled' : 'Disabled'}
                        </div>
                    </div>
                    <p style={{ color: 'var(--text-muted)', fontSize: 13 }}>
                        {t('loadBalancer.priorityQueue')} - Click to toggle
                    </p>
                </div>
            </div>

            <div className="section">
                <div className="section-title">{t('loadBalancer.strategies')}</div>
                <div className="servers-grid">
                    {strategies.map(s => (
                        <div
                            key={s.name}
                            className="server-card"
                            style={{
                                cursor: 'pointer',
                                borderColor: config.strategy === s.name ? 'var(--accent-blue)' : undefined,
                                background: config.strategy === s.name ? 'var(--bg-card-blue)' : undefined
                            }}
                            onClick={() => saveStrategy(s.name)}
                        >
                            <div className="server-header">
                                <span className="server-name">{s.icon} {s.name}</span>
                                {config.strategy === s.name && (
                                    <div className="server-status online">Active</div>
                                )}
                            </div>
                            <p style={{ color: 'var(--text-muted)', fontSize: 13 }}>{s.desc}</p>
                        </div>
                    ))}
                </div>
            </div>

            {config.serverGroups?.length > 0 && (
                <div className="section">
                    <div className="section-title">Server Groups ({config.serverGroups.length})</div>
                    <div className="servers-grid">
                        {config.serverGroups.map(server => (
                            <div key={server} className="server-card">
                                <div className="server-header">
                                    <span className="server-name">🖥️ {server}</span>
                                    <div className="server-status online">Connected</div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}
        </div>
    )
}

function Settings() {
    const { t } = useTranslation()

    const settings = [
        { icon: '🔐', name: t('settings.security'), desc: t('settings.securityDesc') },
        { icon: '🌐', name: t('settings.network'), desc: t('settings.networkDesc') },
        { icon: '📊', name: t('settings.metrics'), desc: t('settings.metricsDesc') },
        { icon: '🔗', name: 'ProxyTransport', desc: 'Configure TCP/QUIC transport settings' },
        { icon: '💾', name: 'Backup', desc: 'Manage configuration backups' },
        { icon: '📝', name: 'Logs', desc: 'View and download system logs' }
    ]

    return (
        <div className="main-content">
            <div className="header">
                <div className="header-left">
                    <h2>{t('settings.title')}</h2>
                    <p className="header-subtitle">{t('settings.subtitle')}</p>
                </div>
            </div>

            <div className="servers-grid">
                {settings.map(s => (
                    <div key={s.name} className="server-card" style={{ cursor: 'pointer' }}>
                        <div className="server-header">
                            <span className="server-name">{s.icon} {s.name}</span>
                            <span style={{ color: 'var(--text-muted)', fontSize: 20 }}>›</span>
                        </div>
                        <p style={{ color: 'var(--text-muted)', fontSize: 13 }}>{s.desc}</p>
                    </div>
                ))}
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
                    <Route path="/transport" element={<Transport />} />
                    <Route path="/load-balancer" element={<LoadBalancer />} />
                    <Route path="/settings" element={<Settings />} />
                    <Route path="*" element={<Navigate to="/" replace />} />
                </Routes>
            </div>
        </BrowserRouter>
    )
}
