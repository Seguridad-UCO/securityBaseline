package co.edu.uco.seguridad;

import co.edu.uco.seguridad.pdp.PdpApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = PdpApplication.class)
class PdpApplicationTests extends AbstractSurrealDbIntegrationTest {

    @Test
    void contextLoads() {
        // Intentionally empty: the test's only purpose is to fail if the Spring context
        // does not start correctly.
    }
}
