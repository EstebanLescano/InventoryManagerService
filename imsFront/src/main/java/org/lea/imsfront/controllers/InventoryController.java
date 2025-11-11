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

    @GetMapping({"/", "/reserve"})
    public String showForm() {
        return "reserve"; // http://localhost:9092/inventory/reserve (GET) -> Carga el formulario
    }

    @PostMapping("/reserve")
    public Mono<String> reserveStock(
            // Recibe los campos individualmente y por su nombre (el nombre del 'name' en el input)
            @RequestParam("storeId") String storeId,
            @RequestParam("sku") String sku,
            // Usamos Integer para manejar nulos de forma más segura antes de la validación
            @RequestParam("quantity") String quantityStr,
            Model model) {

        int quantity = 0;

        // 1. Manejo y Conversión Segura de Quantity
        try {
            if (quantityStr != null && !quantityStr.trim().isEmpty()) {
                quantity = Integer.parseInt(quantityStr.trim());
            }
        } catch (NumberFormatException e) {
            log.error("Formato de cantidad inválido: {}", quantityStr);
            String errorMessage = "Error: La cantidad debe ser un número entero válido.";
            model.addAttribute("message", errorMessage);
            model.addAttribute("success", false);
            return Mono.just("reserve");
        }
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
                    return "redirect:/inventory/reserve";
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

