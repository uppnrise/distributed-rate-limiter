package dev.bnacar.distributedratelimiter.ratelimit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Configuration for composite rate limiting with multiple algorithm support.
 */
public class CompositeRateLimitConfig {
    
    private final List<LimitDefinition> limits;
    private final CombinationLogic combinationLogic;
    private final Map<String, Double> weights;
    private final boolean hierarchical;
    
    public CompositeRateLimitConfig() {
        this.limits = null;
        this.combinationLogic = CombinationLogic.ALL_MUST_PASS;
        this.weights = null;
        this.hierarchical = false;
    }
    
    public CompositeRateLimitConfig(List<LimitDefinition> limits, CombinationLogic combinationLogic) {
        this(limits, combinationLogic, null, false);
    }
    
    @JsonCreator
    public CompositeRateLimitConfig(@JsonProperty("limits") List<LimitDefinition> limits, 
                                  @JsonProperty("combinationLogic") CombinationLogic combinationLogic,
                                  @JsonProperty("weights") Map<String, Double> weights,
                                  @JsonProperty("hierarchical") boolean hierarchical) {
        this.limits = limits;
        this.combinationLogic = combinationLogic != null ? combinationLogic : CombinationLogic.ALL_MUST_PASS;
        this.weights = weights;
        this.hierarchical = hierarchical;
    }
    
    /**
     * Definition of a single limit component within a composite configuration.
     */
    public static class LimitDefinition {
        private final String name;
        private final RateLimitAlgorithm algorithm;
        private final String scope; // USER, TENANT, GLOBAL
        private final int capacity;
        private final int refillRate;
        private final double weight;
        private final int priority;
        private final String keyPattern; // For hierarchical limiting
        
        public LimitDefinition() {
            this.name = null;
            this.algorithm = RateLimitAlgorithm.TOKEN_BUCKET;
            this.scope = null;
            this.capacity = 10;
            this.refillRate = 1;
            this.weight = 1.0;
            this.priority = 0;
            this.keyPattern = null;
        }
        
        public LimitDefinition(String name, RateLimitAlgorithm algorithm, int capacity, int refillRate) {
            this(name, algorithm, null, capacity, refillRate, 1.0, 0, null);
        }
        
        @JsonCreator
        public LimitDefinition(@JsonProperty("name") String name, 
                              @JsonProperty("algorithm") RateLimitAlgorithm algorithm, 
                              @JsonProperty("scope") String scope, 
                              @JsonProperty("capacity") int capacity, 
                              @JsonProperty("refillRate") int refillRate, 
                              @JsonProperty("weight") double weight, 
                              @JsonProperty("priority") int priority, 
                              @JsonProperty("keyPattern") String keyPattern) {
            this.name = name;
            this.algorithm = algorithm != null ? algorithm : RateLimitAlgorithm.TOKEN_BUCKET;
            this.scope = scope;
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.weight = weight > 0 ? weight : 1.0;
            this.priority = priority;
            this.keyPattern = keyPattern;
        }
        
        // Getters
        public String getName() { return name; }
        public RateLimitAlgorithm getAlgorithm() { return algorithm; }
        public String getScope() { return scope; }
        public int getCapacity() { return capacity; }
        public int getRefillRate() { return refillRate; }
        public double getWeight() { return weight; }
        public int getPriority() { return priority; }
        public String getKeyPattern() { return keyPattern; }
        
        @Override
        public String toString() {
            return "LimitDefinition{" +
                    "name='" + name + '\'' +
                    ", algorithm=" + algorithm +
                    ", scope='" + scope + '\'' +
                    ", capacity=" + capacity +
                    ", refillRate=" + refillRate +
                    ", weight=" + weight +
                    ", priority=" + priority +
                    ", keyPattern='" + keyPattern + '\'' +
                    '}';
        }
    }
    
    public List<LimitDefinition> getLimits() { return limits; }
    public CombinationLogic getCombinationLogic() { return combinationLogic; }
    public Map<String, Double> getWeights() { return weights; }
    public boolean isHierarchical() { return hierarchical; }
    
    @Override
    public String toString() {
        return "CompositeRateLimitConfig{" +
                "limits=" + limits +
                ", combinationLogic=" + combinationLogic +
                ", weights=" + weights +
                ", hierarchical=" + hierarchical +
                '}';
    }
}