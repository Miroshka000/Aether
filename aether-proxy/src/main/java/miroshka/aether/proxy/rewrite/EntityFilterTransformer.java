package miroshka.aether.proxy.rewrite;

import io.netty.buffer.ByteBuf;
import miroshka.aether.common.protocol.rewrite.PacketTransformer;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EntityFilterTransformer implements PacketTransformer {

    private static final int ADD_ENTITY_PACKET_ID = 0x0d;
    private static final int REMOVE_ENTITY_PACKET_ID = 0x0e;

    private final Map<String, Set<Long>> hiddenEntities;
    private final Map<UUID, Set<Long>> playerHiddenEntities;

    public EntityFilterTransformer() {
        this.hiddenEntities = new ConcurrentHashMap<>();
        this.playerHiddenEntities = new ConcurrentHashMap<>();
    }

    @Override
    public String id() {
        return "entity-filter";
    }

    @Override
    public boolean shouldTransform(TransformContext context) {
        if (!context.isDownstream()) {
            return false;
        }

        int packetId = context.packetId();
        return packetId == ADD_ENTITY_PACKET_ID || packetId == REMOVE_ENTITY_PACKET_ID;
    }

    @Override
    public ByteBuf transform(ByteBuf packet, TransformContext context) {
        long entityId = readEntityId(packet);

        if (isEntityHidden(context.targetServer(), entityId) ||
                isEntityHiddenForPlayer(context.playerUuid(), entityId)) {
            packet.clear();
            return packet;
        }

        return packet;
    }

    @Override
    public int priority() {
        return 100;
    }

    public void hideEntity(String serverName, long entityId) {
        hiddenEntities.computeIfAbsent(serverName, k -> ConcurrentHashMap.newKeySet())
                .add(entityId);
    }

    public void showEntity(String serverName, long entityId) {
        Set<Long> entities = hiddenEntities.get(serverName);
        if (entities != null) {
            entities.remove(entityId);
        }
    }

    public void hideEntityForPlayer(UUID playerUuid, long entityId) {
        playerHiddenEntities.computeIfAbsent(playerUuid, k -> new HashSet<>())
                .add(entityId);
    }

    public void showEntityForPlayer(UUID playerUuid, long entityId) {
        Set<Long> entities = playerHiddenEntities.get(playerUuid);
        if (entities != null) {
            entities.remove(entityId);
        }
    }

    public void clearHiddenEntities(String serverName) {
        hiddenEntities.remove(serverName);
    }

    public void clearPlayerHiddenEntities(UUID playerUuid) {
        playerHiddenEntities.remove(playerUuid);
    }

    private boolean isEntityHidden(String serverName, long entityId) {
        Set<Long> entities = hiddenEntities.get(serverName);
        return entities != null && entities.contains(entityId);
    }

    private boolean isEntityHiddenForPlayer(UUID playerUuid, long entityId) {
        Set<Long> entities = playerHiddenEntities.get(playerUuid);
        return entities != null && entities.contains(entityId);
    }

    private long readEntityId(ByteBuf packet) {
        int readerIndex = packet.readerIndex();
        try {
            return readVarLong(packet);
        } finally {
            packet.readerIndex(readerIndex);
        }
    }

    private long readVarLong(ByteBuf buffer) {
        long result = 0;
        int shift = 0;
        byte b;
        do {
            b = buffer.readByte();
            result |= (long) (b & 0x7F) << shift;
            shift += 7;
        } while ((b & 0x80) != 0);
        return result;
    }
}
