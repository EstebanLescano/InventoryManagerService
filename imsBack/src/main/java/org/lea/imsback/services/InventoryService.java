package org.lea.imsback.services;


import org.lea.imsback.models.Item;
import org.lea.imsback.repositories.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class InventoryService {
    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private EventPublisher eventPublisher;

    /**
     * Intenta reservar stock de forma reactiva.
     * La consistencia se maneja dentro de la cadena reactiva (Mono.flatMap)
     * que simula una transacción atómica.
     *
     * @return Mono<Boolean> - true si la reserva fue exitosa, false si no hay stock.
     */
    public Mono<Boolean> tryReserveStock(String storeId, String sku, int quantity) {

        // Comienza la cadena reactiva: buscar el ítem
        return inventoryRepository.findBySkuAndStoreId(sku, storeId)
                .flatMap(item -> {
                    // Validación: Si hay suficiente stock
                    int currentQuantity = item.getQuantity();
                    int newQuantity = currentQuantity - quantity;

                    if (newQuantity >= 0) {
                        item.setQuantity(newQuantity);

                        // **Paso 2: Operación atómica (Guardar y Publicar)**
                        // Retorna la cadena de operaciones: guardar -> publicar -> devolver true
                        return inventoryRepository.save(item)
                                .flatMap(savedItem -> {
                                    // Publicar el evento de actualización de stock
                                    return eventPublisher.publishStockUpdate(storeId, sku, savedItem.getQuantity())
                                            .thenReturn(true);
                                })
                                .doOnSuccess(s -> log.info("RESERVA EXITOSA: SKU {} en {}. Stock restante: {}", sku, storeId, newQuantity))
                                .onErrorResume(e -> {
                                    // Manejo de errores de la BD/evento. Aquí podrías intentar un rollback lógico.
                                    log.error("Error al guardar o publicar evento para SKU {} en {}: {}", sku, storeId, e.getMessage());
                                    // Por simplicidad, se devuelve false ante cualquier error de persistencia/evento.
                                    return Mono.just(false);
                                });
                    } else {
                        // Stock insuficiente
                        log.warn("RESERVA FALLIDA: Stock insuficiente para SKU {} en {}. Solicitado: {}, Disponible: {}", sku, storeId, quantity, currentQuantity);
                        return Mono.just(false);
                    }
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // Ítem no encontrado
                    log.warn("RESERVA FALLIDA: Ítem no encontrado: SKU {} en {}", sku, storeId);
                    return Mono.just(false);
                }));
    }

    public Mono<Boolean> createItem(Item item) {
        return inventoryRepository.findBySkuAndStoreId(item.getSku(), item.getStoreId())
                .flatMap(existing -> Mono.just(false)) // Ya existe
                .switchIfEmpty(
                        inventoryRepository.save(item)
                                .thenReturn(true)
                                .doOnSuccess(s -> log.info("ÍTEM CREADO: {} en {}", item.getSku(), item.getStoreId()))
                );
    }

    public Flux<Item> getItemsByStore(String storeId) {
        return inventoryRepository.findAll()
                .filter(item -> item.getStoreId().equals(storeId));
    }

    public Mono<Item> getItemBySkuAndStore(String storeId, String sku) {
        return inventoryRepository.findBySkuAndStoreId(sku, storeId);
    }

    public Mono<Boolean> updateItemQuantity(Item item) {
        return inventoryRepository.findBySkuAndStoreId(item.getSku(), item.getStoreId())
                .flatMap(existing -> {
                    existing.setQuantity(item.getQuantity());
                    return inventoryRepository.save(existing)
                            .then(eventPublisher.publishStockUpdate(existing.getStoreId(), existing.getSku(), existing.getQuantity()))
                            .thenReturn(true);
                })
                .switchIfEmpty(Mono.just(false));
    }

    public Mono<Boolean> deleteItem(String storeId, String sku) {
        return inventoryRepository.findBySkuAndStoreId(sku, storeId)
                .flatMap(existing -> inventoryRepository.delete(existing).thenReturn(true))
                .switchIfEmpty(Mono.just(false));
    }



}
