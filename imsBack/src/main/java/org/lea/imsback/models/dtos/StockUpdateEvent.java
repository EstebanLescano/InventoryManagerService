package org.lea.imsback.models.dtos;

// Usando Java Record para el DTO del evento (inmutable y conciso)
public record StockUpdateEvent(String storeId, String sku, int newQuantity) {}
