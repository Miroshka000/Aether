package miroshka.aether.common.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PacketHelperTest {

    @Test
    void testWriteAndReadVarInt() {
        ByteBuf buffer = Unpooled.buffer();

        PacketHelper.writeVarInt(buffer, 127);
        PacketHelper.writeVarInt(buffer, 128);
        PacketHelper.writeVarInt(buffer, 300);
        PacketHelper.writeVarInt(buffer, 16384);

        assertEquals(127, PacketHelper.readVarInt(buffer));
        assertEquals(128, PacketHelper.readVarInt(buffer));
        assertEquals(300, PacketHelper.readVarInt(buffer));
        assertEquals(16384, PacketHelper.readVarInt(buffer));

        buffer.release();
    }

    @Test
    void testWriteAndReadString() {
        ByteBuf buffer = Unpooled.buffer();
        String testString = "Hello, Aether!";

        PacketHelper.writeString(buffer, testString);
        String result = PacketHelper.readString(buffer);

        assertEquals(testString, result);
        buffer.release();
    }

    @Test
    void testWriteAndReadMap() {
        ByteBuf buffer = Unpooled.buffer();
        Map<String, String> testMap = Map.of(
                "key1", "value1",
                "key2", "value2");

        PacketHelper.writeMap(buffer, testMap);
        Map<String, String> result = PacketHelper.readMap(buffer);

        assertEquals(testMap.size(), result.size());
        assertEquals(testMap.get("key1"), result.get("key1"));
        assertEquals(testMap.get("key2"), result.get("key2"));
        buffer.release();
    }

    @Test
    void testWriteAndReadLong() {
        ByteBuf buffer = Unpooled.buffer();
        long testValue = 123456789012345L;

        PacketHelper.writeLong(buffer, testValue);
        long result = PacketHelper.readLong(buffer);

        assertEquals(testValue, result);
        buffer.release();
    }

    @Test
    void testWriteAndReadDouble() {
        ByteBuf buffer = Unpooled.buffer();
        double testValue = 3.14159265359;

        PacketHelper.writeDouble(buffer, testValue);
        double result = PacketHelper.readDouble(buffer);

        assertEquals(testValue, result, 0.0001);
        buffer.release();
    }

    @Test
    void testWriteAndReadBoolean() {
        ByteBuf buffer = Unpooled.buffer();

        PacketHelper.writeBoolean(buffer, true);
        PacketHelper.writeBoolean(buffer, false);

        assertTrue(PacketHelper.readBoolean(buffer));
        assertFalse(PacketHelper.readBoolean(buffer));
        buffer.release();
    }
}
