package dev.bnacar.distributedratelimiter.security;

import dev.bnacar.distributedratelimiter.config.SecurityConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class ApiKeyServiceTest {

    private SecurityConfiguration securityConfiguration;
    private ApiKeyService apiKeyService;

    @BeforeEach
    public void setUp() {
        securityConfiguration = new SecurityConfiguration();
        apiKeyService = new ApiKeyService(securityConfiguration);
    }

    @Test
    public void testValidApiKeyWhenEnabled() {
        // Configure valid API keys
        securityConfiguration.getApiKeys().setEnabled(true);
        securityConfiguration.getApiKeys().setValidKeys(Arrays.asList("key1", "key2", "premium-key"));

        assertTrue(apiKeyService.isValidApiKey("key1"));
        assertTrue(apiKeyService.isValidApiKey("key2"));
        assertTrue(apiKeyService.isValidApiKey("premium-key"));
    }

    @Test
    public void testInvalidApiKeyWhenEnabled() {
        // Configure valid API keys
        securityConfiguration.getApiKeys().setEnabled(true);
        securityConfiguration.getApiKeys().setValidKeys(Arrays.asList("key1", "key2"));

        assertFalse(apiKeyService.isValidApiKey("invalid-key"));
        assertFalse(apiKeyService.isValidApiKey(""));
        assertFalse(apiKeyService.isValidApiKey(null));
        assertFalse(apiKeyService.isValidApiKey("   "));
    }

    @Test
    public void testApiKeyValidationDisabled() {
        // Disable API key validation
        securityConfiguration.getApiKeys().setEnabled(false);

        // Any key should be valid when validation is disabled
        assertTrue(apiKeyService.isValidApiKey("any-key"));
        assertTrue(apiKeyService.isValidApiKey(""));
        assertTrue(apiKeyService.isValidApiKey(null));
    }

    @Test
    public void testIsApiKeyRequired() {
        securityConfiguration.getApiKeys().setEnabled(true);
        assertTrue(apiKeyService.isApiKeyRequired());

        securityConfiguration.getApiKeys().setEnabled(false);
        assertFalse(apiKeyService.isApiKeyRequired());
    }

    @Test
    public void testEmptyValidKeysListWhenEnabled() {
        securityConfiguration.getApiKeys().setEnabled(true);
        securityConfiguration.getApiKeys().setValidKeys(Arrays.asList());

        assertFalse(apiKeyService.isValidApiKey("any-key"));
    }
}