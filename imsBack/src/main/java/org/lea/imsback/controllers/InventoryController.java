package org.lea.imsback.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.lea.imsback.models.dtos.ReservationRequest;
import org.lea.imsback.models.Item;
import org.lea.imsback.services.InventoryService;
import org.lea.imsback.services.LogAnalysisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping
@Tag(
        name = "Inventario",
        description = "Controlador Reactivo que gestiona las operaciones CRUD y de"+
                " reserva de stock del sistema de inventario distribuido."
)
public class InventoryController {
    private final InventoryService inventoryService;
    private final LogAnalysisService logAnalysisService;

    public InventoryController(InventoryService inventoryService, LogAnalysisService logAnalysisService) {
        this.inventoryService = inventoryService;
        this.logAnalysisService = logAnalysisService;
    }

    /**
     * Resuelve el log completo de la excepción para que la IA lo analice.
     */
    private String formatErrorLog(ReservationRequest request, Throwable error) {
        // Creamos una entrada de log detallada para el contexto de la IA
        return String.format(
                "CRITICAL EXCEPTION on SKU Reservation. Request Details: StoreId=%s, SKU=%s, Quantity=%d.\n" +
                        "STACK TRACE:\n%s",
                request.storeId(),
                request.sku(),
                request.quantity(),
                error.toString()
        );
    }

    @Operation(summary = "Reserva stock de un SKU en una tienda específica",
            description = "Esta operación es **no bloqueante y reactiva**, lo que permite manejar" +
                    " * un alto volumen de peticiones concurrentes sin saturar los hilos del servidor," +
                    " * cumpliendo así con los requisitos de alto rendimiento del sistema"
    )

    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock reservado exitosamente. Evento publicado."),
            @ApiResponse(responseCode = "409", description = "Conflicto. Stock insuficiente o SKU/Tienda no encontrado."),
            @ApiResponse(responseCode = "400", description = "Petición inválida (ej. cuerpo JSON incorrecto).")
    })

    @PostMapping("/reserve")
    public Mono<ResponseEntity<String>> reserveStock(@Valid @RequestBody ReservationRequest request) {

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
                })
                // === Bloque de Manejo de Errores con Diagnóstico de IA ===
                .onErrorResume(Exception.class, error -> {
                    // 1. Prepara el log para la IA
                    String logEntry = formatErrorLog(request, error);

                    // 2. Llama al servicio de análisis de logs de la IA (retorna Mono<String>)
                    return logAnalysisService.analyzeErrorLog(logEntry)
                            .map(aiAnalysis ->
                                    // 3. Mapea el análisis de la IA a una respuesta HTTP 500
                                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                            .body("ERROR CRÍTICO INTERNO. Se ha activado el diagnóstico de IA. \n\n" + aiAnalysis)
                            )
                            // 4. Fallback: Si la IA falla (ej. timeout), devuelve un mensaje genérico.
                            .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body("Error crítico. Fallo al obtener el diagnóstico de IA. Consulte logs de servidor.")
                            );
                }); // Fin de onErrorResume
    }

    @Operation(summary = "Crea un nuevo ítem en el inventario")
    @PostMapping("/create")
    public Mono<ResponseEntity<String>> createItem(@Valid @RequestBody Item item) {
        return inventoryService.createItem(item)
                .map(created -> created
                        ? ResponseEntity.ok("Ítem creado correctamente.")
                        : ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Ya existe un ítem con ese SKU en la tienda."));
    }

    @GetMapping("/store/{storeId}")
    @Operation(summary = "Obtiene todos los ítems de una tienda")
    public Flux<Item> getItemsByStore(@PathVariable String storeId) {
        return inventoryService.getItemsByStore(storeId);
    }

    @GetMapping("/store/{storeId}/sku/{sku}")
    @Operation(summary = "Obtiene un ítem específico por tienda y SKU")
    public Mono<ResponseEntity<Item>> getItemBySkuAndStore(@PathVariable String storeId, @PathVariable String sku) {
        return inventoryService.getItemBySkuAndStore(storeId, sku)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PatchMapping("/update")
    @Operation(summary = "Actualiza manualmente la cantidad de un ítem existente")
    public Mono<ResponseEntity<String>> updateItem(@RequestBody Item item) {
        return inventoryService.updateItemQuantity(item)
                .map(updated -> updated
                        ? ResponseEntity.ok("Cantidad actualizada correctamente.")
                        : ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Ítem no encontrado."));
    }

    @DeleteMapping("/delete/{storeId}/{sku}")
    @Operation(summary = "Elimina un ítem del inventario")
    public Mono<ResponseEntity<String>> deleteItem(@PathVariable String storeId, @PathVariable String sku) {
        return inventoryService.deleteItem(storeId, sku)
                .map(deleted -> deleted
                        ? ResponseEntity.ok("Ítem eliminado correctamente.")
                        : ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Ítem no encontrado."));
    }
}