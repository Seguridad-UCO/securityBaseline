package co.edu.uco.seguridad;
import co.edu.uco.seguridad.pdp.PdpApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
class ModulithStructureTests { @Test void verifies_pdp_module_dependency_map() { ApplicationModules.of(PdpApplication.class).verify(); } }
