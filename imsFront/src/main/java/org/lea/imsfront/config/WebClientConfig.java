package org.lea.imsfront.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    private static final String GATEWAY_BASE_URL = "http://localhost:9092";
    private static final String FAKE_JWT_TOKEN = "test-token-value";

    @Bean
    public WebClient webClient() {
        // Configuramos el WebClient para que todas las llamadas se dirijan
        // automáticamente al Gateway.
        return WebClient.builder()
                .baseUrl(GATEWAY_BASE_URL)
                // ** AÑADIMOS EL DEFAULT HEADER CON EL TOKEN REQUERIDO **
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + FAKE_JWT_TOKEN
                )
                .build();
    }
}

