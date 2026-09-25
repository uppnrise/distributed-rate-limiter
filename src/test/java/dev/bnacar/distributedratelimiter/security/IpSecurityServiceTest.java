package dev.bnacar.distributedratelimiter.security;

import dev.bnacar.distributedratelimiter.config.SecurityConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class IpSecurityServiceTest {

    private SecurityConfiguration securityConfiguration;
    private IpSecurityService ipSecurityService;

    @BeforeEach
    public void setUp() {
        securityConfiguration = new SecurityConfiguration();
        ipSecurityService = new IpSecurityService(securityConfiguration);
    }

    @Test
    public void testIpAllowedWithNoWhitelistOrBlacklist() {
        // No whitelist or blacklist configured - should allow all IPs
        assertTrue(ipSecurityService.isIpAllowed("192.168.1.1"));
        assertTrue(ipSecurityService.isIpAllowed("10.0.0.1"));
        assertTrue(ipSecurityService.isIpAllowed("127.0.0.1"));
    }

    @Test
    public void testIpBlacklisted() {
        securityConfiguration.getIp().setBlacklist(Arrays.asList("192.168.1.100", "10.0.0.50"));

        assertFalse(ipSecurityService.isIpAllowed("192.168.1.100"));
        assertFalse(ipSecurityService.isIpAllowed("10.0.0.50"));
        assertTrue(ipSecurityService.isIpAllowed("192.168.1.1"));
    }

    @Test
    public void testIpWhitelistOnly() {
        securityConfiguration.getIp().setWhitelist(Arrays.asList("127.0.0.1", "192.168.1.10"));

        assertTrue(ipSecurityService.isIpAllowed("127.0.0.1"));
        assertTrue(ipSecurityService.isIpAllowed("192.168.1.10"));
        assertFalse(ipSecurityService.isIpAllowed("192.168.1.1"));
        assertFalse(ipSecurityService.isIpAllowed("10.0.0.1"));
    }

    @Test
    public void testBlacklistOverridesWhitelist() {
        // IP is both whitelisted and blacklisted - blacklist should take precedence
        securityConfiguration.getIp().setWhitelist(Arrays.asList("127.0.0.1", "192.168.1.10"));
        securityConfiguration.getIp().setBlacklist(Arrays.asList("127.0.0.1"));

        assertFalse(ipSecurityService.isIpAllowed("127.0.0.1"));
        assertTrue(ipSecurityService.isIpAllowed("192.168.1.10"));
    }

    @Test
    public void testInvalidIpAddress() {
        assertFalse(ipSecurityService.isIpAllowed(""));
        assertFalse(ipSecurityService.isIpAllowed(null));
        assertFalse(ipSecurityService.isIpAllowed("   "));
    }

    @Test
    public void testCreateIpBasedKey() {
        assertEquals("ip:192.168.1.1:user123", 
                    ipSecurityService.createIpBasedKey("user123", "192.168.1.1"));
        assertEquals("ip:127.0.0.1:api-key-456", 
                    ipSecurityService.createIpBasedKey("api-key-456", "127.0.0.1"));
    }

    @Test
    public void testCreateIpBasedKeyWithNullOrEmptyIp() {
        assertEquals("user123", ipSecurityService.createIpBasedKey("user123", null));
        assertEquals("user123", ipSecurityService.createIpBasedKey("user123", ""));
        assertEquals("user123", ipSecurityService.createIpBasedKey("user123", "   "));
    }

    @Test
    public void testIpv6LoopbackVariantsAreTreatedAsEquivalent() {
        // "::1" and "0:0:0:0:0:0:0:1" are the same address; the servlet
        // container may report either form depending on platform/JDK.
        securityConfiguration.getIp().setWhitelist(Arrays.asList("127.0.0.1", "::1"));

        assertTrue(ipSecurityService.isIpAllowed("::1"));
        assertTrue(ipSecurityService.isIpAllowed("0:0:0:0:0:0:0:1"));
        assertFalse(ipSecurityService.isIpAllowed("::2"));
    }

    @Test
    public void testIpv4MappedIpv6AddressMatchesIpv4Whitelist() {
        securityConfiguration.getIp().setWhitelist(Arrays.asList("127.0.0.1"));

        assertTrue(ipSecurityService.isIpAllowed("::ffff:127.0.0.1"));
    }

    @Test
    public void testNonIpValuesFallBackToRawStringComparisonWithoutDnsLookup() {
        // Values that are not valid IP literals must never trigger a DNS
        // lookup; they can only match via exact string equality.
        securityConfiguration.getIp().setWhitelist(Arrays.asList("not-an-ip"));

        assertTrue(ipSecurityService.isIpAllowed("not-an-ip"));
        assertFalse(ipSecurityService.isIpAllowed("still-not-an-ip"));
    }
}