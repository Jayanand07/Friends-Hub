package com.example.socialmedia.util;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;

/**
 * Resolves the real client IP from X-Forwarded-For safely.
 *
 * SECURITY (H-1): Proxies APPEND the real client IP to X-Forwarded-For, so the
 * FIRST entry is attacker-controlled. Reading the first entry allowed clients
 * to spoof their IP and bypass IP-based rate limiting entirely. We walk the
 * list right-to-left and return the last entry that is NOT a trusted proxy.
 */
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    /** Trusted internal proxy / loopback address prefixes. */
    private static final Set<String> TRUSTED_PROXY_PREFIXES = Set.of(
            "10.", "172.16.", "172.17.", "172.18.", "172.19.", "172.20.",
            "172.21.", "172.22.", "172.23.", "172.24.", "172.25.", "172.26.",
            "172.27.", "172.28.", "172.29.", "172.30.", "172.31.", "192.168.",
            "fc00:", "fd", "fe80:"
    );

    public static boolean isTrustedProxy(String ip) {
        if (ip == null || ip.isBlank()) {
            return true;
        }
        String normalized = ip.toLowerCase();
        // Strip IPv6-mapped IPv4 prefix (::ffff:10.0.0.1 -> 10.0.0.1)
        if (normalized.startsWith("::ffff:")) {
            normalized = normalized.substring(7);
        }
        if (normalized.startsWith("127.") || normalized.equals("::1")
                || normalized.equals("0:0:0:0:0:0:0:1")) {
            return true;
        }
        for (String prefix : TRUSTED_PROXY_PREFIXES) {
            if (normalized.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the best estimate of the real client IP.
     * Only consults X-Forwarded-For when the request arrives from a trusted proxy.
     */
    public static String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff == null || xff.isBlank()) {
            return remoteAddr;
        }
        String[] entries = xff.split(",");
        // Walk right-to-left: the right-most non-trusted entry is the real client.
        // Everything left of it can be spoofed by the client itself.
        for (int i = entries.length - 1; i >= 0; i--) {
            String candidate = entries[i].trim();
            if (candidate.isEmpty()) {
                continue;
            }
            // Strip an optional port (e.g. "1.2.3.4:5678")
            if (candidate.contains(":") && candidate.split(":").length == 2) {
                candidate = candidate.substring(0, candidate.lastIndexOf(':')).trim();
            }
            if (!isTrustedProxy(candidate)) {
                return candidate;
            }
        }
        return remoteAddr;
    }
}
