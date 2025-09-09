package dev.bnacar.distributedratelimiter.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.bnacar.distributedratelimiter.models.RateLimitRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import dev.bnacar.distributedratelimiter.TestcontainersConfiguration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
    "ratelimiter.security.api-keys.enabled=false",
    "ratelimiter.security.headers.enabled=true",
    "ratelimiter.security.ip.whitelist=",
    "ratelimiter.security.ip.blacklist=",
    "ratelimiter.security.max-request-size=1KB"
})
public class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testSecurityHeadersAreAdded() throws Exception {
        RateLimitRequest request = new RateLimitRequest("test-user", 1);
        
        mockMvc.perform(post("/api/ratelimit/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-XSS-Protection", "1; mode=block"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Cache-Control", "no-cache, no-store, must-revalidate"))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(header().string("Expires", "0"));
    }

    @Test
    public void testRateLimitingWithIpBasedKey() throws Exception {
        RateLimitRequest request = new RateLimitRequest("ip-test-user", 1);
        
        // First request should be allowed
        mockMvc.perform(post("/api/ratelimit/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("X-Forwarded-For", "192.168.1.100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true));
    }

    @Test
    public void testRequestSizeLimitFilter() throws Exception {
        // Create a request that should be rejected due to size (1KB limit in test config)
        StringBuilder largeKey = new StringBuilder();
        for (int i = 0; i < 2000; i++) { // Much larger than 1KB
            largeKey.append("a");
        }
        
        RateLimitRequest request = new RateLimitRequest(largeKey.toString(), 1);
        
        mockMvc.perform(post("/api/ratelimit/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isRequestEntityTooLarge())
                .andExpect(content().contentType("application/json"));
    }
}