package co.edu.uco.seguridad;

import co.edu.uco.seguridad.pdp.PdpApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = PdpApplication.class)
class SeguridadApplicationTests extends AbstractSurrealDbIntegrationTest {

    @Test
    void contextLoads() {
    }
}
