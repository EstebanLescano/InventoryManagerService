package org.lea.imsback;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lea.imsback.models.Item;
import org.lea.imsback.repositories.InventoryRepository;
import org.lea.imsback.services.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class InventoryServiceTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    private static final String SKU_TEST = "CONC-100";
    private static final String STORE_ID = "TEST_STORE";
    private static final int INITIAL_STOCK = 5;
    private static final int CONCURRENT_ATTEMPTS = 10;
    private static final int RESERVATION_QTY = 1;


    @BeforeEach
    void setup() {
        // Limpiar y crear stock inicial para cada test de forma reactiva
        // Nota: Se bloquea solo en el setup, lo cual es permitido.
        inventoryRepository.deleteBySkuAndStoreId(SKU_TEST, STORE_ID)
                .then(inventoryRepository.save(new Item(SKU_TEST, INITIAL_STOCK, STORE_ID)))
                .block(Duration.ofSeconds(5));
    }

    /**
     * Prueba CLAVE de concurrencia. Simula 10 reservas simultáneas (10 > 5).
     * Solo 5 reservas deben tener éxito (true) y el stock final debe ser 0.
     */
    @Test
    void testConcurrentReservations_EnsuresNoOverSelling() {
        // 1. Crear 10 intentos de reserva concurrentes
        Flux<Mono<Boolean>> reservationAttempts = Flux.range(1, CONCURRENT_ATTEMPTS)
                .map(i -> inventoryService.tryReserveStock(STORE_ID, SKU_TEST, RESERVATION_QTY));

        // 2. Ejecutar todas las reservas concurrentemente (paralelismo 10) y contar los éxitos
        Mono<Long> successfulReservationsCount = reservationAttempts
                .flatMap(mono -> mono, 10) // Ejecución concurrente
                .filter(success -> success)  // Contar solo los 'true'
                .count();

        // 3. Encadenar la verificación final de la base de datos a la terminación del conteo
        // Se usa flatMap para garantizar que esta lógica se ejecute SOLO después de que
        // todas las operaciones de reserva hayan finalizado.
        Mono<Item> finalVerificationChain = successfulReservationsCount
                .flatMap(count -> {
                    // Aserción 1: El número de reservas exitosas debe ser igual al stock inicial
                    assertEquals((long) INITIAL_STOCK, count,
                            "Solo el stock inicial debe ser exitoso.");

                    // Aserción 2: Obtener el estado final del ítem de la BD
                    return inventoryRepository.findBySkuAndStoreId(SKU_TEST, STORE_ID);
                });

        // 4. Bloquear y verificar el resultado final (solo para el test)
        Item finalItem = finalVerificationChain
                .block(Duration.ofSeconds(10)); // Bloquear por un tiempo suficiente

        // Aserción 3: Verificar que el stock final es 0
        assertNotNull(finalItem, "El ítem debe existir en la base de datos.");
        assertEquals(0, finalItem.getQuantity(), "El stock final debe ser cero (cero sobreventa).");
    }
}
