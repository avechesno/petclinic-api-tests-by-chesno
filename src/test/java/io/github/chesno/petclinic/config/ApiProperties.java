package io.github.chesno.petclinic.config;

import java.net.URI;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("petclinic")
public record ApiProperties(String baseUrl, int connectTimeoutMs, int readTimeoutMs) {
    public ApiProperties {
        URI uri = URI.create(baseUrl);
        if (!Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("baseUrl должен быть абсолютным HTTP(S)-адресом без учётных данных, параметров запроса и фрагмента");
        }
        if (connectTimeoutMs <= 0 || readTimeoutMs <= 0) {
            throw new IllegalArgumentException("Таймауты HTTP должны быть больше нуля");
        }
        baseUrl = baseUrl.replaceAll("/+$", "");
    }
}
