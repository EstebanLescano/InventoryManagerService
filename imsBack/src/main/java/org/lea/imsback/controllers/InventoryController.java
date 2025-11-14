package org.lea.imsback.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.lea.imsback.models.dtos.ReservationRequest;
import org.lea.imsback.models.Item;
import org.lea.imsback.services.ErrorDignosisService;
import org.lea.imsback.services.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/inventario")
@Tag(
        name = "Inventario",
        description = "Controlador Reactivo que gestiona las operaciones CRUD y de"+
                " reserva de stock del sistema de inventario distribuido."
)
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {
    private final InventoryService inventoryService;
    private final ErrorDignosisService errorDignosisService;


    public InventoryController(InventoryService inventoryService, ErrorDignosisService errorDignosisService) {
        this.inventoryService = inventoryService;

        this.errorDignosisService = errorDignosisService;
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
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/reserve")
    public Mono<ResponseEntity<String>> reserveStock(@Valid @RequestBody ReservationRequest request) {
        return inventoryService.tryReserveStock(request.storeId(), request.sku(), request.quantity())
                .map(success -> Boolean.TRUE.equals(success)
                        ? ResponseEntity.ok("Stock reservado. Evento de actualización publicado.")
                        : ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Reserva fallida. Stock insuficiente o ítem no encontrado."))
                .onErrorResume(error -> errorDignosisService.handleError(request, error));
    }

    @Operation(summary = "Crea un nuevo ítem en el inventario")
    @PostMapping("/create")
    public Mono<ResponseEntity<String>> createItem(@Valid @RequestBody Item item) {
        return inventoryService.createItem(item)
                .map(created -> Boolean.TRUE.equals(created)
                        ? ResponseEntity.ok("Ítem creado correctamente.")
                        : ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Ya existe un ítem con ese SKU en la tienda."))
                .onErrorResume(error -> errorDignosisService.handleError(item, error));
    }

    @GetMapping("/store/{storeId}")
    @Operation(summary = "Obtiene todos los ítems de una tienda")
    public Flux<Item> getItemsByStore(@PathVariable String storeId) {
        return inventoryService.getItemsByStore(storeId)
                .onErrorResume(error -> {
                    // En este caso, devolvemos un flujo vacío con log automático
                    errorDignosisService.handleError(storeId, error).subscribe();
                    return Flux.empty();
                });
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
                .map(updated -> Boolean.TRUE.equals(updated)
                        ? ResponseEntity.ok("Cantidad actualizada correctamente.")
                        : ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Ítem no encontrado."))
                .onErrorResume(error -> errorDignosisService.handleError(item, error));
    }

    @DeleteMapping("/delete/{storeId}/{sku}")
    @Operation(summary = "Elimina un ítem del inventario")
    public Mono<ResponseEntity<String>> deleteItem(@PathVariable String storeId, @PathVariable String sku) {
        return inventoryService.deleteItem(storeId, sku)
                .map(deleted -> Boolean.TRUE.equals(deleted)
                        ? ResponseEntity.ok("Ítem eliminado correctamente.")
                        : ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Ítem no encontrado."))
                .onErrorResume(error -> errorDignosisService.handleError(Map.of("storeId", storeId, "sku", sku), error));
    }
}