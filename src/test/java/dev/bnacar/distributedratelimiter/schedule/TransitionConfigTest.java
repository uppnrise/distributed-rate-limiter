package dev.bnacar.distributedratelimiter.schedule;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TransitionConfig model.
 */
class TransitionConfigTest {
    
    @Test
    void testDefaultConstructor() {
        TransitionConfig config = new TransitionConfig();
        assertEquals(0, config.getRampUpMinutes());
        assertEquals(0, config.getRampDownMinutes());
    }
    
    @Test
    void testParameterizedConstructor() {
        TransitionConfig config = new TransitionConfig(15, 30);
        assertEquals(15, config.getRampUpMinutes());
        assertEquals(30, config.getRampDownMinutes());
    }
    
    @Test
    void testSetters() {
        TransitionConfig config = new TransitionConfig();
        config.setRampUpMinutes(10);
        config.setRampDownMinutes(20);
        
        assertEquals(10, config.getRampUpMinutes());
        assertEquals(20, config.getRampDownMinutes());
    }
    
    @Test
    void testToString() {
        TransitionConfig config = new TransitionConfig(5, 10);
        String str = config.toString();
        
        assertTrue(str.contains("rampUpMinutes=5"));
        assertTrue(str.contains("rampDownMinutes=10"));
    }
}
