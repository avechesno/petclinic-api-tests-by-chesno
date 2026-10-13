package io.github.chesno.petclinic.config;

import java.net.URI;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("petclinic")
public record ApiProperties(String baseUrl, int connectTimeoutMs, int readTimeoutMs, boolean allowRemote) {
    public ApiProperties {
        URI uri = URI.create(baseUrl);
        if (!Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("baseUrl must be an absolute HTTP(S) URL without credentials/query/fragment");
        }
        if (!allowRemote && !Set.of("localhost", "127.0.0.1", "[::1]", "::1").contains(uri.getHost())) {
            throw new IllegalArgumentException("Tests create/delete data. Remote targets require explicit -DallowRemote=true");
        }
        if (connectTimeoutMs <= 0 || readTimeoutMs <= 0) {
            throw new IllegalArgumentException("HTTP timeouts must be positive");
        }
        baseUrl = baseUrl.replaceAll("/+$", "");
    }
}
