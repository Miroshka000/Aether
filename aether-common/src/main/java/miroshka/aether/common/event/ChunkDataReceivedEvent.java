package miroshka.aether.common.event;

import miroshka.aether.common.protocol.ChunkDataPacket;

import java.util.Objects;

public record ChunkDataReceivedEvent(ChunkDataPacket packet) implements AetherEvent {

    public ChunkDataReceivedEvent {
        Objects.requireNonNull(packet, "packet");
    }

    public static ChunkDataReceivedEvent of(ChunkDataPacket packet) {
        return new ChunkDataReceivedEvent(packet);
    }
}
