package miroshka.aether.web.controller;

import io.javalin.http.Context;
import miroshka.aether.web.WebServer.AetherWebContext;
import miroshka.aether.web.WebServer.PortalDto;

import java.util.List;
import java.util.Objects;

public final class PortalController {

    private final AetherWebContext context;

    public PortalController(AetherWebContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    public void list(Context ctx) {
        List<PortalDto> portals = context.getPortals();
        ctx.json(new PortalsResponse(portals, portals.size()));
    }

    public void create(Context ctx) {
        PortalDto portal = ctx.bodyAsClass(PortalDto.class);
        context.createPortal(portal);
        ctx.status(201).json(portal);
    }

    public void update(Context ctx) {
        String id = ctx.pathParam("id");
        PortalDto portal = ctx.bodyAsClass(PortalDto.class);
        context.updatePortal(id, portal);
        ctx.json(portal);
    }

    public void delete(Context ctx) {
        String id = ctx.pathParam("id");
        context.deletePortal(id);
        ctx.status(204);
    }

    public record PortalsResponse(List<PortalDto> portals, int count) {
    }
}
