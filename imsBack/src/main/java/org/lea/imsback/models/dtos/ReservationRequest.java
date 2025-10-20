package org.lea.imsback.models.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO inmutable utilizado para solicitar la reserva de stock.
 * Define los parámetros necesarios para identificar qué producto y dónde debe
 * realizarse la reserva, y la cantidad. Su inmutabilidad (al ser un 'record')
 * Al ser un {@code record}, garantiza la inmutabilidad y la consistencia de los
 * datos de la solicitud durante su procesamiento.
 **/

public record ReservationRequest(
        @NotBlank String storeId,
        @NotBlank String sku,
        @Min(1) int quantity
) {
}
