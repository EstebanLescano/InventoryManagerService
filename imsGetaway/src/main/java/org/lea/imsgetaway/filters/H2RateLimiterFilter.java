package org.lea.imsgetaway.filters;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class H2RateLimiterFilter implements WebFilter {
    private final Map<String, List<LocalDateTime>> requestLog = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS = 3;
    private static final Duration WINDOW = Duration.ofSeconds(10);
    // Rutas protegidas por rate limit
    private static final List<String> PROTECTED_PATHS = List.of(
            "/api/inventory/reserve"

    );
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Si la ruta no está protegida, continuar sin rate limit
        boolean protectedRoute = PROTECTED_PATHS.stream().anyMatch(path::startsWith);
        if (!protectedRoute) {
            return chain.filter(exchange);
        }

        String clientIp = Objects.requireNonNull(exchange.getRequest().getRemoteAddress())
                .getAddress().getHostAddress();

        LocalDateTime now = LocalDateTime.now();
        requestLog.putIfAbsent(clientIp, new ArrayList<>());
        List<LocalDateTime> timestamps = requestLog.get(clientIp);

        timestamps.removeIf(t -> t.isBefore(now.minus(WINDOW)));

        if (timestamps.size() >= MAX_REQUESTS) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }

        timestamps.add(now);
        return chain.filter(exchange);
    }
}
