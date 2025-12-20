package miroshka.aether.common.protocol;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class PacketHelper {

    private PacketHelper() {
    }

    public static void writeVarInt(ByteBuf buffer, int value) {
        Objects.requireNonNull(buffer, "buffer");
        while ((value & ~0x7F) != 0) {
            buffer.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        buffer.writeByte(value);
    }

    public static int readVarInt(ByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");
        int result = 0;
        int shift = 0;
        byte currentByte;
        do {
            if (shift >= ProtocolConstants.VARINT_MAX_BYTES * 7) {
                throw new IllegalStateException("VarInt too big");
            }
            currentByte = buffer.readByte();
            result |= (currentByte & 0x7F) << shift;
            shift += 7;
        } while ((currentByte & 0x80) != 0);
        return result;
    }

    public static int varIntSize(int value) {
        int size = 0;
        do {
            size++;
            value >>>= 7;
        } while (value != 0);
        return size;
    }

    public static void writeString(ByteBuf buffer, String value) {
        Objects.requireNonNull(buffer, "buffer");
        Objects.requireNonNull(value, "value");
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(buffer, bytes.length);
        buffer.writeBytes(bytes);
    }

    public static String readString(ByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");
        int length = readVarInt(buffer);
        if (length < 0 || length > ProtocolConstants.MAX_PROPERTY_VALUE_SIZE) {
            throw new IllegalStateException("String too long: " + length);
        }
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static void writePropertyMap(ByteBuf buffer, Map<String, String> properties) {
        Objects.requireNonNull(buffer, "buffer");
        Objects.requireNonNull(properties, "properties");
        if (properties.size() > ProtocolConstants.MAX_PROPERTY_MAP_ENTRIES) {
            throw new IllegalArgumentException("Property map too large: " + properties.size());
        }
        writeVarInt(buffer, properties.size());
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            writeString(buffer, entry.getKey());
            writeString(buffer, entry.getValue());
        }
    }

    public static Map<String, String> readPropertyMap(ByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");
        int size = readVarInt(buffer);
        if (size < 0 || size > ProtocolConstants.MAX_PROPERTY_MAP_ENTRIES) {
            throw new IllegalStateException("Property map too large: " + size);
        }
        if (size == 0) {
            return Collections.emptyMap();
        }
        Map<String, String> result = HashMap.newHashMap(size);
        for (int i = 0; i < size; i++) {
            String key = readString(buffer);
            String value = readString(buffer);
            result.put(key, value);
        }
        return Collections.unmodifiableMap(result);
    }

    public static void writeLong(ByteBuf buffer, long value) {
        Objects.requireNonNull(buffer, "buffer");
        buffer.writeLong(value);
    }

    public static long readLong(ByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");
        return buffer.readLong();
    }

    public static void writeInt(ByteBuf buffer, int value) {
        Objects.requireNonNull(buffer, "buffer");
        buffer.writeInt(value);
    }

    public static int readInt(ByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");
        return buffer.readInt();
    }

    public static void writeDouble(ByteBuf buffer, double value) {
        Objects.requireNonNull(buffer, "buffer");
        buffer.writeDouble(value);
    }

    public static double readDouble(ByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");
        return buffer.readDouble();
    }

    public static void writeBoolean(ByteBuf buffer, boolean value) {
        Objects.requireNonNull(buffer, "buffer");
        buffer.writeBoolean(value);
    }

    public static boolean readBoolean(ByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");
        return buffer.readBoolean();
    }
}
