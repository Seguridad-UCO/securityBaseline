package co.edu.uco.seguridad.pdp.platform.infrastructure;

import co.edu.uco.seguridad.pdp.platform.application.PlatformAdministrationService;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/** Cableado del módulo de administración y esquema de sus identidades locales. */
@Configuration
class PlatformConfiguration {
    @Bean PlatformAdministrationService platformAdministrationService(SurrealDbClient db) { return new PlatformAdministrationServiceImpl(db); }
    @Bean ApplicationRunner platformSchema( SurrealDbClient db) { return new ApplicationRunner() {
        @Override public void run(ApplicationArguments args) {
            db.ensureNamespaceAndDatabase().then(db.execute("""
                DEFINE TABLE IF NOT EXISTS security_user SCHEMALESS;
                DEFINE TABLE IF NOT EXISTS external_identity SCHEMALESS;
                DEFINE INDEX IF NOT EXISTS external_identity_issuer_subject ON external_identity COLUMNS issuer, subject UNIQUE;
                DEFINE INDEX IF NOT EXISTS security_user_email ON security_user COLUMNS email UNIQUE;
                """, Map.of())).then().block();
        }
    }; }
}
