🧭 Inventory Manager Service Reactive DDD Prototype

OBJETIVO:
Prototipo de sistema de gestión de inventario distribuido, diseñado con principios DDD 
(Domain-Driven Design) y arquitectura reactiva (Spring WebFlux + R2DBC).
El objetivo es optimizar la consistencia del inventario, reducir latencia y garantizar disponibilidad
mediante un modelo de eventos y operaciones no bloqueantes.

⚙️ STACK TECNOLOGICO

Java 21
Spring Boot 3.3.x (WebFlux + R2DBC)
H2 Database (en memoria)
Maven como gestor de dependencias
Lombok
SLF4J (logs)
Reactive Programming (Mono/Flux)

🧩 ARQUITECTURA
┌──────────────────────────────┐
│          API Layer           │
│ InventoryController          │
│ (maneja las requests HTTP)   │
└───────────────▲──────────────┘
│
┌───────────────┴──────────────┐
│       Application Layer      │
│ InventoryService             │
│ (caso de uso: reservar stock)│
└───────────────▲──────────────┘
│
┌───────────────┴──────────────┐
│         Domain Layer          │
│ Item, StockUpdateEvent        │
│ (modelo y reglas de negocio)  │
└───────────────▲──────────────┘
│
┌───────────────┴──────────────┐
│      Infrastructure Layer     │
│ InventoryRepository,          │
│ EventPublisher (simulado)     │
└──────────────────────────────┘

🧠 Flujo Principal

El cliente hace POST /api/v1/inventory/reserve.

El servicio busca el ítem y valida el stock disponible.

Si hay stock suficiente:

Actualiza el registro.

Publica un StockUpdateEvent (simulado en log).

Si no hay stock → responde con 409 Conflict.

# 🧪 Como Probar por medio de Swagger UI

Iniciar la aplicación y acceder a:
curl -X POST http://localhost:8080/webjars/swagger-ui/index.html#/\
-H "Content-Type: application/json" \
-d '{
"storeId": "STORE_A",
"sku": "A101",
"quantity": 2
}'

Respuesta exitosa
Stock reservado. Evento de actualización publicado.


Respuesta con error
Reserva fallida. Stock insuficiente o ítem no encontrado.

🚀 Cómo Ejecutar el Proyecto
Acceder a:
http://localhost:9090/api/inventory/reserve

🧩 Decisiones Técnicas Clave
Decisión	Justificación
Arquitectura DDD	Permite aislar la lógica del dominio del framework y la infraestructura.
WebFlux (reactivo)	Reduce la latencia y mejora la concurrencia en IO.
R2DBC + H2	Permite persistencia reactiva sin bloqueo.
EventPublisher simulado	Representa la futura integración con Kafka o RabbitMQ.
Log detallado	Facilita la trazabilidad de eventos y errores.