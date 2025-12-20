package miroshka.aether.api.luckperms;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface LuckPermsIntegration {

    boolean isAvailable();

    CompletableFuture<Collection<String>> getPlayerGroups(UUID playerUuid);

    CompletableFuture<Optional<String>> getPrimaryGroup(UUID playerUuid);

    CompletableFuture<Boolean> hasPermission(UUID playerUuid, String permission);

    CompletableFuture<Boolean> isInGroup(UUID playerUuid, String groupName);

    CompletableFuture<Integer> getGroupWeight(String groupName);

    CompletableFuture<Optional<String>> getGroupPrefix(String groupName);

    CompletableFuture<Optional<String>> getGroupSuffix(String groupName);

    CompletableFuture<Optional<String>> getPlayerPrefix(UUID playerUuid);

    CompletableFuture<Optional<String>> getPlayerSuffix(UUID playerUuid);

    int getPlayerPriority(UUID playerUuid);

    default CompletableFuture<Boolean> hasAnyGroup(UUID playerUuid, Collection<String> groups) {
        return getPlayerGroups(playerUuid)
                .thenApply(playerGroups -> groups.stream().anyMatch(playerGroups::contains));
    }

    default CompletableFuture<Boolean> hasAllGroups(UUID playerUuid, Collection<String> groups) {
        return getPlayerGroups(playerUuid)
                .thenApply(playerGroups -> playerGroups.containsAll(groups));
    }
}
