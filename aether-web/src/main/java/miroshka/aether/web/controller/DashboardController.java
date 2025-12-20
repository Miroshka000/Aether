package miroshka.aether.web.controller;

import io.javalin.http.Context;
import miroshka.aether.web.WebServer.AetherWebContext;
import miroshka.aether.web.WebServer.ServerDto;
import miroshka.aether.web.WebServer.PlayerDto;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DashboardController {

    private final AetherWebContext context;

    public DashboardController(AetherWebContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    public void getOverview(Context ctx) {
        OverviewResponse response = new OverviewResponse(
                context.getGlobalOnline(),
                context.getGlobalMaxPlayers(),
                context.getServerCount(),
                calculateAverageTps(),
                System.currentTimeMillis());
        ctx.json(response);
    }

    public void getServers(Context ctx) {
        List<ServerDto> servers = context.getServers();
        ctx.json(new ServersResponse(servers, servers.size()));
    }

    public void getPlayers(Context ctx) {
        String server = ctx.queryParam("server");
        List<PlayerDto> players = context.getPlayers();

        if (server != null && !server.isEmpty()) {
            players = players.stream()
                    .filter(p -> server.equals(p.server()))
                    .toList();
        }

        ctx.json(new PlayersResponse(players, players.size()));
    }

    public void getMetrics(Context ctx) {
        Map<String, Object> metrics = context.getMetrics();
        ctx.json(metrics);
    }

    private double calculateAverageTps() {
        List<ServerDto> servers = context.getServers();
        if (servers.isEmpty()) {
            return 20.0;
        }
        return servers.stream()
                .mapToDouble(ServerDto::tps)
                .average()
                .orElse(20.0);
    }

    public record OverviewResponse(
            int globalOnline,
            int globalMaxPlayers,
            int serverCount,
            double averageTps,
            long timestamp) {
    }

    public record ServersResponse(List<ServerDto> servers, int count) {
    }

    public record PlayersResponse(List<PlayerDto> players, int count) {
    }
}
