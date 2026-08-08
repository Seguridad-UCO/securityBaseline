package co.edu.uco.seguridad.pdp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Implementación de línea base del contenedor PDP del mapa C4/Modulith aceptado. */
@SpringBootApplication(scanBasePackages = "co.edu.uco.seguridad")
public class PdpApplication {
    public static void main(String[] args) { SpringApplication.run(PdpApplication.class, args); }
}
