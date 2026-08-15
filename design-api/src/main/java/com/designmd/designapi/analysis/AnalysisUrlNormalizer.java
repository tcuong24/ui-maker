package com.designmd.designapi.analysis;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Component
public class AnalysisUrlNormalizer {

    public NormalizedAnalysis normalize(
            String websiteUrl,
            List<String> additionalPaths,
            boolean includeScreenshot
    ) {
        URI uri = parse(websiteUrl.trim());
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        int port = normalizePort(scheme, uri.getPort());
        String path = uri.getRawPath();

        if (path == null || path.isBlank()) {
            path = "/";
        }

        String normalizedUrl;
        try {
            normalizedUrl = new URI(
                    scheme,
                    null,
                    host,
                    port,
                    path,
                    uri.getRawQuery(),
                    null
            ).toASCIIString();
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Invalid website URL", exception);
        }

        List<String> normalizedPaths = additionalPaths == null
                ? List.of()
                : additionalPaths.stream()
                .map(String::trim)
                .distinct()
                .sorted()
                .toList();

        String fingerprint = String.join("\n",
                normalizedUrl,
                String.join("\u001F", normalizedPaths),
                Boolean.toString(includeScreenshot)
        );

        return new NormalizedAnalysis(
                normalizedUrl,
                host,
                sha256(fingerprint)
        );
    }

    private URI parse(String value) {
        try {
            URI uri = new URI(value);
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new IllegalArgumentException("Website URL must include a public host");
            }
            return uri;
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Invalid website URL", exception);
        }
    }

    private int normalizePort(String scheme, int port) {
        if (("http".equals(scheme) && port == 80)
                || ("https".equals(scheme) && port == 443)) {
            return -1;
        }
        return port;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public record NormalizedAnalysis(
            String normalizedUrl,
            String domain,
            String cacheKey
    ) {
    }
}
