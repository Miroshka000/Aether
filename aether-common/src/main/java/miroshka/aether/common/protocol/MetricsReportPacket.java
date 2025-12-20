package miroshka.aether.common.protocol;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record MetricsReportPacket(
        List<DataPoint> tpsHistory,
        List<DataPoint> playerHistory,
        Map<String, Long> customCounters) implements Packet {

    public MetricsReportPacket {
        Objects.requireNonNull(tpsHistory, "tpsHistory");
        Objects.requireNonNull(playerHistory, "playerHistory");
        Objects.requireNonNull(customCounters, "customCounters");
        tpsHistory = List.copyOf(tpsHistory);
        playerHistory = List.copyOf(playerHistory);
    }

    @Override
    public int packetId() {
        return PacketIds.METRICS_REPORT;
    }

    @Override
    public void encode(ByteBuf buffer) {
        PacketHelper.writeVarInt(buffer, tpsHistory.size());
        for (DataPoint point : tpsHistory) {
            point.encode(buffer);
        }
        PacketHelper.writeVarInt(buffer, playerHistory.size());
        for (DataPoint point : playerHistory) {
            point.encode(buffer);
        }
        PacketHelper.writeVarInt(buffer, customCounters.size());
        for (Map.Entry<String, Long> entry : customCounters.entrySet()) {
            PacketHelper.writeString(buffer, entry.getKey());
            PacketHelper.writeLong(buffer, entry.getValue());
        }
    }

    @Override
    public Priority priority() {
        return Priority.LOW;
    }

    public static MetricsReportPacket decode(ByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");
        int tpsSize = PacketHelper.readVarInt(buffer);
        List<DataPoint> tpsHistory = new ArrayList<>(tpsSize);
        for (int i = 0; i < tpsSize; i++) {
            tpsHistory.add(DataPoint.decode(buffer));
        }
        int playerSize = PacketHelper.readVarInt(buffer);
        List<DataPoint> playerHistory = new ArrayList<>(playerSize);
        for (int i = 0; i < playerSize; i++) {
            playerHistory.add(DataPoint.decode(buffer));
        }
        int counterSize = PacketHelper.readVarInt(buffer);
        Map<String, Long> customCounters = new java.util.HashMap<>(counterSize);
        for (int i = 0; i < counterSize; i++) {
            String key = PacketHelper.readString(buffer);
            long value = PacketHelper.readLong(buffer);
            customCounters.put(key, value);
        }
        return new MetricsReportPacket(
                Collections.unmodifiableList(tpsHistory),
                Collections.unmodifiableList(playerHistory),
                Collections.unmodifiableMap(customCounters));
    }

    public record DataPoint(long timestamp, double value) {

        public void encode(ByteBuf buffer) {
            PacketHelper.writeLong(buffer, timestamp);
            PacketHelper.writeDouble(buffer, value);
        }

        public static DataPoint decode(ByteBuf buffer) {
            Objects.requireNonNull(buffer, "buffer");
            long timestamp = PacketHelper.readLong(buffer);
            double value = PacketHelper.readDouble(buffer);
            return new DataPoint(timestamp, value);
        }
    }
}
