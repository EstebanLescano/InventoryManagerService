
# **Para el funcionamiento del proyecto, sigue estos pasos:**

1. Clona el repositorio a tu máquina local.
2. Asegúrate de tener Java21 y Maven instalados.
3. Navega al directorio del proyecto y ejecuta `mvn spring-boot:run` para iniciar la aplicación.
4. utilizo intellij si se abre el proyecto completo estara disponibles los servicios para correr solo con dar play
5. La aplicación disponible en los siguientes puertos:
6. - frontend: http://localhost:9091
7. - backend: http://localhost:9090
8. - Gateway: http://localhost:9092
9. Usa Postman o cualquier cliente HTTP para interactuar con los endpoints REST del backend.
10. tambien se configuro Swagger para documentar los endpoints,
11. accede a http://localhost:9090/webjars/swagger-ui/index.html 
12. Ejecuta los tests con `mvn test` para verificar el correcto funcionamiento de la aplicación.
13. estos no se aplicaron a todos los endpoint, solo a los de reserva de stock.