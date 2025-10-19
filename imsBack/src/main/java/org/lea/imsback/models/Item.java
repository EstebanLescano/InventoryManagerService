package org.lea.imsback.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor // Necesario para R2DBC
@Table("item")
// Implementar Persistable<Long>
public class Item implements Persistable<Long> {

    @Id
    private Long id;

    private String sku;
    private int quantity;
    private String storeId;

    // Constructor de negocio (para nuevos ítems, si los hubiera)
    public Item(String sku, int quantity, String storeId) {
        this.sku = sku;
        this.quantity = quantity;
        this.storeId = storeId;
    }

    // -----------------------------------------------------------------
    // Implementación Persistable
    // -----------------------------------------------------------------

    // Indica si la entidad es NUEVA (true = INSERT) o EXISTENTE (false = UPDATE)
    @Override
    public boolean isNew() {
        // Si el ID es nulo, es nueva. Si tiene valor, no es nueva (UPDATE).
        return this.id == null;
    }

    // Retorna el ID (Persistable requiere este método)
    @Override
    public Long getId() {
        return this.id;
    }
}

