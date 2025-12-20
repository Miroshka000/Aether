package miroshka.aether.common.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import miroshka.aether.common.protocol.Packet;
import miroshka.aether.common.protocol.ProtocolConstants;
import org.xerial.snappy.Snappy;

import java.util.Objects;

public final class PacketEncoder extends MessageToByteEncoder<Packet> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Packet packet, ByteBuf out) throws Exception {
        Objects.requireNonNull(ctx, "ctx");
        Objects.requireNonNull(packet, "packet");
        Objects.requireNonNull(out, "out");

        ByteBuf payloadBuffer = ctx.alloc().buffer();
        try {
            packet.encode(payloadBuffer);
            byte[] payload = new byte[payloadBuffer.readableBytes()];
            payloadBuffer.readBytes(payload);

            byte flags = buildFlags(packet, payload.length);
            byte[] finalPayload = shouldCompress(flags) ? Snappy.compress(payload) : payload;

            int totalLength = ProtocolConstants.FRAME_FLAGS_SIZE
                    + ProtocolConstants.FRAME_PACKET_ID_SIZE
                    + finalPayload.length;

            out.writeInt(totalLength);
            out.writeByte(flags);
            out.writeInt(packet.packetId());
            out.writeBytes(finalPayload);
        } finally {
            payloadBuffer.release();
        }
    }

    private byte buildFlags(Packet packet, int payloadLength) {
        byte flags = 0;
        if (payloadLength > ProtocolConstants.COMPRESSION_THRESHOLD) {
            flags |= ProtocolConstants.FLAG_COMPRESSION_ENABLED;
        }
        if (packet.priority() == Packet.Priority.CRITICAL) {
            flags |= ProtocolConstants.FLAG_PRIORITY_CRITICAL;
        }
        return flags;
    }

    private boolean shouldCompress(byte flags) {
        return (flags & ProtocolConstants.FLAG_COMPRESSION_ENABLED) != 0;
    }
}
