package dev.bnacar.distributedratelimiter.security;

import dev.bnacar.distributedratelimiter.config.SecurityConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class IpSecurityService {

    private final SecurityConfiguration securityConfiguration;

    @Autowired
    public IpSecurityService(SecurityConfiguration securityConfiguration) {
        this.securityConfiguration = securityConfiguration;
    }

    /**
     * Checks if an IP address is allowed based on whitelist/blacklist configuration
     * @param ipAddress the IP address to check
     * @return true if the IP is allowed, false otherwise
     */
    public boolean isIpAllowed(String ipAddress) {
        if (!StringUtils.hasText(ipAddress)) {
            return false; // No IP address provided
        }

        // Check blacklist first
        if (securityConfiguration.getIp().getBlacklist().contains(ipAddress)) {
            return false;
        }

        // If whitelist is configured and not empty, only allow whitelisted IPs
        if (!securityConfiguration.getIp().getWhitelist().isEmpty()) {
            return securityConfiguration.getIp().getWhitelist().contains(ipAddress);
        }

        // If no whitelist configured, allow all IPs not in blacklist
        return true;
    }

    /**
     * Creates a rate limiting key that includes the IP address for IP-based rate limiting
     * @param originalKey the original rate limiting key
     * @param ipAddress the client IP address
     * @return a combined key for IP-based rate limiting
     */
    public String createIpBasedKey(String originalKey, String ipAddress) {
        if (!StringUtils.hasText(ipAddress)) {
            return originalKey;
        }
        return "ip:" + ipAddress + ":" + originalKey;
    }
}