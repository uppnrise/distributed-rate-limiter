package dev.bnacar.distributedratelimiter.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class IpAddressExtractor {

    private static final String[] IP_HEADER_NAMES = {
        "X-Forwarded-For",
        "X-Real-IP",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_X_FORWARDED_FOR",
        "HTTP_X_FORWARDED",
        "HTTP_X_CLUSTER_CLIENT_IP",
        "HTTP_CLIENT_IP",
        "HTTP_FORWARDED_FOR",
        "HTTP_FORWARDED",
        "HTTP_VIA",
        "REMOTE_ADDR"
    };

    /**
     * Extracts the client IP address from the HTTP request, considering common proxy headers
     * @param request the HTTP servlet request
     * @return the client IP address, or the remote address if no proxy headers are found
     */
    public String getClientIpAddress(HttpServletRequest request) {
        for (String header : IP_HEADER_NAMES) {
            String ip = request.getHeader(header);
            if (StringUtils.hasText(ip) && !isUnknownIp(ip)) {
                // Handle comma-separated IPs (take the first one)
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        
        // Fall back to the remote address
        return request.getRemoteAddr();
    }

    private boolean isUnknownIp(String ip) {
        return "unknown".equalsIgnoreCase(ip) || ip.trim().isEmpty();
    }
}