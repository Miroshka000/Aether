package miroshka.aether.api.balancer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoadBalancerTest {

    @Test
    void testBalancingStrategyValues() {
        assertNotNull(LoadBalancer.BalancingStrategy.ROUND_ROBIN);
        assertNotNull(LoadBalancer.BalancingStrategy.LEAST_CONNECTIONS);
        assertNotNull(LoadBalancer.BalancingStrategy.LEAST_TPS_LOAD);
        assertNotNull(LoadBalancer.BalancingStrategy.RANDOM);
        assertNotNull(LoadBalancer.BalancingStrategy.PRIORITY_QUEUE);

        assertEquals(5, LoadBalancer.BalancingStrategy.values().length);
    }

    @Test
    void testServerMetricsRecord() {
        LoadBalancer.ServerMetrics metrics = new LoadBalancer.ServerMetrics(
                "lobby",
                50,
                100,
                19.5,
                2,
                System.currentTimeMillis(),
                true);

        assertEquals("lobby", metrics.serverName());
        assertEquals(50, metrics.onlinePlayers());
        assertEquals(100, metrics.maxPlayers());
        assertEquals(19.5, metrics.tps());
        assertEquals(2, metrics.weight());
        assertTrue(metrics.available());
    }

    @Test
    void testServerMetricsIsHealthy() {
        LoadBalancer.ServerMetrics healthy = new LoadBalancer.ServerMetrics("s1", 50, 100, 19.0, 1, System.currentTimeMillis(), true);
        LoadBalancer.ServerMetrics lowTps = new LoadBalancer.ServerMetrics("s2", 50, 100, 10.0, 1, System.currentTimeMillis(), true);
        LoadBalancer.ServerMetrics unavailable = new LoadBalancer.ServerMetrics("s3", 50, 100, 20.0, 1, System.currentTimeMillis(), false);

        assertTrue(healthy.tps() >= 15);
        assertTrue(lowTps.tps() < 15);
        assertFalse(unavailable.available());
    }
}
