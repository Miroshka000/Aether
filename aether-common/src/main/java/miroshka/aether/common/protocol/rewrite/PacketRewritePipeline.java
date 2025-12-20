package miroshka.aether.common.protocol.rewrite;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PacketRewritePipeline {

    private final List<PacketTransformer> transformers;
    private volatile boolean enabled;

    public PacketRewritePipeline() {
        this.transformers = new CopyOnWriteArrayList<>();
        this.enabled = true;
    }

    public void addTransformer(PacketTransformer transformer) {
        Objects.requireNonNull(transformer, "transformer");
        transformers.add(transformer);
        sortTransformers();
    }

    public void removeTransformer(String id) {
        transformers.removeIf(t -> t.id().equals(id));
    }

    public void removeTransformer(PacketTransformer transformer) {
        transformers.remove(transformer);
    }

    public ByteBuf process(ByteBuf packet, PacketTransformer.TransformContext context) {
        if (!enabled || transformers.isEmpty()) {
            return packet;
        }

        ByteBuf current = packet;
        for (PacketTransformer transformer : transformers) {
            if (transformer.shouldTransform(context)) {
                current = transformer.transform(current, context);
            }
        }

        return current;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<PacketTransformer> getTransformers() {
        return new ArrayList<>(transformers);
    }

    public int getTransformerCount() {
        return transformers.size();
    }

    private void sortTransformers() {
        List<PacketTransformer> sorted = new ArrayList<>(transformers);
        sorted.sort(Comparator.comparingInt(PacketTransformer::priority).reversed());
        transformers.clear();
        transformers.addAll(sorted);
    }
}
