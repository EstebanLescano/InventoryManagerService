package org.lea.imsback.services;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Servicio reactivo para analizar logs utilizando un modelo de lenguaje de IA.
 * * NOTA: Esta clase asume que tienes configurada la clave API en
 * application.properties (ej: spring.ai.openai.api-key=...)
 */
@Service
public class LogAnalysisService {
    // ChatClient es el bean de Spring AI recomendado para la interacción con el modelo.
    private final ChatClient chatClient;
    /**
     * Constructor con inyección del ChatClient.
     * Spring Boot auto-configura un ChatClient si se incluye un starter de modelo.
     * @param chatClientBuilder Un constructor para crear una instancia de ChatClient.
     */
    public LogAnalysisService(ChatClient.Builder chatClientBuilder) {
        // Configuramos un ChatClient por defecto con el rol base del analista.
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                Actúa como un analista de soporte Nivel 2 experimentado para un sistema de inventario distribuido.
                Tu tarea es analizar logs y responder SIEMPRE en español con el siguiente formato estricto:

                1. Causa Raíz Técnica: [Resumen conciso del problema, 2-5 palabras]
                2. Resumen de Impacto: [Explicación del efecto en el sistema/usuarios]
                3. Acción Sugerida: [Pasos claros para el ingeniero de turno]
                
                """)
                .build();
    }

    /**
     * Analiza el log del error y genera un resumen diagnóstico de forma reactiva.
     * * @param logEntry La traza del error o log de advertencia.
     * @return Mono<String> con el resumen generado por la IA.
     */
    public Mono<String> analyzeErrorLog(String logEntry) {
        // El prompt solo necesita pasar el log de entrada,
        // ya que el "System Prompt" ya definió el rol y el formato de salida.
        String userPrompt = "LOG DE ERROR A ANALIZAR:\n\n" + logEntry;

        // La respuesta reactiva se obtiene al no usar el método .stream()
        // sino al ejecutar la solicitud a través del Mono.fromCallable.
        // Spring AI maneja internamente la llamada síncrona/asíncrona del cliente HTTP.

        return Mono.fromCallable(() ->
                chatClient.prompt()
                        .user(userPrompt)
                        .call()
                        .content()
        );
    }
}