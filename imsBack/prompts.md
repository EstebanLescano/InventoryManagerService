# Prompts usados con GenAI (ejemplos)

1) Generar estructura DDD para Java/Spring con maven y JPA:
```
Genera un proyecto Java con Spring Boot y Maven que implemente un sistema de gestión de 
inventario siguiendo principios DDD (Domain-Driven Design). La estructura del proyecto
 debe incluir las siguientes capas:
- Domain Layer: Entidades Item y StockUpdateEvent, con lógica de negocio para gestionar stock.
- Application Layer: Servicio InventoryService con caso de uso para reservar stock.
- Infrastructure Layer: Repositorio InventoryRepository usando Spring Data JPA para persistencia.
- API Layer: Controlador REST InventoryController con endpoints para consultar y reservar stock.
Utiliza H2 como base de datos en memoria y configura JPA
```

2) Implementar reintentos ante OptimisticLockingFailureException:
```
Escribe un componente que reintente hasta 3 veces operaciones transaccionales que fallen
 por OptimisticLockingFailureException.
```

3) Escribir tests de integración con TestRestTemplate:
```
Escribe un test que arranque el contexto y pruebe reservar stock concurrentemente usando
 multiple threads o async tasks.
```
