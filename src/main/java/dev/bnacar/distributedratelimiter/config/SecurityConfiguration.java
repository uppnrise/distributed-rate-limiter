package dev.bnacar.distributedratelimiter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.ArrayList;

@Configuration
@ConfigurationProperties(prefix = "ratelimiter.security")
public class SecurityConfiguration {

    private ApiKeys apiKeys = new ApiKeys();
    private Ip ip = new Ip();
    private String maxRequestSize = "1MB";
    private Headers headers = new Headers();

    public static class ApiKeys {
        private boolean enabled = true;
        private List<String> validKeys = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getValidKeys() {
            return validKeys != null ? new ArrayList<>(validKeys) : new ArrayList<>();
        }

        public void setValidKeys(List<String> validKeys) {
            this.validKeys = validKeys != null ? new ArrayList<>(validKeys) : new ArrayList<>();
        }
    }

    public static class Ip {
        private List<String> whitelist = new ArrayList<>();
        private List<String> blacklist = new ArrayList<>();

        public List<String> getWhitelist() {
            return whitelist != null ? new ArrayList<>(whitelist) : new ArrayList<>();
        }

        public void setWhitelist(List<String> whitelist) {
            this.whitelist = whitelist != null ? new ArrayList<>(whitelist) : new ArrayList<>();
        }

        public List<String> getBlacklist() {
            return blacklist != null ? new ArrayList<>(blacklist) : new ArrayList<>();
        }

        public void setBlacklist(List<String> blacklist) {
            this.blacklist = blacklist != null ? new ArrayList<>(blacklist) : new ArrayList<>();
        }
    }

    public static class Headers {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public ApiKeys getApiKeys() {
        return apiKeys;  // Note: ApiKeys class already implements defensive copying in its own getters/setters
    }

    public void setApiKeys(ApiKeys apiKeys) {
        this.apiKeys = apiKeys;  // Note: ApiKeys class handles defensive copying internally
    }

    public Ip getIp() {
        return ip;  // Note: Ip class already implements defensive copying in its own getters/setters
    }

    public void setIp(Ip ip) {
        this.ip = ip;  // Note: Ip class handles defensive copying internally
    }

    public String getMaxRequestSize() {
        return maxRequestSize;
    }

    public void setMaxRequestSize(String maxRequestSize) {
        this.maxRequestSize = maxRequestSize;
    }

    public Headers getHeaders() {
        return headers;  // Note: Headers class only contains primitives, no mutable state to protect
    }

    public void setHeaders(Headers headers) {
        this.headers = headers;  // Note: Headers class only contains primitives, no mutable state to protect
    }
}