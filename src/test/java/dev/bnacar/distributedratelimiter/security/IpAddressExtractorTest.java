package dev.bnacar.distributedratelimiter.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class IpAddressExtractorTest {

    @Mock
    private HttpServletRequest request;

    private IpAddressExtractor ipAddressExtractor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        ipAddressExtractor = new IpAddressExtractor();
    }

    @Test
    public void testGetClientIpFromXForwardedFor() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        String ip = ipAddressExtractor.getClientIpAddress(request);
        assertEquals("192.168.1.100", ip);
    }

    @Test
    public void testGetClientIpFromXRealIp() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("192.168.1.200");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        String ip = ipAddressExtractor.getClientIpAddress(request);
        assertEquals("192.168.1.200", ip);
    }

    @Test
    public void testGetClientIpFromRemoteAddr() {
        // No proxy headers available
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        String ip = ipAddressExtractor.getClientIpAddress(request);
        assertEquals("127.0.0.1", ip);
    }

    @Test
    public void testGetClientIpFromCommaSeparatedXForwardedFor() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100, 10.0.0.50, 172.16.0.1");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        String ip = ipAddressExtractor.getClientIpAddress(request);
        assertEquals("192.168.1.100", ip); // Should take the first IP
    }

    @Test
    public void testGetClientIpIgnoresUnknownHeader() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
        when(request.getHeader("X-Real-IP")).thenReturn("192.168.1.300");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        String ip = ipAddressExtractor.getClientIpAddress(request);
        assertEquals("192.168.1.300", ip); // Should skip "unknown" and use X-Real-IP
    }

    @Test
    public void testGetClientIpIgnoresEmptyHeader() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("");
        when(request.getHeader("X-Real-IP")).thenReturn("   ");
        when(request.getHeader("Proxy-Client-IP")).thenReturn("192.168.1.400");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        String ip = ipAddressExtractor.getClientIpAddress(request);
        assertEquals("192.168.1.400", ip); // Should skip empty headers
    }

    @Test
    public void testGetClientIpWithWhitespaceInCommaSeparated() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(" 192.168.1.100 , 10.0.0.50 ");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        String ip = ipAddressExtractor.getClientIpAddress(request);
        assertEquals("192.168.1.100", ip); // Should trim whitespace
    }
}