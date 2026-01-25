package miroshka.aether.server.portal;

import java.util.List;
import java.util.Objects;

public record PortalConfig(
        boolean enabled,
        List<PortalDefinition> portals) {

    public PortalConfig {
        Objects.requireNonNull(portals, "portals");
    }

    public static PortalConfig defaults() {
        return new PortalConfig(true, List.of());
    }

    public sealed interface PortalDefinition permits RegionPortalDefinition, BoundaryPortalDefinition {
        String id();
        String targetServer();
        boolean seamless();
        boolean enabled();
    }

    public record RegionPortalDefinition(
            String id,
            String targetServer,
            boolean seamless,
            boolean enabled,
            String world,
            Position min,
            Position max) implements PortalDefinition {
        public RegionPortalDefinition {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(targetServer, "targetServer");
            Objects.requireNonNull(world, "world");
            Objects.requireNonNull(min, "min");
            Objects.requireNonNull(max, "max");
        }
    }

    public record BoundaryPortalDefinition(
            String id,
            String targetServer,
            boolean seamless,
            boolean enabled,
            String world,
            Direction direction,
            int threshold) implements PortalDefinition {
        public BoundaryPortalDefinition {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(targetServer, "targetServer");
            Objects.requireNonNull(world, "world");
            Objects.requireNonNull(direction, "direction");
        }
    }

    public enum Direction {
        NORTH, SOUTH, EAST, WEST, UP, DOWN
    }

    public record Position(int x, int y, int z) {
    }
}
