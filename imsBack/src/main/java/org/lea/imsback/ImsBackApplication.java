package org.lea.imsback;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(
        info = @Info(
                title = "Servicio de Gestión de Inventario (IMS)",
                version = "1.0",
                description = "API reactiva para la gestión y reserva de stock en tiempo real."
        )
)
@SpringBootApplication
public class ImsBackApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImsBackApplication.class, args);
    }

}
