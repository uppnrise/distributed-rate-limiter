package dev.bnacar.distributedratelimiter.security;

import dev.bnacar.distributedratelimiter.config.SecurityConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class IpSecurityService {

    // Strict IPv4/IPv6 literal matchers. normalize() only calls InetAddress on
    // values that already match one of these, guaranteeing it never performs a
    // DNS lookup - ipAddress may originate from a client-controlled header
    // (see IpAddressExtractor), so resolving arbitrary/non-literal input would
    // be an SSRF/DoS-adjacent risk.
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3}$");

    private static final Pattern IPV6_PATTERN = Pattern.compile(
            "^("
                    + "([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}|"
                    + "([0-9a-fA-F]{1,4}:){1,7}:|"
                    + "([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|"
                    + "([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|"
                    + "([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|"
                    + "([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|"
                    + "([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|"
                    + "[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|"
                    + ":((:[0-9a-fA-F]{1,4}){1,7}|:)|"
                    + "::(ffff(:0{1,4})?:)?((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])|"
                    + "([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])"
                    + ")$");

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
        if (containsIp(securityConfiguration.getIp().getBlacklist(), ipAddress)) {
            return false;
        }

        // If whitelist is configured and not empty, only allow whitelisted IPs
        if (!securityConfiguration.getIp().getWhitelist().isEmpty()) {
            return containsIp(securityConfiguration.getIp().getWhitelist(), ipAddress);
        }

        // If no whitelist configured, allow all IPs not in blacklist
        return true;
    }

    /**
     * Checks whether {@code ipAddress} matches any entry in {@code configuredIps}.
     * <p>
     * IPv6 loopback in particular has multiple textual representations that are
     * all the same address (e.g. "::1" vs the servlet container's canonical
     * "0:0:0:0:0:0:0:1"), so a literal string comparison alone would silently
     * reject addresses that operators clearly intended to allow/deny. Both sides
     * are normalized via {@link InetAddress} (a local, network-free parse for IP
     * literals) before falling back to a raw string comparison for values that
     * are not parseable IP addresses.
     */
    private boolean containsIp(List<String> configuredIps, String ipAddress) {
        if (configuredIps.contains(ipAddress)) {
            return true;
        }

        String normalizedAddress = normalize(ipAddress);
        if (normalizedAddress == null) {
            return false;
        }

        for (String configuredIp : configuredIps) {
            if (normalizedAddress.equals(normalize(configuredIp))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String ipAddress) {
        if (!isIpLiteral(ipAddress)) {
            // Not a strictly valid IPv4/IPv6 literal - never resolve it, to
            // avoid triggering a DNS lookup on client-controlled input.
            return null;
        }
        try {
            return InetAddress.getByName(ipAddress).getHostAddress();
        } catch (UnknownHostException e) {
            // Matched the literal pattern but failed to parse (shouldn't
            // normally happen); the raw string comparison in containsIp()
            // already covers this value.
            return null;
        }
    }

    private boolean isIpLiteral(String value) {
        return IPV4_PATTERN.matcher(value).matches() || IPV6_PATTERN.matcher(value).matches();
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