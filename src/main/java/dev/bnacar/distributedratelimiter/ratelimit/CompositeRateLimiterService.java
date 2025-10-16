package dev.bnacar.distributedratelimiter.ratelimit;

import dev.bnacar.distributedratelimiter.models.CompositeRateLimitResponse;
import dev.bnacar.distributedratelimiter.models.CompositeRateLimitResponse.ComponentResult;
import dev.bnacar.distributedratelimiter.models.CompositeRateLimitResponse.CombinationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for handling composite rate limiting operations.
 * Provides detailed results for each component in the composite rate limiter.
 */
@Service
public class CompositeRateLimiterService {
    
    private final RateLimiterService rateLimiterService;
    
    @Autowired
    public CompositeRateLimiterService(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }
    
    /**
     * Check composite rate limits with detailed component results.
     */
    public CompositeRateLimitResponse checkCompositeRateLimit(String key, int tokens, CompositeRateLimitConfig config) {
        if (config == null) {
            // Fall back to regular rate limiting if no composite config provided
            boolean allowed = rateLimiterService.isAllowed(key, tokens);
            return createSimpleCompositeResponse(key, tokens, allowed);
        }
        
        // Create rate limiters for each component
        List<LimitComponent> components = createLimitComponents(key, config);
        CompositeRateLimiter compositeRateLimiter = new CompositeRateLimiter(components, config.getCombinationLogic());
        
        // Collect component results before attempting consumption
        Map<String, ComponentResult> componentResults = new HashMap<>();
        for (LimitComponent component : components) {
            int currentTokens = component.getCurrentTokens();
            boolean wouldAllow = currentTokens >= tokens;
            componentResults.put(component.getName(), new ComponentResult(
                wouldAllow, currentTokens, component.getRateLimiter().getCapacity(), component.getScope()
            ));
        }
        
        // Attempt to consume tokens
        boolean allowed = compositeRateLimiter.tryConsume(tokens);
        
        // Determine which component caused the limitation (if any)
        String limitingComponent = null;
        if (!allowed) {
            limitingComponent = findLimitingComponent(componentResults, config.getCombinationLogic());
        }
        
        // Create combination result with scoring
        CombinationResult combinationResult = createCombinationResult(
            config.getCombinationLogic(), componentResults, config.getWeights()
        );
        
        return new CompositeRateLimitResponse(
            key, tokens, allowed, componentResults, limitingComponent, combinationResult
        );
    }
    
    /**
     * Create rate limiter components from configuration.
     */
    private List<LimitComponent> createLimitComponents(String baseKey, CompositeRateLimitConfig config) {
        List<LimitComponent> components = new ArrayList<>();
        
        for (CompositeRateLimitConfig.LimitDefinition limitDef : config.getLimits()) {
            String componentKey = createComponentKey(baseKey, limitDef);
            RateLimiter rateLimiter = createRateLimiterFromDefinition(limitDef);
            
            LimitComponent component = new LimitComponent(
                limitDef.getName(),
                rateLimiter,
                limitDef.getWeight(),
                limitDef.getPriority(),
                limitDef.getScope()
            );
            
            components.add(component);
        }
        
        return components;
    }
    
    /**
     * Create component-specific key for rate limiting.
     */
    private String createComponentKey(String baseKey, CompositeRateLimitConfig.LimitDefinition limitDef) {
        if (limitDef.getKeyPattern() != null) {
            return limitDef.getKeyPattern().replace("{key}", baseKey);
        }
        return baseKey + ":" + limitDef.getName();
    }
    
    /**
     * Create rate limiter instance from limit definition.
     */
    private RateLimiter createRateLimiterFromDefinition(CompositeRateLimitConfig.LimitDefinition limitDef) {
        switch (limitDef.getAlgorithm()) {
            case TOKEN_BUCKET:
                return new TokenBucket(limitDef.getCapacity(), limitDef.getRefillRate());
            case SLIDING_WINDOW:
                return new SlidingWindow(limitDef.getCapacity(), limitDef.getRefillRate());
            case FIXED_WINDOW:
                return new FixedWindow(limitDef.getCapacity(), limitDef.getRefillRate());
            case LEAKY_BUCKET:
                return new LeakyBucket(limitDef.getCapacity(), limitDef.getRefillRate());
            default:
                throw new IllegalArgumentException("Unsupported algorithm in composite: " + limitDef.getAlgorithm());
        }
    }
    
    /**
     * Find which component caused the rate limiting.
     */
    private String findLimitingComponent(Map<String, ComponentResult> componentResults, CombinationLogic logic) {
        switch (logic) {
            case ALL_MUST_PASS:
            case HIERARCHICAL_AND:
            case PRIORITY_BASED:
                // Find first component that would not allow
                return componentResults.entrySet().stream()
                    .filter(entry -> !entry.getValue().isAllowed())
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
            case ANY_CAN_PASS:
                // If none allowed, return the first one
                return componentResults.keySet().iterator().next();
            case WEIGHTED_AVERAGE:
                // Return component with lowest score
                return componentResults.entrySet().stream()
                    .filter(entry -> !entry.getValue().isAllowed())
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
            default:
                return null;
        }
    }
    
    /**
     * Create combination result with scoring information.
     */
    private CombinationResult createCombinationResult(CombinationLogic logic, 
                                                     Map<String, ComponentResult> componentResults,
                                                     Map<String, Double> weights) {
        Map<String, Double> componentScores = new HashMap<>();
        double overallScore = 0.0;
        
        switch (logic) {
            case ALL_MUST_PASS:
            case HIERARCHICAL_AND:
            case PRIORITY_BASED:
                // Score is 1.0 if all pass, 0.0 if any fail
                boolean allPass = componentResults.values().stream().allMatch(ComponentResult::isAllowed);
                overallScore = allPass ? 1.0 : 0.0;
                componentResults.forEach((name, result) -> 
                    componentScores.put(name, result.isAllowed() ? 1.0 : 0.0));
                break;
                
            case ANY_CAN_PASS:
                // Score is 1.0 if any pass, 0.0 if all fail
                boolean anyPass = componentResults.values().stream().anyMatch(ComponentResult::isAllowed);
                overallScore = anyPass ? 1.0 : 0.0;
                componentResults.forEach((name, result) -> 
                    componentScores.put(name, result.isAllowed() ? 1.0 : 0.0));
                break;
                
            case WEIGHTED_AVERAGE:
                // Calculate weighted average
                double totalWeight = 0.0;
                double weightedSum = 0.0;
                
                for (Map.Entry<String, ComponentResult> entry : componentResults.entrySet()) {
                    String name = entry.getKey();
                    ComponentResult result = entry.getValue();
                    double weight = weights != null ? weights.getOrDefault(name, 1.0) : 1.0;
                    
                    totalWeight += weight;
                    double score = result.isAllowed() ? 1.0 : 0.0;
                    weightedSum += score * weight;
                    componentScores.put(name, score);
                }
                
                overallScore = totalWeight > 0 ? weightedSum / totalWeight : 0.0;
                break;
        }
        
        return new CombinationResult(logic, overallScore, componentScores);
    }
    
    /**
     * Create simple composite response for non-composite requests.
     */
    private CompositeRateLimitResponse createSimpleCompositeResponse(String key, int tokens, boolean allowed) {
        Map<String, ComponentResult> componentResults = new HashMap<>();
        componentResults.put("default", new ComponentResult(allowed, allowed ? tokens : 0, tokens, "PRIMARY"));
        
        CombinationResult combinationResult = new CombinationResult(
            CombinationLogic.ALL_MUST_PASS, 
            allowed ? 1.0 : 0.0, 
            Map.of("default", allowed ? 1.0 : 0.0)
        );
        
        return new CompositeRateLimitResponse(
            key, tokens, allowed, componentResults, allowed ? null : "default", combinationResult
        );
    }
}