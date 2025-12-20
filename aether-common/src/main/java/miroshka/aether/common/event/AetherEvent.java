package miroshka.aether.common.event;

public sealed interface AetherEvent permits
                ConnectionEstablishedEvent,
                ConnectionLostEvent,
                PacketReceivedEvent,
                PacketSentEvent,
                NetworkStateUpdatedEvent,
                CircuitBreakerTriggeredEvent,
                AuthenticationCompletedEvent,
                ChunkDataReceivedEvent,
                PDCSyncReceivedEvent,
                EventBroadcastReceivedEvent {
}
