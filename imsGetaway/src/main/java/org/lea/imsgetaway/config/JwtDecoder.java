package org.lea.imsgetaway.config;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;

/**
 * Decodificador JWT reactivo customizado para el entorno de desarrollo.
 * * Este decodificador ignora la validación del token real (Keycloak/Issuer-URI)
 * y devuelve un objeto Jwt válido con claims predefinidos, permitiendo que
 * las rutas protegidas sean accesibles con cualquier token Bearer.
 */
public class JwtDecoder implements ReactiveJwtDecoder {
    private static final String FAKE_TOKEN_VALUE = "test-token-value";
    private static final String FAKE_USERNAME = "fakeUser@gateway.com";

    @Override
    public Mono<Jwt> decode(String token) {
        // Determina el token a usar. Si el token que llega es nulo/vacío,
        // usamos el valor de prueba para evitar que Spring Security lo rechace por formato.
        final String tokenToUse = (token == null || token.trim().isEmpty())
                ? FAKE_TOKEN_VALUE
                : token;

        // Si realmente quieres forzar el 401 si no hay token (comportamiento habitual),
        // usa el siguiente bloque (comentado si quieres que el fallback funcione):
        /*
        if (token == null || token.trim().isEmpty()) {
            return Mono.error(new BadJwtException("Token nulo o vacío."));
        }
        */

        // 1. Definir los headers y claims (cargas útiles) del JWT
        Map<String, Object> headers = Collections.singletonMap("alg", "none");
        Map<String, Object> claims = new HashMap<>();

        // 2. Claims esenciales para Resource Server
        claims.put("sub", FAKE_USERNAME); // Subject/Usuario
        claims.put("user_name", FAKE_USERNAME);
        claims.put("exp", Instant.now().plusSeconds(3600)); // Expiración en 1 hora
        claims.put("iat", Instant.now()); // Emitido en

        // 3. Crear el objeto Jwt (el JWT real que Spring Security procesará)
        Jwt jwt = new Jwt(
                tokenToUse,               // El token original (cualquier valor)
                Instant.now(),       // emitido en
                Instant.now().plusSeconds(36000), // expira en
                headers,
                claims
        );

        return Mono.just(jwt);
    }
}
