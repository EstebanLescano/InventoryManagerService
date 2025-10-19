package org.lea.imsback.repositories;

import org.lea.imsback.models.Item;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface InventoryRepository extends R2dbcRepository<Item, String> {
    Mono<Item> findBySkuAndStoreId(String sku, String storeId);
    Mono<Void> deleteBySkuAndStoreId(String sku, String storeId);
    Mono<Long> countByStoreId(String storeId);

}
