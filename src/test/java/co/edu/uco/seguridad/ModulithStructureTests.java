package co.edu.uco.seguridad;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithStructureTests {
    @Test void verifies_module_dependencies() { ApplicationModules.of(SeguridadApplication.class).verify(); }
}
