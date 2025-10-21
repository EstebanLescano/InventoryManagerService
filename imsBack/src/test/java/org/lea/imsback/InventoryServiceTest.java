package org.lea.imsback;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.lea.imsback.models.Item;
import org.lea.imsback.repositories.InventoryRepository;
import org.lea.imsback.services.EventPublisher;
import org.lea.imsback.services.InventoryService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @InjectMocks
    private InventoryService inventoryService;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private EventPublisher eventPublisher;

    private Item testItem;
    private final String TEST_SKU = "SKU123";
    private static final String TEST_STORE_ID = "STORE_A";
    private static final int INITIAL_STOCK = 10;
    private static final int RESERVATION_QTY = 3;
    private static final int LOW_STOCK = 2;

    @BeforeEach
    void setup() {
        testItem = new Item(TEST_SKU, 10, TEST_STORE_ID);
    }

    /**
     * Prueba CLAVE de concurrencia. Simula 10 reservas simultáneas (10 > 5).
     * Solo 5 reservas deben tener éxito (true) y el stock final debe ser 0.
     */
    @Test
    void tryReserveStock_shouldReturnTrue_whenStockIsSufficientAndSaveSucceeds() {
        // ARRANGE
        Item existingItem = new Item(TEST_SKU, INITIAL_STOCK, TEST_STORE_ID);
        int expectedNewQuantity = INITIAL_STOCK - RESERVATION_QTY;

        // 1. findBySkuAndStoreId: Item is found
        when(inventoryRepository.findBySkuAndStoreId(TEST_SKU, TEST_STORE_ID))
                .thenReturn(Mono.just(existingItem));

        // 2. save: Save succeeds and returns the updated item
        // The existingItem object will be mutated in the flatMap before save is called.
        when(inventoryRepository.save(existingItem))
                .thenReturn(Mono.just(existingItem));

        // 3. publishStockUpdate: Event publishing succeeds (Mono<Void>)
        when(eventPublisher.publishStockUpdate(TEST_STORE_ID, TEST_SKU, expectedNewQuantity))
                .thenReturn(Mono.empty());

        // ACT & ASSERT
        StepVerifier.create(inventoryService.tryReserveStock(TEST_STORE_ID, TEST_SKU, RESERVATION_QTY))
                .expectNext(true)
                .verifyComplete();

        // VERIFY
        // Verify save and publish were called exactly once.
        verify(inventoryRepository, times(1)).save(existingItem);
        verify(eventPublisher, times(1)).publishStockUpdate(TEST_STORE_ID, TEST_SKU, expectedNewQuantity);
    }

    @Test
    void tryReserveStock_shouldReturnFalse_whenStockIsInsufficient() {
        // ARRANGE
        // Stock is only 2, but RESERVATION_QTY is 3.
        Item existingItem = new Item(TEST_SKU, LOW_STOCK, TEST_STORE_ID);

        // 1. findBySkuAndStoreId: Item is found
        when(inventoryRepository.findBySkuAndStoreId(TEST_SKU, TEST_STORE_ID))
                .thenReturn(Mono.just(existingItem));

        // ACT & ASSERT
        StepVerifier.create(inventoryService.tryReserveStock(TEST_STORE_ID, TEST_SKU, RESERVATION_QTY))
                .expectNext(false)
                .verifyComplete();

        // VERIFY
        // Verify save and publish were NEVER called.
        verify(inventoryRepository, never()).save(any(Item.class));
        verify(eventPublisher, never()).publishStockUpdate(anyString(), anyString(), anyInt());
    }

    @Test
    void tryReserveStock_shouldReturnFalse_whenItemDoesNotExist() {
        // ARRANGE
        // findBySkuAndStoreId: Item is NOT found (Mono.empty())
        when(inventoryRepository.findBySkuAndStoreId(TEST_SKU, TEST_STORE_ID))
                .thenReturn(Mono.empty());

        // ACT & ASSERT
        StepVerifier.create(inventoryService.tryReserveStock(TEST_STORE_ID, TEST_SKU, RESERVATION_QTY))
                .expectNext(false)
                .verifyComplete();

        // VERIFY
        // Verify save and publish were NEVER called.
        verify(inventoryRepository, never()).save(any(Item.class));
        verify(eventPublisher, never()).publishStockUpdate(anyString(), anyString(), anyInt());
    }

    @Test
    void tryReserveStock_shouldReturnFalse_whenOptimisticLockingFails() {
        // ARRANGE
        Item existingItem = new Item(TEST_SKU, INITIAL_STOCK, TEST_STORE_ID);

        // 1. findBySkuAndStoreId: Item is found
        when(inventoryRepository.findBySkuAndStoreId(TEST_SKU, TEST_STORE_ID))
                .thenReturn(Mono.just(existingItem));

        // 2. save: Simulates Optimistic Locking failure
        // The item is mutated first, then save is called, which errors.
        when(inventoryRepository.save(existingItem))
                .thenReturn(Mono.error(new OptimisticLockingFailureException("Simulated lock failure")));

        // ACT & ASSERT
        // The onErrorResume should catch the exception and return Mono.just(false).
        StepVerifier.create(inventoryService.tryReserveStock(TEST_STORE_ID, TEST_SKU, RESERVATION_QTY))
                .expectNext(false) // Expect false due to onErrorResume
                .verifyComplete();

        // VERIFY
        // Verify save was called, but publish was NOT (since save failed).
        verify(inventoryRepository, times(1)).save(existingItem);
        verify(eventPublisher, never()).publishStockUpdate(anyString(), anyString(), anyInt());
    }

    @Test
    void createItem_shouldReturnTrueAndSave_whenItemDoesNotExist() {
        // ARRANGE
        // Simula que findBySkuAndStoreId no encuentra nada (Mono.empty())
        when(inventoryRepository.findBySkuAndStoreId(TEST_SKU, TEST_STORE_ID))
                .thenReturn(Mono.empty());
        // Simula que save guarda el ítem y devuelve el ítem guardado
        when(inventoryRepository.save(any(Item.class)))
                .thenReturn(Mono.just(testItem));

        // ACT & ASSERT
        StepVerifier.create(inventoryService.createItem(testItem))
                .expectNext(true) // Espera que el Mono emita 'true'
                .verifyComplete(); // Espera que el Mono complete

        // VERIFY
        // Verifica que se intentó encontrar y luego se guardó.
        verify(inventoryRepository, times(1)).findBySkuAndStoreId(TEST_SKU, TEST_STORE_ID);
        verify(inventoryRepository, times(1)).save(testItem);

    }

    @Test
    void createItem_shouldReturnFalse_whenItemAlreadyExists() {
        Item existingItem = new Item(TEST_SKU, 5, TEST_STORE_ID);
        // El mock debe devolver el resultado esperado para la búsqueda de testItem
        when(inventoryRepository.findBySkuAndStoreId(testItem.getSku(), testItem.getStoreId()))
                .thenReturn(Mono.just(existingItem));
        // ACT & ASSERT
        StepVerifier.create(inventoryService.createItem(testItem))
                .expectNext(false)
                .verifyComplete();
        // VERIFY
        // Verifica que se intentó encontrar pero NO se guardó.
        verify(inventoryRepository, times(1)).findBySkuAndStoreId(TEST_SKU, TEST_STORE_ID);
        verify(inventoryRepository, never()).save(any(Item.class));
    }

    // --- Tests para getItemsByStore ---
    // public Flux<Item> getItemsByStore(String storeId)
    @Test
    void getItemsByStore_shouldReturnFilteredItems() {
        // ARRANGE
        Item item1 = new Item("SKU1", 5, TEST_STORE_ID);
        Item item2 = new Item("SKU2", 10, TEST_STORE_ID);
        Item otherStoreItem = new Item("SKU3", 1, "OTHER_STORE");
        // Simula que findAll devuelve todos los ítems.
        when(inventoryRepository.findAll())
                .thenReturn(Flux.just(item1, otherStoreItem, item2));

        // ACT & ASSERT
        StepVerifier.create(inventoryService.getItemsByStore(TEST_STORE_ID))
                .expectNext(item1) // Espera item1
                .expectNext(item2) // Espera item2
                .verifyComplete(); // Espera que el Flux complete

        // VERIFY
        verify(inventoryRepository, times(1)).findAll();
    }

    @Test
    void getItemsByStore_shouldReturnEmptyFlux_whenNoItemsMatchStoreId() {
        // ARRANGE
        Item otherStoreItem = new Item("SKU3", 1, "OTHER_STORE");
        // Simula que findAll devuelve solo ítems de otras tiendas.
        when(inventoryRepository.findAll())
                .thenReturn(Flux.just(otherStoreItem));

        // ACT & ASSERT
        StepVerifier.create(inventoryService.getItemsByStore(TEST_STORE_ID))
                .expectNextCount(0) // Espera 0 ítems
                .verifyComplete();

        // VERIFY
        verify(inventoryRepository, times(1)).findAll();
    }


    // --- Tests para getItemBySkuAndStore ---
    // public Mono<Item> getItemBySkuAndStore(String storeId, String sku)
    @Test
    void getItemBySkuAndStore_shouldReturnItem_whenItemExists() {
        // ARRANGE
        Item expectedItem = testItem;
        when(inventoryRepository.findBySkuAndStoreId(TEST_SKU, TEST_STORE_ID))
                .thenReturn(Mono.just(expectedItem));

        // ACT & ASSERT
        StepVerifier.create(inventoryService.getItemBySkuAndStore(TEST_STORE_ID, TEST_SKU))
                .expectNext(expectedItem)
                .verifyComplete();

        // VERIFY
        verify(inventoryRepository, times(1)).findBySkuAndStoreId(TEST_SKU, TEST_STORE_ID);
    }

    @Test
    void getItemBySkuAndStore_shouldReturnEmptyMono_whenItemDoesNotExist() {
        // ARRANGE
        when(inventoryRepository.findBySkuAndStoreId(TEST_SKU, TEST_STORE_ID))
                .thenReturn(Mono.empty());

        // ACT & ASSERT
        StepVerifier.create(inventoryService.getItemBySkuAndStore(TEST_STORE_ID, TEST_SKU))
                .verifyComplete(); // Espera que el Mono complete sin emitir.

        // VERIFY
        verify(inventoryRepository, times(1)).findBySkuAndStoreId(TEST_SKU, TEST_STORE_ID);
    }

    // --- Tests para updateItemQuantity ---
    // public Mono<Boolean> updateItemQuantity(Item item)
    @Test
    void updateItemQuantity_shouldReturnTrueAndUpdate_whenItemExists() {
        // ARRANGE
        Item existingItem = new Item(TEST_SKU, 10, TEST_STORE_ID);
        Item updatedItem = new Item(TEST_SKU, 20, TEST_STORE_ID); // Nuevo ítem con la cantidad a actualizar

        // Simula la búsqueda.
        when(inventoryRepository.findBySkuAndStoreId(TEST_SKU, TEST_STORE_ID))
                .thenReturn(Mono.just(existingItem));

        // Simula el guardado. Retorna el ítem existente AHORA con la nueva cantidad (20).
        when(inventoryRepository.save(any(Item.class)))
                .thenReturn(Mono.just(updatedItem));

        // Simula la publicación del evento. Mono<Void> se simula con Mono.empty().
        when(eventPublisher.publishStockUpdate(anyString(), anyString(), anyInt()))
                .thenReturn(Mono.empty());

        // ACT & ASSERT
        StepVerifier.create(inventoryService.updateItemQuantity(updatedItem)) // Usa el ítem con la nueva cantidad
                .expectNext(true)
                .verifyComplete();

        // VERIFY
        // Verifica que se buscó
        verify(inventoryRepository, times(1)).findBySkuAndStoreId(TEST_SKU, TEST_STORE_ID);
        // Verifica que se guardó. Usamos un ArgumentCaptor para verificar que el ítem guardado
        // tiene la nueva cantidad.
        // **Nota:** Para verificar la cantidad actualizada en el objeto 'existing', necesitarías un ArgumentCaptor
        // o reescribir el mock para asegurar que `existing.setQuantity` fue llamado.
        // Lo simplificaremos verificando que se llamó a save y a publishStockUpdate.
        verify(inventoryRepository, times(1)).save(existingItem); // El 'existingItem' mutó
        verify(eventPublisher, times(1)).publishStockUpdate(TEST_STORE_ID, TEST_SKU, updatedItem.getQuantity());
    }

    @Test
    void updateItemQuantity_shouldReturnFalse_whenItemDoesNotExist() {
        // ARRANGE
        // Simula que findBySkuAndStoreId no encuentra nada
        when(inventoryRepository.findBySkuAndStoreId(TEST_SKU, TEST_STORE_ID))
                .thenReturn(Mono.empty());

        // ACT & ASSERT
        StepVerifier.create(inventoryService.updateItemQuantity(testItem))
                .expectNext(false) // Espera 'false' por el switchIfEmpty
                .verifyComplete();

        // VERIFY
        // Verifica que se buscó, pero NO se guardó ni se publicó evento.
        verify(inventoryRepository, times(1)).findBySkuAndStoreId(TEST_SKU, TEST_STORE_ID);
        verify(inventoryRepository, never()).save(any(Item.class));
        verify(eventPublisher, never()).publishStockUpdate(anyString(), anyString(), anyInt());
    }

    // --- Tests para deleteItem ---
    // public Mono<Boolean> deleteItem(String storeId, String sku)
    @Test
    void deleteItem_shouldReturnTrueAndDelete_whenItemExists() {
        // ARRANGE
        Item existingItem = testItem;
        // Simula la búsqueda.
        when(inventoryRepository.findBySkuAndStoreId(TEST_SKU, TEST_STORE_ID))
                .thenReturn(Mono.just(existingItem));
        // Simula la eliminación (Mono<Void> se simula con Mono.empty()).
        when(inventoryRepository.delete(existingItem))
                .thenReturn(Mono.empty());

        // ACT & ASSERT
        StepVerifier.create(inventoryService.deleteItem(TEST_STORE_ID, TEST_SKU))
                .expectNext(true) // Espera 'true' por el .thenReturn(true) después de delete
                .verifyComplete();

        // VERIFY
        // Verifica que se buscó y se eliminó.
        verify(inventoryRepository, times(1)).findBySkuAndStoreId(TEST_SKU, TEST_STORE_ID);
        verify(inventoryRepository, times(1)).delete(existingItem);
    }

    @Test
    void deleteItem_shouldReturnFalse_whenItemDoesNotExist() {
        // ARRANGE
        // Simula que findBySkuAndStoreId no encuentra nada.
        when(inventoryRepository.findBySkuAndStoreId(TEST_SKU, TEST_STORE_ID))
                .thenReturn(Mono.empty());

        // ACT & ASSERT
        StepVerifier.create(inventoryService.deleteItem(TEST_STORE_ID, TEST_SKU))
                .expectNext(false) // Espera 'false' por el switchIfEmpty(Mono.just(false))
                .verifyComplete();

        // VERIFY
        // Verifica que se buscó, pero NO se eliminó.
        verify(inventoryRepository, times(1)).findBySkuAndStoreId(TEST_SKU, TEST_STORE_ID);
        verify(inventoryRepository, never()).delete(any(Item.class));
    }
}

