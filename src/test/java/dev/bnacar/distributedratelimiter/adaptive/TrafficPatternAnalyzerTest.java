package dev.bnacar.distributedratelimiter.adaptive;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TrafficPatternAnalyzerTest {
    
    private TrafficPatternAnalyzer analyzer;
    
    @BeforeEach
    void setUp() {
        analyzer = new TrafficPatternAnalyzer();
    }
    
    @Test
    void testAnalyzePattern_NoHistory() {
        // Act
        TrafficPattern pattern = analyzer.analyzePattern("test:key");
        
        // Assert
        assertNotNull(pattern);
        assertEquals("test:key", pattern.getKey());
        assertFalse(pattern.getSeasonality().isDetected());
        assertEquals("STABLE", pattern.getTrend().getDirection());
        assertEquals("LOW", pattern.getVolatility().getVolatilityLevel());
        assertEquals(0.3, pattern.getConfidence());
    }
    
    @Test
    void testRecordTrafficEvent_BuildsHistory() {
        // Arrange
        String key = "test:key";
        
        // Record some traffic events
        for (int i = 0; i < 50; i++) {
            analyzer.recordTrafficEvent(key, 5, true);
        }
        
        // Act
        TrafficPattern pattern = analyzer.analyzePattern(key);
        
        // Assert
        assertNotNull(pattern);
        assertEquals(key, pattern.getKey());
        assertTrue(pattern.getConfidence() > 0.3);
        assertNotNull(pattern.getVolatility());
    }
    
    @Test
    void testAnalyzePattern_DetectsTrend() {
        // Arrange
        String key = "test:key";
        
        // Record increasing traffic
        for (int i = 0; i < 50; i++) {
            analyzer.recordTrafficEvent(key, i + 1, true);
        }
        
        // Act
        TrafficPattern pattern = analyzer.analyzePattern(key);
        
        // Assert
        assertNotNull(pattern);
        assertNotNull(pattern.getTrend());
        // Should detect increasing trend
        assertEquals("INCREASING", pattern.getTrend().getDirection());
    }
    
    @Test
    void testClearHistory() {
        // Arrange
        String key = "test:key";
        analyzer.recordTrafficEvent(key, 10, true);
        
        // Act
        analyzer.clearHistory(key);
        TrafficPattern pattern = analyzer.analyzePattern(key);
        
        // Assert
        assertEquals(0.3, pattern.getConfidence()); // Default for no history
    }
}
