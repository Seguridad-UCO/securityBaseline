package co.edu.uco.seguridad.pdp;

import co.edu.uco.seguridad.shared.config.ProjectPackages;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Implementación de línea base del contenedor PDP del mapa C4/Modulith aceptado.
 */
@SpringBootApplication(scanBasePackages = ProjectPackages.BASE)
public class PdpApplication {
    public static void main(String[] args) {
        SpringApplication.run(PdpApplication.class, args);
    }
}
