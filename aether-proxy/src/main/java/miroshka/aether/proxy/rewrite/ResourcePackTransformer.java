package miroshka.aether.proxy.rewrite;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import miroshka.aether.common.protocol.rewrite.PacketTransformer;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ResourcePackTransformer implements PacketTransformer {

    private static final int RESOURCE_PACKS_INFO_PACKET_ID = 0x06;

    private final Map<String, ResourcePackOverride> serverOverrides;
    private final Map<UUID, ResourcePackOverride> playerOverrides;

    public ResourcePackTransformer() {
        this.serverOverrides = new ConcurrentHashMap<>();
        this.playerOverrides = new ConcurrentHashMap<>();
    }

    @Override
    public String id() {
        return "resource-pack";
    }

    @Override
    public boolean shouldTransform(TransformContext context) {
        if (!context.isDownstream()) {
            return false;
        }

        return context.packetId() == RESOURCE_PACKS_INFO_PACKET_ID &&
                (serverOverrides.containsKey(context.sourceServer()) ||
                        playerOverrides.containsKey(context.playerUuid()));
    }

    @Override
    public ByteBuf transform(ByteBuf packet, TransformContext context) {
        ResourcePackOverride override = playerOverrides.get(context.playerUuid());
        if (override == null) {
            override = serverOverrides.get(context.sourceServer());
        }

        if (override == null) {
            return packet;
        }

        return rewriteResourcePack(packet, override);
    }

    @Override
    public int priority() {
        return 50;
    }

    public void setServerResourcePack(String serverName, ResourcePackOverride override) {
        Objects.requireNonNull(serverName, "serverName");
        Objects.requireNonNull(override, "override");
        serverOverrides.put(serverName, override);
    }

    public void removeServerResourcePack(String serverName) {
        serverOverrides.remove(serverName);
    }

    public void setPlayerResourcePack(UUID playerUuid, ResourcePackOverride override) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(override, "override");
        playerOverrides.put(playerUuid, override);
    }

    public void removePlayerResourcePack(UUID playerUuid) {
        playerOverrides.remove(playerUuid);
    }

    public void clearAll() {
        serverOverrides.clear();
        playerOverrides.clear();
    }

    private ByteBuf rewriteResourcePack(ByteBuf original, ResourcePackOverride override) {
        original.release();

        ByteBuf result = Unpooled.buffer();
        result.writeByte(override.mustAccept() ? 1 : 0);
        result.writeBoolean(override.hasScripts());
        writePackInfo(result, override);

        return result;
    }

    private void writePackInfo(ByteBuf buffer, ResourcePackOverride override) {
        writeShort(buffer, 1);
        writeString(buffer, override.packId());
        writeString(buffer, override.packVersion());
        writeLong(buffer, override.packSize());
        writeString(buffer, override.contentKey());
        writeString(buffer, override.subPackName());
        writeString(buffer, override.contentId());
        buffer.writeBoolean(override.hasScripts());
        buffer.writeBoolean(override.isRtxCapable());
    }

    private void writeString(ByteBuf buffer, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(buffer, bytes.length);
        buffer.writeBytes(bytes);
    }

    private void writeShort(ByteBuf buffer, int value) {
        buffer.writeShortLE(value);
    }

    private void writeLong(ByteBuf buffer, long value) {
        buffer.writeLongLE(value);
    }

    private void writeVarInt(ByteBuf buffer, int value) {
        while ((value & ~0x7F) != 0) {
            buffer.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        buffer.writeByte(value);
    }

    public record ResourcePackOverride(
            String packId,
            String packVersion,
            long packSize,
            String contentKey,
            String subPackName,
            String contentId,
            boolean mustAccept,
            boolean hasScripts,
            boolean isRtxCapable) {

        public static ResourcePackOverride of(String packId, String packVersion, long packSize) {
            return new ResourcePackOverride(packId, packVersion, packSize, "", "", "", true, false, false);
        }

        public static ResourcePackOverride of(String packId, String packVersion, long packSize, boolean mustAccept) {
            return new ResourcePackOverride(packId, packVersion, packSize, "", "", "", mustAccept, false, false);
        }
    }
}
