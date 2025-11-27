package dev.bnacar.distributedratelimiter.adaptive;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserBehaviorModelerTest {

    private UserBehaviorModeler modeler;

    @BeforeEach
    void setUp() {
        modeler = new UserBehaviorModeler();
    }

    @Test
    void testGetCurrentBehavior_NoHistory() {
        // Act
        UserBehavior behavior = modeler.getCurrentBehavior("test:key");

        // Assert
        assertNotNull(behavior);
        assertEquals(0.0, behavior.getAverageRequestRate());
        assertEquals(0.0, behavior.getBurstiness());
        assertEquals(0.0, behavior.getSessionDuration());
        assertEquals(0.0, behavior.getAnomalyScore());
    }

    @Test
    void testRecordRequest_BuildsHistory() {
        // Arrange
        String key = "test:key";

        // Record some requests
        for (int i = 0; i < 50; i++) {
            modeler.recordRequest(key, 1, true);
        }

        // Act
        UserBehavior behavior = modeler.getCurrentBehavior(key);

        // Assert
        assertNotNull(behavior);
        assertTrue(behavior.getAverageRequestRate() >= 0);
        assertTrue(behavior.getSessionDuration() >= 0);
    }

    @Test
    void testBurstinessCalculation() {
        // Arrange
        String key = "test:key";

        // Record requests with varying intervals to create burstiness
        for (int i = 0; i < 20; i++) {
            modeler.recordRequest(key, 1, true);
        }

        // Act
        UserBehavior behavior = modeler.getCurrentBehavior(key);

        // Assert
        assertNotNull(behavior);
        assertTrue(behavior.getBurstiness() >= 0);
    }

    @Test
    void testTimeOfDayPattern() {
        // Arrange
        String key = "test:key";

        // Record requests
        for (int i = 0; i < 30; i++) {
            modeler.recordRequest(key, 1, true);
        }

        // Act
        UserBehavior behavior = modeler.getCurrentBehavior(key);

        // Assert
        assertNotNull(behavior);
        assertNotNull(behavior.getTimeOfDayPattern());
        assertTrue(behavior.getTimeOfDayPattern().getPeakHour() >= 0);
        assertTrue(behavior.getTimeOfDayPattern().getPeakHour() < 24);
    }

    @Test
    void testClearHistory() {
        // Arrange
        String key = "test:key";
        modeler.recordRequest(key, 1, true);

        // Act
        modeler.clearHistory(key);
        UserBehavior behavior = modeler.getCurrentBehavior(key);

        // Assert
        assertEquals(0.0, behavior.getAverageRequestRate());
    }

    @Test
    void testAnomalyScore_InsufficientData() {
        // Arrange
        String key = "test:key";

        // Record only a few requests (less than minimum for anomaly detection)
        for (int i = 0; i < 5; i++) {
            modeler.recordRequest(key, 1, true);
        }

        // Act
        UserBehavior behavior = modeler.getCurrentBehavior(key);

        // Assert - should return 0 due to insufficient data
        assertEquals(0.0, behavior.getAnomalyScore());
    }
}
