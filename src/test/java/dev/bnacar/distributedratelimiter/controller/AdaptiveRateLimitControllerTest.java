package dev.bnacar.distributedratelimiter.controller;

import dev.bnacar.distributedratelimiter.adaptive.AdaptiveRateLimitEngine;
import dev.bnacar.distributedratelimiter.models.AdaptiveStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdaptiveRateLimitControllerTest {
    
    @Mock
    private AdaptiveRateLimitEngine adaptiveEngine;
    
    private AdaptiveRateLimitController controller;
    
    @BeforeEach
    void setUp() {
        controller = new AdaptiveRateLimitController(adaptiveEngine);
        // Set default test configuration values
        ReflectionTestUtils.setField(controller, "adaptiveEnabled", false);
        ReflectionTestUtils.setField(controller, "evaluationIntervalMs", 300000L);
        ReflectionTestUtils.setField(controller, "minConfidenceThreshold", 0.7);
        ReflectionTestUtils.setField(controller, "maxAdjustmentFactor", 2.0);
        ReflectionTestUtils.setField(controller, "minCapacity", 10);
        ReflectionTestUtils.setField(controller, "maxCapacity", 100000);
    }
    
    @Test
    void testGetAdaptiveStatus_Success() {
        // Arrange
        String key = "test:key";
        Map<String, String> reasoning = new HashMap<>();
        reasoning.put("decision", "System stable");
        
        AdaptiveRateLimitEngine.AdaptiveStatusInfo mockStatus = 
            new AdaptiveRateLimitEngine.AdaptiveStatusInfo(
                "ADAPTIVE",
                0.85,
                100,
                10,
                120,
                12,
                reasoning
            );
        
        when(adaptiveEngine.getStatus(key)).thenReturn(mockStatus);
        
        // Act
        ResponseEntity<AdaptiveStatus> response = controller.getAdaptiveStatus(key);
        
        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(key, response.getBody().getKey());
        assertEquals(120, response.getBody().getCurrentLimits().getCapacity());
        assertEquals(12, response.getBody().getCurrentLimits().getRefillRate());
        assertEquals("ADAPTIVE", response.getBody().getAdaptiveStatus().getMode());
        assertEquals(0.85, response.getBody().getAdaptiveStatus().getConfidence());
        
        verify(adaptiveEngine, times(1)).getStatus(key);
    }
    
    @Test
    void testOverrideAdaptation_Success() {
        // Arrange
        String key = "test:key";
        AdaptiveRateLimitController.AdaptationOverrideRequest request = 
            new AdaptiveRateLimitController.AdaptationOverrideRequest(200, 20, "Test override");
        
        // Act
        ResponseEntity<Void> response = controller.overrideAdaptation(key, request);
        
        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        
        verify(adaptiveEngine, times(1)).setOverride(eq(key), any(AdaptiveRateLimitEngine.AdaptationOverride.class));
    }
    
    @Test
    void testRemoveOverride_Success() {
        // Arrange
        String key = "test:key";
        
        // Act
        ResponseEntity<Void> response = controller.removeOverride(key);
        
        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        
        verify(adaptiveEngine, times(1)).removeOverride(key);
    }
    
    @Test
    void testGetAdaptiveConfig_Success() {
        // Act
        ResponseEntity<AdaptiveRateLimitController.AdaptiveConfigResponse> response = 
            controller.getAdaptiveConfig();
        
        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        // Note: enabled is false by default for safety - users must explicitly opt-in
        assertFalse(response.getBody().isEnabled());
        assertEquals(300000L, response.getBody().getEvaluationIntervalMs());
        assertEquals(0.7, response.getBody().getMinConfidenceThreshold());
        assertEquals(2.0, response.getBody().getMaxAdjustmentFactor());
    }
}
