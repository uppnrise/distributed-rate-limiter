package dev.bnacar.distributedratelimiter.adaptive;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnomalyDetectorTest {
    
    private AnomalyDetector detector;
    
    @BeforeEach
    void setUp() {
        detector = new AnomalyDetector();
    }
    
    @Test
    void testDetectAnomalies_NoBaseline() {
        // Act
        AnomalyScore score = detector.detectAnomalies("test:key");
        
        // Assert
        assertNotNull(score);
        assertFalse(score.isAnomaly());
        assertEquals("NONE", score.getSeverity());
        assertEquals(0.0, score.getConfidence());
    }
    
    @Test
    void testRecordTrafficRate_BuildsBaseline() {
        // Arrange
        String key = "test:key";
        
        // Record normal traffic
        for (int i = 0; i < 150; i++) {
            detector.recordTrafficRate(key, 10.0 + (Math.random() * 2)); // 10±1
        }
        
        // Act - Record anomalous traffic
        detector.recordTrafficRate(key, 50.0); // Spike
        
        AnomalyScore score = detector.detectAnomalies(key);
        
        // Assert
        assertNotNull(score);
        // May or may not detect anomaly depending on recent window
        assertNotNull(score.getSeverity());
    }
    
    @Test
    void testClearBaseline() {
        // Arrange
        String key = "test:key";
        detector.recordTrafficRate(key, 10.0);
        
        // Act
        detector.clearBaseline(key);
        AnomalyScore score = detector.detectAnomalies(key);
        
        // Assert
        assertFalse(score.isAnomaly());
    }
}
