package co.edu.uco.seguridad.pep;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class PepArchitectureTests {
    @Test
    void module_boundaries_and_clean_layers_are_enforced() {
        ApplicationModules.of(PepApplication.class).verify();
        var classes = new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("co.edu.uco.seguridad.pep");
        noClasses().that().resideInAPackage("..application..").and().doNotHaveSimpleName("package-info").should().dependOnClassesThat()
                .resideInAnyPackage("..infrastructure..", "org.springframework..").check(classes);
        noClasses().that().resideInAnyPackage("..domain..", "..commons..").should().dependOnClassesThat()
                .resideInAnyPackage("..application..", "..infrastructure..", "org.springframework..", "reactor..")
                .check(classes);
        noClasses().should().dependOnClassesThat().resideInAnyPackage(
                "co.edu.uco.seguridad.pdp..", "co.edu.uco.seguridad.shared..", "jakarta.servlet..").check(classes);
    }
}
