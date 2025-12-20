package miroshka.aether.api.pdc;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface DistributedPDC {

    <T> void set(UUID playerUuid, String key, T value, PDCSerializer<T> serializer);

    <T> Optional<T> get(UUID playerUuid, String key, PDCSerializer<T> serializer);

    <T> T getOrDefault(UUID playerUuid, String key, T defaultValue, PDCSerializer<T> serializer);

    boolean has(UUID playerUuid, String key);

    void remove(UUID playerUuid, String key);

    Map<String, byte[]> getAll(UUID playerUuid);

    CompletableFuture<Void> sync(UUID playerUuid);

    CompletableFuture<Void> load(UUID playerUuid);

    void invalidateCache(UUID playerUuid);

    void setConflictResolver(ConflictResolver resolver);

    interface PDCSerializer<T> {
        byte[] serialize(T value);

        T deserialize(byte[] data);

        Class<T> getType();
    }

    @FunctionalInterface
    interface ConflictResolver {
        byte[] resolve(String key, byte[] localValue, byte[] remoteValue, long localVersion, long remoteVersion);

        static ConflictResolver lastWriteWins() {
            return (key, local, remote, localVer, remoteVer) -> remoteVer > localVer ? remote : local;
        }

        static ConflictResolver localWins() {
            return (key, local, remote, localVer, remoteVer) -> local;
        }

        static ConflictResolver remoteWins() {
            return (key, local, remote, localVer, remoteVer) -> remote;
        }
    }

    final class Serializers {
        private Serializers() {
        }

        public static final PDCSerializer<String> STRING = new PDCSerializer<>() {
            @Override
            public byte[] serialize(String value) {
                return value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            }

            @Override
            public String deserialize(byte[] data) {
                return new String(data, java.nio.charset.StandardCharsets.UTF_8);
            }

            @Override
            public Class<String> getType() {
                return String.class;
            }
        };

        public static final PDCSerializer<Integer> INTEGER = new PDCSerializer<>() {
            @Override
            public byte[] serialize(Integer value) {
                return java.nio.ByteBuffer.allocate(4).putInt(value).array();
            }

            @Override
            public Integer deserialize(byte[] data) {
                return java.nio.ByteBuffer.wrap(data).getInt();
            }

            @Override
            public Class<Integer> getType() {
                return Integer.class;
            }
        };

        public static final PDCSerializer<Long> LONG = new PDCSerializer<>() {
            @Override
            public byte[] serialize(Long value) {
                return java.nio.ByteBuffer.allocate(8).putLong(value).array();
            }

            @Override
            public Long deserialize(byte[] data) {
                return java.nio.ByteBuffer.wrap(data).getLong();
            }

            @Override
            public Class<Long> getType() {
                return Long.class;
            }
        };

        public static final PDCSerializer<Double> DOUBLE = new PDCSerializer<>() {
            @Override
            public byte[] serialize(Double value) {
                return java.nio.ByteBuffer.allocate(8).putDouble(value).array();
            }

            @Override
            public Double deserialize(byte[] data) {
                return java.nio.ByteBuffer.wrap(data).getDouble();
            }

            @Override
            public Class<Double> getType() {
                return Double.class;
            }
        };

        public static final PDCSerializer<Boolean> BOOLEAN = new PDCSerializer<>() {
            @Override
            public byte[] serialize(Boolean value) {
                return new byte[] { (byte) (value ? 1 : 0) };
            }

            @Override
            public Boolean deserialize(byte[] data) {
                return data[0] != 0;
            }

            @Override
            public Class<Boolean> getType() {
                return Boolean.class;
            }
        };

        public static final PDCSerializer<byte[]> BYTES = new PDCSerializer<>() {
            @Override
            public byte[] serialize(byte[] value) {
                return value;
            }

            @Override
            public byte[] deserialize(byte[] data) {
                return data;
            }

            @Override
            public Class<byte[]> getType() {
                return byte[].class;
            }
        };
    }
}
