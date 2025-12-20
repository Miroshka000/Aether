package miroshka.aether.web.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsMessageContext;
import miroshka.aether.web.WebServer.AetherWebContext;
import miroshka.aether.web.WebServer.ServerDto;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class WebSocketHandler {

    private final AetherWebContext context;
    private final ObjectMapper mapper;
    private final ScheduledExecutorService scheduler;
    private final Map<WsContext, ScheduledFuture<?>> scheduledTasks;
    private final Set<WsContext> clients;

    public WebSocketHandler(AetherWebContext context) {
        this.context = Objects.requireNonNull(context, "context");
        this.mapper = new ObjectMapper();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.scheduledTasks = new ConcurrentHashMap<>();
        this.clients = ConcurrentHashMap.newKeySet();
    }

    public void onConnect(WsContext ctx) {
        clients.add(ctx);
        sendState(ctx);
        scheduleUpdates(ctx);
    }

    public void onMessage(WsMessageContext ctx) {
        String message = ctx.message();
        try {
            WsMessage wsMessage = mapper.readValue(message, WsMessage.class);
            handleMessage(ctx, wsMessage);
        } catch (JsonProcessingException ignored) {
        }
    }

    public void onClose(WsContext ctx) {
        clients.remove(ctx);
        ScheduledFuture<?> task = scheduledTasks.remove(ctx);
        if (task != null) {
            task.cancel(false);
        }
    }

    private void handleMessage(WsContext ctx, WsMessage message) {
        switch (message.type()) {
            case "refresh" -> sendState(ctx);
            case "subscribe", "unsubscribe" -> {
            }
            default -> {
            }
        }
    }

    private void sendState(WsContext ctx) {
        try {
            StateUpdate update = new StateUpdate(
                    "state",
                    context.getGlobalOnline(),
                    context.getGlobalMaxPlayers(),
                    context.getServerCount(),
                    context.getServers(),
                    System.currentTimeMillis());
            ctx.send(mapper.writeValueAsString(update));
        } catch (JsonProcessingException ignored) {
        }
    }

    private void scheduleUpdates(WsContext ctx) {
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
            if (ctx.session.isOpen()) {
                sendState(ctx);
            }
        }, 5, 5, TimeUnit.SECONDS);
        scheduledTasks.put(ctx, task);
    }

    public void broadcastUpdate(Map<String, Object> data) {
        try {
            String json = mapper.writeValueAsString(data);
            for (WsContext client : clients) {
                if (client.session.isOpen()) {
                    client.send(json);
                }
            }
        } catch (JsonProcessingException ignored) {
        }
    }

    public void shutdown() {
        scheduler.shutdown();
        scheduledTasks.values().forEach(task -> task.cancel(false));
        scheduledTasks.clear();
    }

    public record WsMessage(String type, Map<String, Object> payload) {
    }

    public record StateUpdate(
            String type,
            int globalOnline,
            int globalMaxPlayers,
            int serverCount,
            List<ServerDto> servers,
            long timestamp) {
    }
}
