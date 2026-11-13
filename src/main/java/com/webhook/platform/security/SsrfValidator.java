package com.webhook.platform.security;

import com.webhook.platform.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

@Component
public class SsrfValidator {

    @Value("${webhook.security.allow-loopback:true}")
    private boolean allowLoopback;

    public void validateUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Webhook endpoint URL must not be blank");
        }

        URI uri;
        try {
            uri = URI.create(rawUrl);
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid URL format: " + rawUrl);
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Webhook URL must use HTTP or HTTPS scheme");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Webhook URL host cannot be empty");
        }

        // SSRF protection: Resolve host and verify it's not a restricted target
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress addr : addresses) {
                if (addr.isAnyLocalAddress() || addr.isMulticastAddress()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "SSRF Guard: Target host resolves to a restricted multicast or any-local address: " + host);
                }

                if (addr.isLoopbackAddress()) {
                    if (!allowLoopback) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "SSRF Guard: Loopback destinations (localhost) are disallowed in production: " + host);
                    }
                    continue;
                }

                if (addr.isLinkLocalAddress() || isCloudMetadata(addr)) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "SSRF Guard: Target host resolves to a link-local or cloud metadata address: " + host);
                }

                if (addr.isSiteLocalAddress() && !allowLoopback) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "SSRF Guard: Target host resolves to a private network address (RFC 1918): " + host);
                }
            }
        } catch (UnknownHostException e) {
            // In dev or docker internal host names (e.g. host.docker.internal), DNS might not resolve during registration,
            // which can be allowed or flagged. We validate that syntax is sound.
        }
    }

    private boolean isCloudMetadata(InetAddress addr) {
        String ip = addr.getHostAddress();
        // AWS, GCP, Azure link-local metadata address 169.254.169.254
        return ip.startsWith("169.254.");
    }
}

