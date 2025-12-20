package miroshka.aether.server.luckperms;

import miroshka.aether.api.luckperms.LuckPermsIntegration;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class LuckPermsService implements LuckPermsIntegration {

    private static final Logger LOGGER = LoggerFactory.getLogger(LuckPermsService.class);

    private volatile LuckPerms luckPerms;
    private volatile boolean available;
    private final Map<UUID, Integer> priorityCache;

    public LuckPermsService() {
        this.priorityCache = new ConcurrentHashMap<>();
        tryInitialize();
    }

    private void tryInitialize() {
        try {
            luckPerms = LuckPermsProvider.get();
            available = true;
            LOGGER.info("LuckPerms integration initialized");
        } catch (IllegalStateException e) {
            available = false;
            LOGGER.warn("LuckPerms not available, integration disabled");
        }
    }

    @Override
    public boolean isAvailable() {
        return available && luckPerms != null;
    }

    @Override
    public CompletableFuture<Collection<String>> getPlayerGroups(UUID playerUuid) {
        if (!isAvailable()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        return luckPerms.getUserManager().loadUser(playerUuid)
                .thenApply(user -> user.getInheritedGroups(user.getQueryOptions())
                        .stream()
                        .map(Group::getName)
                        .toList());
    }

    @Override
    public CompletableFuture<Optional<String>> getPrimaryGroup(UUID playerUuid) {
        if (!isAvailable()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return luckPerms.getUserManager().loadUser(playerUuid)
                .thenApply(user -> Optional.ofNullable(user.getPrimaryGroup()));
    }

    @Override
    public CompletableFuture<Boolean> hasPermission(UUID playerUuid, String permission) {
        if (!isAvailable()) {
            return CompletableFuture.completedFuture(false);
        }

        return luckPerms.getUserManager().loadUser(playerUuid)
                .thenApply(user -> user.getCachedData()
                        .getPermissionData()
                        .checkPermission(permission)
                        .asBoolean());
    }

    @Override
    public CompletableFuture<Boolean> isInGroup(UUID playerUuid, String groupName) {
        return getPlayerGroups(playerUuid)
                .thenApply(groups -> groups.contains(groupName));
    }

    @Override
    public CompletableFuture<Integer> getGroupWeight(String groupName) {
        if (!isAvailable()) {
            return CompletableFuture.completedFuture(0);
        }

        return luckPerms.getGroupManager().loadGroup(groupName)
                .thenApply(groupOpt -> groupOpt
                        .map(group -> group.getWeight().orElse(0))
                        .orElse(0));
    }

    @Override
    public CompletableFuture<Optional<String>> getGroupPrefix(String groupName) {
        if (!isAvailable()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return luckPerms.getGroupManager().loadGroup(groupName)
                .thenApply(groupOpt -> groupOpt
                        .flatMap(group -> Optional.ofNullable(group.getCachedData().getMetaData().getPrefix())));
    }

    @Override
    public CompletableFuture<Optional<String>> getGroupSuffix(String groupName) {
        if (!isAvailable()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return luckPerms.getGroupManager().loadGroup(groupName)
                .thenApply(groupOpt -> groupOpt
                        .flatMap(group -> Optional.ofNullable(group.getCachedData().getMetaData().getSuffix())));
    }

    @Override
    public CompletableFuture<Optional<String>> getPlayerPrefix(UUID playerUuid) {
        if (!isAvailable()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return luckPerms.getUserManager().loadUser(playerUuid)
                .thenApply(user -> Optional.ofNullable(
                        user.getCachedData().getMetaData().getPrefix()));
    }

    @Override
    public CompletableFuture<Optional<String>> getPlayerSuffix(UUID playerUuid) {
        if (!isAvailable()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return luckPerms.getUserManager().loadUser(playerUuid)
                .thenApply(user -> Optional.ofNullable(
                        user.getCachedData().getMetaData().getSuffix()));
    }

    @Override
    public int getPlayerPriority(UUID playerUuid) {
        return priorityCache.getOrDefault(playerUuid, 0);
    }

    public void updatePriorityCache(UUID playerUuid) {
        if (!isAvailable()) {
            return;
        }

        getPlayerGroups(playerUuid).thenAccept(groups -> {
            int maxPriority = 0;
            for (String group : groups) {
                int weight = getGroupWeight(group).join();
                if (weight > maxPriority) {
                    maxPriority = weight;
                }
            }
            priorityCache.put(playerUuid, maxPriority);
        });
    }

    public void clearPriorityCache(UUID playerUuid) {
        priorityCache.remove(playerUuid);
    }
}
