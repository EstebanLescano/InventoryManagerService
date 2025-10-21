package org.lea.imsgetaway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
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
                        .pathMatchers("/ui/**").permitAll()
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
    //----------------------------------------------------------------------
    //  USANDO EL DECODIFICADOR SIMULADO PARA DESARROLLO
    //----------------------------------------------------------------------

    /**
     * Bean de Decodificador JWT de Prueba.
     * Usa @ConditionalOnMissingBean para que este bean SOLO se cree
     * si Spring no ha podido crear un ReactiveJwtDecoder automáticamente (ej.
     * porque faltan las propiedades de 'issuer-uri' en application.yml).
     * Esto te permite desarrollar sin Keycloak.
     */
    @Bean
    @ConditionalOnMissingBean(ReactiveJwtDecoder.class)
    public ReactiveJwtDecoder testJwtDecoder() {
        System.out.println("===============================================================");
        System.out.println("!! ATENCION: Activando JwtDecoder: Autenticación JWT Simulada");
        System.out.println("!! Usa 'Authorization: Bearer <CUALQUIER_TOKEN>' para acceder a /api/**");
        System.out.println("===============================================================");
        // Devuelve la instancia de tu clase de prueba
        return new JwtDecoder();
    }
}