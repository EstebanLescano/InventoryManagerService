
# **Proyecto: Sistema de Gestión Distribuida**

# **_DESCRIPCION_**

Este proyecto es un sistema de gestión distribuida basado en microservicios siguiendo principios de DDD (Domain-Driven Design).
El sistema está diseñado con Spring WebFlux para un backend reactivo, un front-end reactivo, 
y un API Gateway que centraliza la seguridad mediante Keycloak.
Se incluyen tests unitarios e integrales, documentación Swagger, y un entorno Docker para facilitar el despliegue.

# _**DIAGRAMA_** :
![img_1.png](img_1.png)


# **_ARQUITECTURA_**

Microservicios Backend:

Separación clara entre domain, service, repository y controller.

Comunicación reactiva usando Spring WebFlux y WebClient.

Persistencia reactiva con R2DBC (para bases como PostgreSQL).

API Gateway:

Centraliza el enrutamiento a los microservicios.

Protege los endpoints mediante JWT generado por Keycloak.

Permite balanceo de carga y manejo de rutas.

Front-end Reactivo:

Consume microservicios a través del API Gateway.

Soporta flujos reactivos y UI dinámica.

Seguridad:

Keycloak maneja autenticación y autorización por roles y recursos.

Roles definidos por microservicio y realm, aplicando RBAC.

Documentación:

Cada servicio expone su documentación Swagger en /swagger-ui.html.

Contenerización: Docker / Docker Compose.


# **_TECNOLOGIAS_**

Backend: Java 21, Spring Boot, Spring WebFlux, R2DBC.

Frontend: Reactivo (Spring WebFlux + Thymeleaf o React).

Seguridad: Keycloak (JWT, roles, permisos).

Base de datos: H2 para pruebas.

Testing: JUnit 5, Spring Boot Test, unitarios.

Build: Maven.

Documentación API: Swagger / OpenAPI.

Contenerización: Docker / Docker Compose.

# **_ESTRUCTURA DEL PROYECTO_**

/project-root
├── backend-service-1
│   ├── src/main/java
│   │   ├── controller
│   │   ├── service
│   │   ├── repository
│   │   └── domain
│   └── resources
├── backend-service-2
│   └── ...
├── frontend
│   └── ...
├── api-gateway
│   └── ...
├── docker-compose.yml
└── README.md


# **_FLUJO DE AUTENTICACION_**

1- El usuario inicia sesión a través de Keycloak.
2- Keycloak emite un JWT con roles y permisos.
3- El API Gateway valida el token y enruta la petición al microservicio correspondiente.
4- Los microservicios verifican los roles y permisos según RBAC antes de procesar la solicitud.

# **_ENDPOINT PRINCIPALES_**

Los endpoints aquí son ejemplos; cada servicio tiene su propia documentación Swagger.

Microservicio de Inventario
Método	Ruta	Descripción
GET	/api/v1/inventory/{id}	Obtener stock por producto
POST	/api/v1/inventory/reserve	Reservar stock
PUT	/api/v1/inventory/{id}	Actualizar stock

**Ejemplo de request para reservar stock:**
{
"storeId": "STORE_A",
"sku": "A101",
"quantity": 10
}

# **_CONFIGURACION DOCKER COMPOSE_**

Keycloak

# Cómo ejecutar

Levantar Keycloak y configurar realm, clientes y roles.

Configurar las propiedades de Keycloak en cada microservicio (application.properties).

Ejecutar los microservicios (backend, gateway).

Levantar el front-end Reactivo.

Acceder a la documentación Swagger en http://localhost:8080/webjars/swagger-ui/index.html#/Inventario/reserveStock