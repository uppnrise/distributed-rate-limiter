package dev.bnacar.distributedratelimiter.models;

import dev.bnacar.distributedratelimiter.ratelimit.RateLimitAlgorithm;
import dev.bnacar.distributedratelimiter.ratelimit.CompositeRateLimitConfig;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public class RateLimitRequest {
    @NotBlank(message = "Key cannot be blank")
    private String key;
    
    @NotNull(message = "Tokens must be specified")
    @Min(value = 1, message = "Tokens must be at least 1")
    private Integer tokens = 1;
    
    private String apiKey;
    
    private RateLimitAlgorithm algorithm;
    
    private CompositeRateLimitConfig compositeConfig;
    
    private ClientInfo clientInfo;

    public RateLimitRequest() {}

    public RateLimitRequest(String key, Integer tokens) {
        this.key = key;
        this.tokens = tokens;
    }

    public RateLimitRequest(String key, Integer tokens, String apiKey) {
        this.key = key;
        this.tokens = tokens;
        this.apiKey = apiKey;
    }
    
    public RateLimitRequest(String key, Integer tokens, RateLimitAlgorithm algorithm, CompositeRateLimitConfig compositeConfig) {
        this.key = key;
        this.tokens = tokens;
        this.algorithm = algorithm;
        this.compositeConfig = compositeConfig;
    }
    
    public RateLimitRequest(String key, Integer tokens, ClientInfo clientInfo) {
        this.key = key;
        this.tokens = tokens;
        this.clientInfo = clientInfo;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer getTokens() {
        return tokens;
    }

    public void setTokens(Integer tokens) {
        this.tokens = tokens;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public RateLimitAlgorithm getAlgorithm() {
        return algorithm;
    }
    
    public void setAlgorithm(RateLimitAlgorithm algorithm) {
        this.algorithm = algorithm;
    }
    
    public CompositeRateLimitConfig getCompositeConfig() {
        return compositeConfig;
    }
    
    public void setCompositeConfig(CompositeRateLimitConfig compositeConfig) {
        this.compositeConfig = compositeConfig;
    }
    
    public ClientInfo getClientInfo() {
        return clientInfo;
    }
    
    public void setClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
    }
    
    /**
     * Client information for geographic rate limiting.
     * Can be provided explicitly or will be extracted from HTTP headers.
     */
    public static class ClientInfo {
        private String sourceIP;
        private String countryCode;     // Explicit country code (e.g., from CDN headers)
        private String region;          // Explicit region (e.g., from CDN headers)
        private String city;            // Explicit city (e.g., from CDN headers)
        private String timezone;        // Explicit timezone (e.g., from CDN headers)
        private Map<String, String> headers;  // Additional headers for geo detection
        
        public ClientInfo() {}
        
        public ClientInfo(String sourceIP) {
            this.sourceIP = sourceIP;
        }
        
        public ClientInfo(String sourceIP, String countryCode, String region) {
            this.sourceIP = sourceIP;
            this.countryCode = countryCode;
            this.region = region;
        }
        
        // Getters and setters
        public String getSourceIP() {
            return sourceIP;
        }
        
        public void setSourceIP(String sourceIP) {
            this.sourceIP = sourceIP;
        }
        
        public String getCountryCode() {
            return countryCode;
        }
        
        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
        }
        
        public String getRegion() {
            return region;
        }
        
        public void setRegion(String region) {
            this.region = region;
        }
        
        public String getCity() {
            return city;
        }
        
        public void setCity(String city) {
            this.city = city;
        }
        
        public String getTimezone() {
            return timezone;
        }
        
        public void setTimezone(String timezone) {
            this.timezone = timezone;
        }
        
        public Map<String, String> getHeaders() {
            return headers;
        }
        
        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }
    }
}