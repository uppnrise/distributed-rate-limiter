package dev.bnacar.distributedratelimiter.ratelimit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompositeRateLimiterTest {
    
    private LimitComponent tokenBucketComponent;
    private LimitComponent slidingWindowComponent;
    
    @BeforeEach
    void setUp() {
        tokenBucketComponent = new LimitComponent(
            "token_bucket",
            new TokenBucket(10, 2),
            1.0, 1, "PRIMARY"
        );
        
        slidingWindowComponent = new LimitComponent(
            "sliding_window", 
            new SlidingWindow(5, 1),
            1.0, 2, "SECONDARY"
        );
    }
    
    @Test
    @DisplayName("Should create composite rate limiter with valid components")
    void shouldCreateCompositeRateLimiterWithValidComponents() {
        List<LimitComponent> components = Arrays.asList(tokenBucketComponent, slidingWindowComponent);
        
        CompositeRateLimiter composite = new CompositeRateLimiter(components, CombinationLogic.ALL_MUST_PASS);
        
        assertNotNull(composite);
        assertEquals(CombinationLogic.ALL_MUST_PASS, composite.getCombinationLogic());
        assertEquals(2, composite.getComponents().size());
    }
    
    @Test
    @DisplayName("Should fail to create composite rate limiter with empty components")
    void shouldFailToCreateCompositeRateLimiterWithEmptyComponents() {
        assertThrows(IllegalArgumentException.class, () -> {
            new CompositeRateLimiter(Arrays.asList(), CombinationLogic.ALL_MUST_PASS);
        });
    }
    
    @Test
    @DisplayName("ALL_MUST_PASS: Should allow when all components allow")
    void allMustPass_shouldAllowWhenAllComponentsAllow() {
        List<LimitComponent> components = Arrays.asList(tokenBucketComponent, slidingWindowComponent);
        CompositeRateLimiter composite = new CompositeRateLimiter(components, CombinationLogic.ALL_MUST_PASS);
        
        // Both components should have enough capacity for 1 token
        assertTrue(composite.tryConsume(1));
    }
    
    @Test
    @DisplayName("ALL_MUST_PASS: Should deny when any component denies")
    void allMustPass_shouldDenyWhenAnyComponentDenies() {
        List<LimitComponent> components = Arrays.asList(tokenBucketComponent, slidingWindowComponent);
        CompositeRateLimiter composite = new CompositeRateLimiter(components, CombinationLogic.ALL_MUST_PASS);
        
        // Exhaust the sliding window component (capacity 5)
        for (int i = 0; i < 5; i++) {
            composite.tryConsume(1);
        }
        
        // Next request should be denied because sliding window is exhausted
        assertFalse(composite.tryConsume(1));
    }
    
    @Test
    @DisplayName("ANY_CAN_PASS: Should allow when at least one component allows")
    void anyCanPass_shouldAllowWhenAtLeastOneComponentAllows() {
        List<LimitComponent> components = Arrays.asList(tokenBucketComponent, slidingWindowComponent);
        CompositeRateLimiter composite = new CompositeRateLimiter(components, CombinationLogic.ANY_CAN_PASS);
        
        // Consume tokens to make sliding window deny but token bucket still allow
        for (int i = 0; i < 5; i++) {
            composite.tryConsume(1);
        }
        
        // Should still allow because token bucket has capacity
        assertTrue(composite.tryConsume(1));
    }
    
    @Test
    @DisplayName("WEIGHTED_AVERAGE: Should calculate weighted scores correctly")
    void weightedAverage_shouldCalculateWeightedScoresCorrectly() {
        // Create components with different weights
        LimitComponent heavyWeight = new LimitComponent(
            "heavy", new TokenBucket(10, 2), 3.0, 1
        );
        LimitComponent lightWeight = new LimitComponent(
            "light", new TokenBucket(1, 1), 1.0, 2  // Will be exhausted quickly
        );
        
        List<LimitComponent> components = Arrays.asList(heavyWeight, lightWeight);
        CompositeRateLimiter composite = new CompositeRateLimiter(components, CombinationLogic.WEIGHTED_AVERAGE);
        
        // First request should pass (both components allow)
        assertTrue(composite.tryConsume(1));
        
        // Light component should now be exhausted, but heavy component has high weight
        // Weighted score: (3.0 * 1 + 1.0 * 0) / (3.0 + 1.0) = 0.75 >= 0.5
        assertTrue(composite.tryConsume(1));
    }
    
    @Test
    @DisplayName("Should return correct capacity as sum of components")
    void shouldReturnCorrectCapacityAsSumOfComponents() {
        List<LimitComponent> components = Arrays.asList(tokenBucketComponent, slidingWindowComponent);
        CompositeRateLimiter composite = new CompositeRateLimiter(components, CombinationLogic.ALL_MUST_PASS);
        
        // Token bucket: 10, Sliding window: 5
        assertEquals(15, composite.getCapacity());
    }
    
    @Test
    @DisplayName("Should return minimum available tokens across components")
    void shouldReturnMinimumAvailableTokensAcrossComponents() {
        List<LimitComponent> components = Arrays.asList(tokenBucketComponent, slidingWindowComponent);
        CompositeRateLimiter composite = new CompositeRateLimiter(components, CombinationLogic.ALL_MUST_PASS);
        
        // Initially, sliding window has 5 tokens (minimum)
        assertEquals(5, composite.getCurrentTokens());
        
        // After consuming 3 tokens, sliding window should have 2 (still minimum)
        composite.tryConsume(3);
        assertEquals(2, composite.getCurrentTokens());
    }
    
    @Test
    @DisplayName("Should handle hierarchical combination logic with scopes")
    void shouldHandleHierarchicalCombinationLogicWithScopes() {
        LimitComponent userComponent = new LimitComponent(
            "user", new TokenBucket(5, 1), 1.0, 1, "USER"
        );
        LimitComponent tenantComponent = new LimitComponent(
            "tenant", new TokenBucket(20, 5), 1.0, 2, "TENANT"  
        );
        LimitComponent globalComponent = new LimitComponent(
            "global", new TokenBucket(100, 10), 1.0, 3, "GLOBAL"
        );
        
        List<LimitComponent> components = Arrays.asList(userComponent, tenantComponent, globalComponent);
        CompositeRateLimiter composite = new CompositeRateLimiter(components, CombinationLogic.HIERARCHICAL_AND);
        
        // Should process in hierarchical order and succeed
        assertTrue(composite.tryConsume(1));
        
        // Exhaust user limit
        for (int i = 0; i < 4; i++) {
            composite.tryConsume(1);
        }
        
        // Should now be limited by user scope
        assertFalse(composite.tryConsume(1));
    }
}