package co.edu.uco.seguridad;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Modulith ({@link ModulithStructureTests}) verifica fronteras ENTRE módulos — cortes verticales
 * (¿puede {@code resources} importar {@code applications}?). No ve fronteras ENTRE CAPAS dentro de un
 * mismo módulo, así que la regla de dependencias de Clean Architecture (ADR-0001: el dominio no
 * conoce el framework, la aplicación no conoce el adaptador) no tenía ningún enforcement automático
 * — se sostenía solo porque nadie la había violado todavía.
 */
class LayeredArchitectureTests {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("co.edu.uco.seguridad");

    @Test
    void application_layer_never_depends_on_infrastructure() {
        ArchRule rule = noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..");
        rule.check(CLASSES);
    }

    @Test
    void domain_layer_never_depends_on_infrastructure() {
        ArchRule rule = noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..");
        rule.check(CLASSES);
    }

    @Test
    void domain_layer_never_depends_on_application() {
        ArchRule rule = noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..application..");
        rule.check(CLASSES);
    }
}
