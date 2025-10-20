package org.lea.imsfront.controllers;

import org.lea.imsfront.models.ReservationRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Controller
@RequestMapping("/inventory")
public class InventoryController {
    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);
    private final WebClient webClient;

    public InventoryController(WebClient webClient) {
        this.webClient = webClient;
    }
    @GetMapping("/reserve")
    public String showForm() {
        // http://localhost:8081/inventory/reserve (GET) -> Carga el formulario
        return "reserve";
    }

    @PostMapping("/reserve")
    public Mono<String> reserveStock(
            @RequestParam MultiValueMap<String, String> formData,
            Model model) {
        // Extraemos los valores del formulario
        String storeId = formData.getFirst("storeId");
        String sku = formData.getFirst("sku");
        // Aseguramos la conversión a int, con manejo básico de nulos
        int quantity = Optional.ofNullable(formData.getFirst("quantity"))
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .orElse(0);

        // Verificamos si los campos requeridos están realmente vacíos
        if (storeId == null || storeId.isEmpty() || sku == null || sku.isEmpty() || quantity <= 0) {
            String errorMessage = "Faltan campos requeridos o la cantidad es inválida.";
            log.error(errorMessage);
            model.addAttribute("message", errorMessage);
            model.addAttribute("success", false);
            return Mono.just("reserve");
        }

        log.info("Recibida solicitud de reserva: StoreID={}, SKU={}, Cantidad={}", storeId, sku, quantity);

        ReservationRequest request = new ReservationRequest(storeId, sku, quantity);

        return webClient.post()
                .uri("/api/inventory/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> {
                    // Manejo de errores 4xx o 5xx provenientes del Back-end (8080)
                    return clientResponse.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                log.error("Error del Back-end (Status {}): {}", clientResponse.statusCode(), errorBody);
                                // Si es 409 (Conflicto de stock), el cuerpo es el mensaje que queremos
                                if (clientResponse.statusCode().value() == 409) {
                                    return Mono.error(new RuntimeException(errorBody));
                                }
                                // Para otros errores (400, 500) que el Back-end pueda devolver por excepción interna
                                return Mono.error(new RuntimeException("Error inesperado al contactar el servicio: Status " + clientResponse.statusCode().value()));
                            });
                })
                .bodyToMono(String.class)
                .map(response -> {
                    model.addAttribute("message", "Error en la reserva: " + response);
                    model.addAttribute("success", true);
                    return "reserve";
                })
                .onErrorResume(error -> {
                    // Loguea la excepción (fallo de conexión, timeout, o la excepción lanzada en onStatus)
                    log.error("Fallo de comunicación/excepción no manejada: {}", error.getMessage());
                    // Muestra un mensaje detallado en el front-end
                    String errorMessage = error.getMessage();
                    if (errorMessage.contains("Connection refused") || errorMessage.contains("Host refused")) {
                        errorMessage = "Fallo de conexión: Asegúrate de que el Back-end (8080) esté corriendo.";
                    }
                    model.addAttribute("message", errorMessage);
                    model.addAttribute("success", false);
                    return Mono.just("reserve");
                });
    }
}

