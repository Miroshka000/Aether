package miroshka.aether.common.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PacketEncodingTest {

    @Test
    void testHeartbeatPacketEncodeDecode() {
        HeartbeatPacket original = HeartbeatPacket.create(42);

        ByteBuf buffer = Unpooled.buffer();
        original.encode(buffer);

        HeartbeatPacket decoded = HeartbeatPacket.decode(buffer);

        assertEquals(original.sequenceId(), decoded.sequenceId());
        assertEquals(original.timestamp(), decoded.timestamp());
        buffer.release();
    }

    @Test
    void testEventBroadcastPacketEncodeDecode() {
        EventBroadcastPacket original = new EventBroadcastPacket(
                "PlayerJoin",
                "lobby",
                UUID.randomUUID(),
                "TestPlayer",
                List.of("vip", "admin"),
                Map.of("key", "value"));

        ByteBuf buffer = Unpooled.buffer();
        original.encode(buffer);

        EventBroadcastPacket decoded = EventBroadcastPacket.decode(buffer);

        assertEquals(original.eventType(), decoded.eventType());
        assertEquals(original.sourceServer(), decoded.sourceServer());
        assertEquals(original.playerUuid(), decoded.playerUuid());
        assertEquals(original.playerName(), decoded.playerName());
        assertEquals(original.playerGroups(), decoded.playerGroups());
        assertEquals(original.eventData().get("key"), decoded.eventData().get("key"));
        buffer.release();
    }

    @Test
    void testTransferRequestPacketEncodeDecode() {
        TransferRequestPacket original = new TransferRequestPacket(
                UUID.randomUUID(),
                "TestPlayer",
                "lobby",
                "survival",
                true,
                100.5,
                64.0,
                -200.3);

        ByteBuf buffer = Unpooled.buffer();
        original.encode(buffer);

        TransferRequestPacket decoded = TransferRequestPacket.decode(buffer);

        assertEquals(original.playerUuid(), decoded.playerUuid());
        assertEquals(original.playerName(), decoded.playerName());
        assertEquals(original.sourceServer(), decoded.sourceServer());
        assertEquals(original.targetServer(), decoded.targetServer());
        assertEquals(original.seamless(), decoded.seamless());
        assertEquals(original.x(), decoded.x(), 0.01);
        assertEquals(original.y(), decoded.y(), 0.01);
        assertEquals(original.z(), decoded.z(), 0.01);
        buffer.release();
    }

    @Test
    void testPacketRegistry() {
        PacketRegistry registry = PacketRegistry.instance();

        assertTrue(registry.isRegistered(HeartbeatPacket.class));
        assertTrue(registry.isRegistered(EventBroadcastPacket.class));
        assertTrue(registry.isRegistered(TransferRequestPacket.class));

        assertEquals(PacketIds.HEARTBEAT, registry.getPacketId(HeartbeatPacket.class));
        assertEquals(PacketIds.EVENT_BROADCAST, registry.getPacketId(EventBroadcastPacket.class));
    }
}
