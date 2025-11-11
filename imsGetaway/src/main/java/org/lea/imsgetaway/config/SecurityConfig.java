package org.lea.imsgetaway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // PERMITIR acceso a TODAS las rutas que empiezan con /ui/ (para el frontend)
                        .pathMatchers("/front/**").permitAll()
                        // Permitir Swagger/OpenAPI/Actuator sin autenticación
                        .pathMatchers("/v3/api-docs/**", "/swagger-ui/**", "/webjars/**").permitAll()
                        // C. Permitir H2 Console (útil para debug si está en el puerto 9090)
                        .pathMatchers("/h2-console/**").permitAll()
                        // D. Permitir Actuator (si lo estás usando)
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/api/**").authenticated()
                        .anyExchange().permitAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }
}