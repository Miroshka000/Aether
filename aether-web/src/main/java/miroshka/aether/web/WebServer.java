package miroshka.aether.web;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.websocket.WsContext;
import miroshka.aether.web.controller.AuthController;
import miroshka.aether.web.controller.DashboardController;
import miroshka.aether.web.security.JwtService;
import miroshka.aether.web.ws.WebSocketHandler;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class WebServer {

    private final Javalin app;
    private final JwtService jwtService;
    private final WebSocketHandler wsHandler;
    private final Set<WsContext> wsClients;
    private final int port;
    private final AetherWebContext context;

    public WebServer(int port, String jwtSecret, AetherWebContext context) {
        this.port = port;
        this.context = context;
        this.jwtService = new JwtService(jwtSecret);
        this.wsHandler = new WebSocketHandler(context);
        this.wsClients = ConcurrentHashMap.newKeySet();

        this.app = Javalin.create(config -> {
            config.staticFiles.add("/static", Location.CLASSPATH);
            config.jsonMapper(new JacksonMapper());
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
        });

        registerRoutes();
    }

    private void registerRoutes() {
        AuthController authController = new AuthController(jwtService);
        DashboardController dashboardController = new DashboardController(context);

        app.before("/api/*", ctx -> {
            if (ctx.path().equals("/api/auth/login")) {
                return;
            }
            String token = ctx.header("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                ctx.status(401).result("Unauthorized");
                return;
            }
            String jwt = token.substring(7);
            if (!jwtService.validateToken(jwt)) {
                ctx.status(401).result("Invalid token");
            }
        });

        app.post("/api/auth/login", authController::login);
        app.post("/api/auth/refresh", authController::refresh);
        app.post("/api/auth/change-password", authController::changePassword);
        app.get("/api/auth/me", authController::me);

        app.get("/api/dashboard/overview", dashboardController::getOverview);
        app.get("/api/dashboard/servers", dashboardController::getServers);
        app.get("/api/dashboard/players", dashboardController::getPlayers);
        app.get("/api/dashboard/metrics", dashboardController::getMetrics);

        app.get("/api/portals", dashboardController::getPortals);
        app.get("/api/events", dashboardController::getEvents);

        app.get("/api/config/balancer", ctx -> ctx.json(context.getBalancerConfig()));
        app.post("/api/config/balancer", ctx -> {
            var config = ctx.bodyAsClass(BalancerConfigDto.class);
            context.setBalancerConfig(config);
            ctx.json(Map.of("success", true));
        });

        app.ws("/ws", ws -> {
            ws.onConnect(ctx -> {
                wsClients.add(ctx);
                wsHandler.onConnect(ctx);
            });
            ws.onMessage(wsHandler::onMessage);
            ws.onClose(ctx -> {
                wsClients.remove(ctx);
                wsHandler.onClose(ctx);
            });
        });
    }

    public void start() {
        app.start(port);
    }

    public void stop() {
        app.stop();
    }

    public void broadcast(String message) {
        for (WsContext client : wsClients) {
            client.send(message);
        }
    }

    public Javalin getApp() {
        return app;
    }

    public interface AetherWebContext {
        int getGlobalOnline();

        int getGlobalMaxPlayers();

        int getServerCount();

        List<ServerDto> getServers();

        List<PlayerDto> getPlayers();

        List<PortalDto> getPortals();

        List<EventDto> getEvents();

        Map<String, Object> getMetrics();

        BalancerConfigDto getBalancerConfig();

        void setBalancerConfig(BalancerConfigDto config);
    }

    public record ServerDto(String name, int online, int maxPlayers, double tps, boolean available) {
    }

    public record PlayerDto(String name, String uuid, String server, long connectedAt, int ping) {
    }

    public record PortalDto(String id, String sourceServer, String targetServer, String type, boolean enabled) {
    }

    public record EventDto(String type, long count, boolean active, long lastTriggered) {
    }

    public record BalancerConfigDto(String strategy, boolean vipPriority, List<String> serverGroups) {
    }
}
