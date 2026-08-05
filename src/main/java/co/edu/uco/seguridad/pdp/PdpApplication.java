package co.edu.uco.seguridad.pdp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Baseline implementation of the PDP container from the accepted C4/Modulith map. */
@SpringBootApplication(scanBasePackages = "co.edu.uco.seguridad")
public class PdpApplication {
    public static void main(String[] args) { SpringApplication.run(PdpApplication.class, args); }
}
