package dev.bnacar.distributedratelimiter.security;

import dev.bnacar.distributedratelimiter.config.SecurityConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class IpSecurityService {

    // Strict IPv4/IPv6 literal matchers. normalize() only parses values that
    // already match one of these; ipAddress may originate from a
    // client-controlled header (see IpAddressExtractor), so anything that
    // does not strictly conform is left untouched rather than guessed at.
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
     * are normalized via {@link #normalize(String)} (a pure, network-free
     * canonicalization of IP literals) before falling back to a raw string
     * comparison for values that are not parseable IP addresses.
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

    /**
     * Canonicalizes an IP literal so that equivalent textual representations
     * (e.g. "::1" and "0:0:0:0:0:0:0:1") compare equal. Returns {@code null}
     * for values that are not valid IPv4/IPv6 literals; such values are never
     * resolved (e.g. via DNS), only compared as-is by the caller.
     * <p>
     * This mirrors {@code java.net.Inet6Address#getHostAddress()}'s output
     * format (expanded lowercase hex groups, except IPv4-mapped addresses
     * such as "::ffff:127.0.0.1" which render as dotted-decimal) without
     * calling into {@code InetAddress}, so there is no checked
     * {@code UnknownHostException} path to reason about or leave untested.
     */
    private String normalize(String ipAddress) {
        if (IPV4_PATTERN.matcher(ipAddress).matches()) {
            return normalizeIpv4(ipAddress);
        }
        if (IPV6_PATTERN.matcher(ipAddress).matches()) {
            return normalizeIpv6(ipAddress);
        }
        return null;
    }

    private String normalizeIpv4(String ipAddress) {
        String[] octets = ipAddress.split("\\.");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < octets.length; i++) {
            if (i > 0) {
                result.append('.');
            }
            result.append(Integer.parseInt(octets[i]));
        }
        return result.toString();
    }

    private String normalizeIpv6(String ipAddress) {
        int compressionIndex = ipAddress.indexOf("::");
        List<String> head;
        List<String> tail;
        if (compressionIndex >= 0) {
            head = splitGroups(ipAddress.substring(0, compressionIndex));
            tail = splitGroups(ipAddress.substring(compressionIndex + 2));
        } else {
            head = splitGroups(ipAddress);
            tail = new ArrayList<>();
        }

        head = expandEmbeddedIpv4(head);
        tail = expandEmbeddedIpv4(tail);

        int[] groups = new int[8];
        int zerosToFill = 8 - head.size() - tail.size();
        int index = 0;
        for (String group : head) {
            groups[index++] = Integer.parseInt(group, 16);
        }
        index += zerosToFill;
        for (String group : tail) {
            groups[index++] = Integer.parseInt(group, 16);
        }

        // Mirrors Inet6Address#getHostAddress(): an IPv4-mapped address
        // (::ffff:0:0/96) renders as dotted-decimal, everything else as
        // expanded lowercase hex groups.
        if (groups[0] == 0 && groups[1] == 0 && groups[2] == 0 && groups[3] == 0
                && groups[4] == 0 && groups[5] == 0xffff) {
            int a = (groups[6] >>> 8) & 0xFF;
            int b = groups[6] & 0xFF;
            int c = (groups[7] >>> 8) & 0xFF;
            int d = groups[7] & 0xFF;
            return a + "." + b + "." + c + "." + d;
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < groups.length; i++) {
            if (i > 0) {
                result.append(':');
            }
            result.append(Integer.toHexString(groups[i]));
        }
        return result.toString();
    }

    private List<String> splitGroups(String value) {
        List<String> groups = new ArrayList<>();
        if (!value.isEmpty()) {
            for (String group : value.split(":")) {
                groups.add(group);
            }
        }
        return groups;
    }

    /**
     * Expands an embedded IPv4 dotted-decimal suffix (e.g. the "1.2.3.4" in
     * "::ffff:1.2.3.4") into its two equivalent 16-bit hex groups so the rest
     * of the pipeline only ever deals with plain hextets.
     */
    private List<String> expandEmbeddedIpv4(List<String> groups) {
        if (groups.isEmpty()) {
            return groups;
        }
        String last = groups.get(groups.size() - 1);
        if (!last.contains(".")) {
            return groups;
        }
        String[] octets = last.split("\\.");
        int high = (Integer.parseInt(octets[0]) << 8) | Integer.parseInt(octets[1]);
        int low = (Integer.parseInt(octets[2]) << 8) | Integer.parseInt(octets[3]);
        List<String> expanded = new ArrayList<>(groups.subList(0, groups.size() - 1));
        expanded.add(Integer.toHexString(high));
        expanded.add(Integer.toHexString(low));
        return expanded;
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