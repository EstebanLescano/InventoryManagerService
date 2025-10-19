package org.lea.imsback.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Simula el envío de eventos de stock de forma reactiva (no bloqueante).
 */
@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    public Mono<Void> publishStockUpdate(String storeId, String sku, int newQuantity) {
        StockUpdateEvent event = new StockUpdateEvent(storeId, sku, newQuantity);

        return Mono.fromRunnable(() -> {
            // Simulamos el envío real a un broker de mensajes (e.g., Kafka)
            log.info("EVENTO PUBLICADO -> StockUpdate: {}", event);
        }).then(); // Retorna Mono<Void> para mantener la cadena reactiva
    }
}
