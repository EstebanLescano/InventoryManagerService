package org.lea.imsgetaway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

@Configuration
public class JwtDecoderConfig {
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
