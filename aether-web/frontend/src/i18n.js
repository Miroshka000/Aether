import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'

const resources = {
    en: {
        translation: {
            nav: {
                overview: 'Overview',
                home: 'Home',
                management: 'Management',
                servers: 'Servers',
                players: 'Players',
                portals: 'Portals',
                events: 'Events',
                configuration: 'Configuration',
                loadBalancer: 'Load Balancer',
                settings: 'Settings',
                logout: 'Logout'
            },
            dashboard: {
                title: 'Dashboard',
                subtitle: 'Aether Network Overview',
                networkUsage: 'Network Usage',
                totalServers: 'Total Servers',
                totalMemory: 'Total Memory',
                averageTps: 'Average TPS',
                activeProcess: 'Active Process',
                bandwidth: 'Bandwidth',
                today: 'Today',
                last7days: 'Last 7 days',
                last30days: 'Last 30 days',
                currentYear: 'Current year',
                vsYesterday: 'vs yesterday',
                vsLastWeek: 'vs last week',
                vsLastMonth: 'vs last month',
                vsLastYear: 'vs last year',
                system: 'System',
                totalOnline: 'Total online on nodes',
                totalTraffic: 'Total traffic',
                ramUsage: 'RAM usage'
            },
            servers: {
                title: 'Servers',
                subtitle: 'Manage connected nodes',
                players: 'Players',
                max: 'Max',
                tps: 'TPS',
                online: 'Online',
                offline: 'Offline',
                noServers: 'No servers connected',
                waiting: 'Waiting for nodes to connect...'
            },
            players: {
                title: 'Players',
                subtitle: 'Online players across network',
                online: 'online',
                ping: 'Ping',
                playtime: 'Playtime',
                noPlayers: 'No players online'
            },
            portals: {
                title: 'Portals',
                subtitle: 'Cross-server portal configuration',
                addPortal: 'Add Portal',
                from: 'From',
                to: 'To',
                type: 'Type',
                active: 'Active',
                disabled: 'Disabled',
                noPortals: 'No portals configured'
            },
            events: {
                title: 'Events',
                subtitle: 'Cross-server event broadcasting',
                eventsToday: 'Events Today',
                activeSubscriptions: 'Active Subscriptions',
                avgLatency: 'Avg Latency',
                eventTypes: 'Event Types'
            },
            loadBalancer: {
                title: 'Load Balancer',
                subtitle: 'Server balancing configuration',
                strategy: 'Strategy',
                vipPriority: 'VIP Priority',
                enabled: 'Enabled',
                serverGroups: 'Server Groups',
                strategies: 'Balancing Strategies',
                roundRobin: 'Sequential rotation',
                leastConnections: 'Fewest players',
                leastTpsLoad: 'Best TPS',
                priorityQueue: 'VIP first'
            },
            settings: {
                title: 'Settings',
                subtitle: 'System configuration',
                security: 'Security',
                securityDesc: 'JWT tokens, API keys',
                network: 'Network',
                networkDesc: 'Ports, timeouts, compression',
                metrics: 'Metrics',
                metricsDesc: 'Prometheus, logging'
            },
            login: {
                username: 'Username',
                password: 'Password',
                signIn: 'Sign In',
                signingIn: 'Signing in...',
                invalidCredentials: 'Invalid credentials'
            },
            common: {
                loading: 'Loading...',
                nodes: 'nodes',
                version: 'v1.0.0'
            }
        }
    },
    ru: {
        translation: {
            nav: {
                overview: 'Обзор',
                home: 'Главная',
                management: 'Управление',
                servers: 'Серверы',
                players: 'Игроки',
                portals: 'Порталы',
                events: 'События',
                configuration: 'Конфигурация',
                loadBalancer: 'Балансировщик',
                settings: 'Настройки',
                logout: 'Выход'
            },
            dashboard: {
                title: 'Панель управления',
                subtitle: 'Обзор сети Aether',
                networkUsage: 'Использование сети',
                totalServers: 'Всего серверов',
                totalMemory: 'Всего памяти',
                averageTps: 'Средний TPS',
                activeProcess: 'Активный процесс',
                bandwidth: 'Трафик',
                today: 'Сегодня',
                last7days: 'За 7 дней',
                last30days: 'За 30 дней',
                currentYear: 'За год',
                vsYesterday: 'к вчера',
                vsLastWeek: 'к прошлой неделе',
                vsLastMonth: 'к прошлому месяцу',
                vsLastYear: 'к прошлому году',
                system: 'Система',
                totalOnline: 'Всего онлайн на нодах',
                totalTraffic: 'Общий трафик',
                ramUsage: 'Использование RAM'
            },
            servers: {
                title: 'Серверы',
                subtitle: 'Управление подключёнными нодами',
                players: 'Игроки',
                max: 'Макс',
                tps: 'TPS',
                online: 'Онлайн',
                offline: 'Оффлайн',
                noServers: 'Нет подключённых серверов',
                waiting: 'Ожидание подключения нод...'
            },
            players: {
                title: 'Игроки',
                subtitle: 'Онлайн игроки в сети',
                online: 'онлайн',
                ping: 'Пинг',
                playtime: 'Наиграно',
                noPlayers: 'Нет игроков онлайн'
            },
            portals: {
                title: 'Порталы',
                subtitle: 'Конфигурация кросс-серверных порталов',
                addPortal: 'Добавить портал',
                from: 'Откуда',
                to: 'Куда',
                type: 'Тип',
                active: 'Активен',
                disabled: 'Отключён',
                noPortals: 'Порталы не настроены'
            },
            events: {
                title: 'События',
                subtitle: 'Кросс-серверная рассылка событий',
                eventsToday: 'Событий сегодня',
                activeSubscriptions: 'Активных подписок',
                avgLatency: 'Средняя задержка',
                eventTypes: 'Типы событий'
            },
            loadBalancer: {
                title: 'Балансировщик',
                subtitle: 'Конфигурация балансировки серверов',
                strategy: 'Стратегия',
                vipPriority: 'Приоритет VIP',
                enabled: 'Включён',
                serverGroups: 'Группы серверов',
                strategies: 'Стратегии балансировки',
                roundRobin: 'Последовательный перебор',
                leastConnections: 'Меньше всего игроков',
                leastTpsLoad: 'Лучший TPS',
                priorityQueue: 'VIP первые'
            },
            settings: {
                title: 'Настройки',
                subtitle: 'Конфигурация системы',
                security: 'Безопасность',
                securityDesc: 'JWT токены, API ключи',
                network: 'Сеть',
                networkDesc: 'Порты, таймауты, сжатие',
                metrics: 'Метрики',
                metricsDesc: 'Prometheus, логи'
            },
            login: {
                username: 'Имя пользователя',
                password: 'Пароль',
                signIn: 'Войти',
                signingIn: 'Вход...',
                invalidCredentials: 'Неверные данные'
            },
            common: {
                loading: 'Загрузка...',
                nodes: 'нод',
                version: 'v1.0.0'
            }
        }
    }
}

const savedLang = localStorage.getItem('language') || 'en'

i18n
    .use(initReactI18next)
    .init({
        resources,
        lng: savedLang,
        fallbackLng: 'en',
        interpolation: {
            escapeValue: false
        }
    })

export default i18n
