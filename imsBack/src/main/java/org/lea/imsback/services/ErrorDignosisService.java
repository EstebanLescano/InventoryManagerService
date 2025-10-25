package org.lea.imsback.services;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ErrorDignosisService {

    private final LogAnalysisService logAnalysisService;

    public ErrorDignosisService(LogAnalysisService logAnalysisService) {
        this.logAnalysisService = logAnalysisService;
    }

    public Mono<ResponseEntity<String>> handleError(Object request, Throwable error) {
        String logEntry = formatErrorLog(request, error);

        return logAnalysisService.analyzeErrorLog(logEntry)
                .map(aiAnalysis -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("ERROR CRÍTICO INTERNO. Se ha activado el diagnóstico de IA.\n\n" + aiAnalysis))
                .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error crítico. Fallo al obtener el diagnóstico de IA. Consulte logs de servidor."));
    }

    private String formatErrorLog(Object request, Throwable error) {
        return String.format("""
                🔥 Error crítico detectado:
                Request: %s
                Exception: %s
                Message: %s
                """, request, error.getClass().getSimpleName(), error.getMessage());
    }
}
