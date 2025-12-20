package miroshka.aether.common.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import miroshka.aether.common.protocol.Packet;
import miroshka.aether.common.protocol.PacketRegistry;
import miroshka.aether.common.protocol.ProtocolConstants;
import miroshka.aether.common.protocol.ProtocolErrorPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class PacketDecoder extends ByteToMessageDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(PacketDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        Objects.requireNonNull(ctx, "ctx");
        Objects.requireNonNull(in, "in");
        Objects.requireNonNull(out, "out");

        if (in.readableBytes() < ProtocolConstants.FRAME_LENGTH_FIELD_SIZE) {
            return;
        }

        in.markReaderIndex();
        int totalLength = in.readInt();

        if (totalLength < 0 || totalLength > ProtocolConstants.MAX_FRAME_SIZE) {
            LOGGER.error("Invalid frame length: {}", totalLength);
            ctx.close();
            return;
        }

        if (in.readableBytes() < totalLength) {
            in.resetReaderIndex();
            return;
        }

        byte flags = in.readByte();
        int packetId = in.readInt();
        int payloadLength = totalLength - ProtocolConstants.FRAME_FLAGS_SIZE - ProtocolConstants.FRAME_PACKET_ID_SIZE;

        byte[] rawPayload = new byte[payloadLength];
        in.readBytes(rawPayload);

        byte[] payload = isCompressed(flags) ? Snappy.uncompress(rawPayload) : rawPayload;
        ByteBuf payloadBuffer = Unpooled.wrappedBuffer(payload);

        try {
            Optional<Packet> packetOpt = PacketRegistry.instance().decode(packetId, payloadBuffer);
            if (packetOpt.isPresent()) {
                out.add(packetOpt.get());
            } else {
                LOGGER.warn("Unknown packet ID: 0x{}", Integer.toHexString(packetId));
                out.add(ProtocolErrorPacket.unknownPacket(packetId));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to decode packet 0x{}: {}", Integer.toHexString(packetId), e.getMessage());
            out.add(ProtocolErrorPacket.malformedPacket(packetId, e.getMessage()));
        } finally {
            payloadBuffer.release();
        }
    }

    private boolean isCompressed(byte flags) {
        return (flags & ProtocolConstants.FLAG_COMPRESSION_ENABLED) != 0;
    }
}
