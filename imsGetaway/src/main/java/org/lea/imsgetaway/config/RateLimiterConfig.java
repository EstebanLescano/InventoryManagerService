package org.lea.imsgetaway.config;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.RequestRateLimiterGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Configuration
public class RateLimiterConfig {
    /**
     * Define el KeyResolver que utiliza la dirección IP remota del cliente
     * como identificador para la limitación de tasa.
     * * El nombre del bean es "ipAddressKeyResolver" y debe coincidir con
     * lo que se usa en application.properties: @ipAddressKeyResolver
     */
    @Bean
    public KeyResolver ipAddressKeyResolver() {
        return exchange -> Mono.just(Objects.
                requireNonNull(exchange.getRequest().getRemoteAddress()).getAddress().getHostAddress());
    }

    @Bean
    public RedisRateLimiter myRateLimiter() {
        // Spring Cloud Gateway lo auto-configurará a R2DBC si Redis no está disponible
        // y R2DBC/H2 sí lo está, manteniendo esta configuración de tasas.
        return new RedisRateLimiter(5, 10, 1);
    }

    @Bean("RequestRateLimiter") // Usar nombre explícito es buena práctica
    public GatewayFilter requestRateLimiter(
            RequestRateLimiterGatewayFilterFactory factory,
            KeyResolver ipAddressKeyResolver,
            RedisRateLimiter myRateLimiter
    ) {
        return factory.apply(config -> config
                .setKeyResolver(ipAddressKeyResolver)
                .setRateLimiter(myRateLimiter)
        );
    }
}

