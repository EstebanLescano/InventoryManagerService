package org.lea.imsback.controllers;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.lea.imsback.services.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
record ReservationRequest(
        @NotBlank String storeId,
        @NotBlank String sku,
        @Min(1) int quantity
) {}

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;
    /**
     * Endpoint reactivo para reservar stock.
     * Retorna Mono<ResponseEntity> para evitar el bloqueo del hilo (WebFlux).
     */
    @PostMapping("/reserve")
    public Mono<ResponseEntity<String>> reserveStock(@RequestBody ReservationRequest request) {

        return inventoryService.tryReserveStock(
                        request.storeId(),
                        request.sku(),
                        request.quantity()
                )
                .map(success -> {
                    if (Boolean.TRUE.equals(success)) {
                        // 200 OK: Reserva exitosa y evento publicado
                        return ResponseEntity.ok("Stock reservado. Evento de actualización publicado.");
                    } else {
                        // 409 Conflict: Falla la reserva por falta de stock
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body("Reserva fallida. Stock insuficiente o ítem no encontrado.");
                    }
                });
    }
}
