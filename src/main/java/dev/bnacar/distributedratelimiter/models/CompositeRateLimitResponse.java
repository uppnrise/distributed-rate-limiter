package dev.bnacar.distributedratelimiter.models;

import dev.bnacar.distributedratelimiter.ratelimit.CombinationLogic;
import java.util.Map;

/**
 * Extended response for composite rate limiting that includes details
 * about individual component results and combination logic.
 */
public class CompositeRateLimitResponse extends RateLimitResponse {
    
    private final Map<String, ComponentResult> componentResults;
    private final String limitingComponent;
    private final CombinationResult combinationResult;
    
    public CompositeRateLimitResponse(String key, int tokensRequested, boolean allowed,
                                    Map<String, ComponentResult> componentResults,
                                    String limitingComponent,
                                    CombinationResult combinationResult) {
        super(key, tokensRequested, allowed);
        this.componentResults = componentResults;
        this.limitingComponent = limitingComponent;
        this.combinationResult = combinationResult;
    }
    
    /**
     * Result for a single component within the composite rate limiter.
     */
    public static class ComponentResult {
        private final boolean allowed;
        private final int currentTokens;
        private final int capacity;
        private final String scope;
        
        public ComponentResult(boolean allowed, int currentTokens, int capacity, String scope) {
            this.allowed = allowed;
            this.currentTokens = currentTokens;
            this.capacity = capacity;
            this.scope = scope;
        }
        
        public boolean isAllowed() { return allowed; }
        public int getCurrentTokens() { return currentTokens; }
        public int getCapacity() { return capacity; }
        public String getScope() { return scope; }
        
        @Override
        public String toString() {
            return "ComponentResult{" +
                    "allowed=" + allowed +
                    ", currentTokens=" + currentTokens +
                    ", capacity=" + capacity +
                    ", scope='" + scope + '\'' +
                    '}';
        }
    }
    
    /**
     * Overall combination result with scoring details.
     */
    public static class CombinationResult {
        private final CombinationLogic logic;
        private final double overallScore;
        private final Map<String, Double> componentScores;
        
        public CombinationResult(CombinationLogic logic, double overallScore, Map<String, Double> componentScores) {
            this.logic = logic;
            this.overallScore = overallScore;
            this.componentScores = componentScores;
        }
        
        public CombinationLogic getLogic() { return logic; }
        public double getOverallScore() { return overallScore; }
        public Map<String, Double> getComponentScores() { return componentScores; }
        
        @Override
        public String toString() {
            return "CombinationResult{" +
                    "logic=" + logic +
                    ", overallScore=" + overallScore +
                    ", componentScores=" + componentScores +
                    '}';
        }
    }
    
    public Map<String, ComponentResult> getComponentResults() {
        return componentResults;
    }
    
    public String getLimitingComponent() {
        return limitingComponent;
    }
    
    public CombinationResult getCombinationResult() {
        return combinationResult;
    }
    
    @Override
    public String toString() {
        return "CompositeRateLimitResponse{" +
                "key='" + getKey() + '\'' +
                ", tokensRequested=" + getTokensRequested() +
                ", allowed=" + isAllowed() +
                ", componentResults=" + componentResults +
                ", limitingComponent='" + limitingComponent + '\'' +
                ", combinationResult=" + combinationResult +
                '}';
    }
}