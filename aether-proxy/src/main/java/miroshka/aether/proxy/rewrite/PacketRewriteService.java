package miroshka.aether.proxy.rewrite;

import miroshka.aether.common.protocol.rewrite.PacketRewritePipeline;
import miroshka.aether.common.protocol.rewrite.PacketTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class PacketRewriteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PacketRewriteService.class);

    private final PacketRewritePipeline pipeline;
    private final Map<String, PacketTransformer> transformers;

    private final EntityFilterTransformer entityFilterTransformer;
    private final ResourcePackTransformer resourcePackTransformer;

    public PacketRewriteService() {
        this.pipeline = new PacketRewritePipeline();
        this.transformers = new ConcurrentHashMap<>();

        this.entityFilterTransformer = new EntityFilterTransformer();
        this.resourcePackTransformer = new ResourcePackTransformer();

        registerDefaultTransformers();
    }

    private void registerDefaultTransformers() {
        registerTransformer(entityFilterTransformer);
        registerTransformer(resourcePackTransformer);

        LOGGER.info("Registered {} default packet transformers", transformers.size());
    }

    public void registerTransformer(PacketTransformer transformer) {
        transformers.put(transformer.id(), transformer);
        pipeline.addTransformer(transformer);
        LOGGER.debug("Registered transformer: {} (priority {})", transformer.id(), transformer.priority());
    }

    public void unregisterTransformer(String id) {
        PacketTransformer removed = transformers.remove(id);
        if (removed != null) {
            pipeline.removeTransformer(id);
            LOGGER.debug("Unregistered transformer: {}", id);
        }
    }

    public Optional<PacketTransformer> getTransformer(String id) {
        return Optional.ofNullable(transformers.get(id));
    }

    public PacketRewritePipeline getPipeline() {
        return pipeline;
    }

    public EntityFilterTransformer getEntityFilterTransformer() {
        return entityFilterTransformer;
    }

    public ResourcePackTransformer getResourcePackTransformer() {
        return resourcePackTransformer;
    }

    public void setEnabled(boolean enabled) {
        pipeline.setEnabled(enabled);
        LOGGER.info("Packet rewrite pipeline {}", enabled ? "enabled" : "disabled");
    }

    public boolean isEnabled() {
        return pipeline.isEnabled();
    }

    public int getTransformerCount() {
        return transformers.size();
    }
}
