package dev.bnacar.distributedratelimiter.ratelimit;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Composite rate limiter that combines multiple rate limiting algorithms
 * with configurable combination logic.
 */
public class CompositeRateLimiter implements RateLimiter {
    
    private final List<LimitComponent> components;
    private final CombinationLogic combinationLogic;
    private final Map<String, Double> componentWeights;
    
    public CompositeRateLimiter(List<LimitComponent> components, CombinationLogic combinationLogic) {
        if (components == null || components.isEmpty()) {
            throw new IllegalArgumentException("Composite rate limiter must have at least one component");
        }
        
        this.components = new ArrayList<>(components);
        this.combinationLogic = combinationLogic;
        this.componentWeights = components.stream()
                .collect(Collectors.toMap(LimitComponent::getName, LimitComponent::getWeight));
        
        // Sort by priority for priority-based and hierarchical evaluation
        if (combinationLogic == CombinationLogic.PRIORITY_BASED || 
            combinationLogic == CombinationLogic.HIERARCHICAL_AND) {
            this.components.sort(Comparator.comparingInt(LimitComponent::getPriority).reversed());
        }
    }
    
    @Override
    public boolean tryConsume(int tokens) {
        switch (combinationLogic) {
            case ALL_MUST_PASS:
                return tryConsumeAllMustPass(tokens);
            case ANY_CAN_PASS:
                return tryConsumeAnyCanPass(tokens);
            case WEIGHTED_AVERAGE:
                return tryConsumeWeightedAverage(tokens);
            case HIERARCHICAL_AND:
                return tryConsumeHierarchical(tokens);
            case PRIORITY_BASED:
                return tryConsumePriorityBased(tokens);
            default:
                throw new IllegalStateException("Unknown combination logic: " + combinationLogic);
        }
    }
    
    /**
     * ALL_MUST_PASS: All components must allow the request.
     */
    private boolean tryConsumeAllMustPass(int tokens) {
        // First, check if all would allow without consuming
        for (LimitComponent component : components) {
            // Create a test consumption to check availability without side effects
            if (!wouldAllow(component, tokens)) {
                return false;
            }
        }
        
        // If all would allow, consume from all
        boolean allConsumed = true;
        for (LimitComponent component : components) {
            if (!component.tryConsume(tokens)) {
                allConsumed = false;
                break;
            }
        }
        
        return allConsumed;
    }
    
    /**
     * ANY_CAN_PASS: At least one component must allow the request.
     */
    private boolean tryConsumeAnyCanPass(int tokens) {
        for (LimitComponent component : components) {
            if (component.tryConsume(tokens)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * WEIGHTED_AVERAGE: Calculate weighted score and allow if above threshold.
     */
    private boolean tryConsumeWeightedAverage(int tokens) {
        double totalWeight = 0.0;
        double weightedScore = 0.0;
        
        for (LimitComponent component : components) {
            double weight = component.getWeight();
            totalWeight += weight;
            
            if (wouldAllow(component, tokens)) {
                weightedScore += weight;
            }
        }
        
        // Allow if weighted average is >= 50%
        boolean allowed = (weightedScore / totalWeight) >= 0.5;
        
        if (allowed) {
            // Consume from components that would allow
            for (LimitComponent component : components) {
                if (wouldAllow(component, tokens)) {
                    component.tryConsume(tokens);
                }
            }
        }
        
        return allowed;
    }
    
    /**
     * HIERARCHICAL_AND: Check components in hierarchical order (by scope).
     */
    private boolean tryConsumeHierarchical(int tokens) {
        // Group by scope and process in order: USER -> TENANT -> GLOBAL
        Map<String, List<LimitComponent>> scopeGroups = components.stream()
                .collect(Collectors.groupingBy(c -> c.getScope() != null ? c.getScope() : "GLOBAL"));
        
        String[] scopeOrder = {"USER", "TENANT", "GLOBAL"};
        
        for (String scope : scopeOrder) {
            List<LimitComponent> scopeComponents = scopeGroups.get(scope);
            if (scopeComponents != null) {
                for (LimitComponent component : scopeComponents) {
                    if (!component.tryConsume(tokens)) {
                        return false;
                    }
                }
            }
        }
        
        // Process any remaining scopes not in the predefined order
        for (Map.Entry<String, List<LimitComponent>> entry : scopeGroups.entrySet()) {
            if (!Arrays.asList(scopeOrder).contains(entry.getKey())) {
                for (LimitComponent component : entry.getValue()) {
                    if (!component.tryConsume(tokens)) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    /**
     * PRIORITY_BASED: Check highest priority components first, fail fast.
     */
    private boolean tryConsumePriorityBased(int tokens) {
        // Components are already sorted by priority (highest first)
        for (LimitComponent component : components) {
            if (!component.tryConsume(tokens)) {
                return false; // Fail fast on first denial
            }
        }
        return true;
    }
    
    /**
     * Check if a component would allow the request without consuming tokens.
     * This is a simple heuristic based on available tokens.
     */
    private boolean wouldAllow(LimitComponent component, int tokens) {
        return component.getCurrentTokens() >= tokens;
    }
    
    @Override
    public int getCurrentTokens() {
        // Return minimum available tokens across all components
        return components.stream()
                .mapToInt(LimitComponent::getCurrentTokens)
                .min()
                .orElse(0);
    }
    
    @Override
    public int getCapacity() {
        // Return sum of all component capacities
        return components.stream()
                .mapToInt(component -> component.getRateLimiter().getCapacity())
                .sum();
    }
    
    @Override
    public int getRefillRate() {
        // Return average refill rate across components
        return (int) components.stream()
                .mapToInt(component -> component.getRateLimiter().getRefillRate())
                .average()
                .orElse(0);
    }
    
    @Override
    public long getLastRefillTime() {
        // Return most recent refill time across components
        return components.stream()
                .mapToLong(component -> component.getRateLimiter().getLastRefillTime())
                .max()
                .orElse(System.currentTimeMillis());
    }
    
    /**
     * Get all components in this composite rate limiter.
     */
    public List<LimitComponent> getComponents() {
        return Collections.unmodifiableList(components);
    }
    
    /**
     * Get the combination logic used by this composite rate limiter.
     */
    public CombinationLogic getCombinationLogic() {
        return combinationLogic;
    }
    
    /**
     * Get component weights map.
     */
    public Map<String, Double> getComponentWeights() {
        return Collections.unmodifiableMap(componentWeights);
    }
}