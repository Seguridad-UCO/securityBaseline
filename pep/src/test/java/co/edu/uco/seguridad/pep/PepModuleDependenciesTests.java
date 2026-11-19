package co.edu.uco.seguridad.pep;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fija el conjunto de fronteras que el PEP declara (ADR-035). {@code verify()} solo falla cuando el codigo viola
 * la lista de dependencias, no cuando alguien la amplia o publica un paquete interno: eso lo vigila esta clase.
 */
class PepModuleDependenciesTests {
    private static final ApplicationModules MODULES = ApplicationModules.of(PepApplication.class);
    private static final String UNNAMED = "<<UNNAMED>>";

    @Test
    void the_pep_has_exactly_the_agreed_modules() {
        var names = MODULES.stream().map(module -> module.getIdentifier().toString()).collect(Collectors.toSet());

        assertThat(names).containsExactlyInAnyOrder("application", "commons", "enforcement", "ingress", "normalization");
    }

    @Test
    void application_is_open_and_the_other_modules_are_closed() {
        assertThat(module("application").isOpen()).isTrue();
        assertThat(module("commons").isOpen()).isFalse();
        assertThat(module("enforcement").isOpen()).isFalse();
        assertThat(module("normalization").isOpen()).isFalse();
        assertThat(module("ingress").isOpen()).isFalse();
    }

    @Test
    void each_module_declares_exactly_the_agreed_dependencies() {
        assertThat(declared("ingress")).containsExactlyInAnyOrder(
                "application", "commons",
                "enforcement", "enforcement :: usecase", "enforcement :: request",
                "normalization :: usecase", "normalization :: request");
        assertThat(declared("enforcement")).containsExactlyInAnyOrder("application", "commons");
        assertThat(declared("normalization")).containsExactlyInAnyOrder("application", "commons");
        assertThat(declared("application")).isEmpty();
        assertThat(declared("commons")).isEmpty();
    }

    @Test
    void application_and_commons_depend_on_no_other_module() {
        assertThat(module("application").getDirectDependencies(MODULES).isEmpty()).isTrue();
        assertThat(module("commons").getDirectDependencies(MODULES).isEmpty()).isTrue();
    }

    @Test
    void commons_is_a_flat_module_with_all_its_types_in_the_root_package() {
        var published = published("commons");

        assertThat(published).isNotEmpty();
        assertThat(published).allMatch(entry -> entry.startsWith(UNNAMED + " -> "));
    }

    @Test
    void enforcement_and_normalization_publish_only_their_use_case_and_their_request() {
        assertThat(published("enforcement")).containsExactlyInAnyOrder(
                UNNAMED + " -> ForwardHttpRequest",
                "usecase -> EnforceAccessUseCase",
                "request -> EnforceAccessRequest");
        assertThat(published("normalization")).containsExactlyInAnyOrder(
                "usecase -> NormalizeAccessUseCase",
                "request -> NormalizeAccessRequest");
    }

    private static ApplicationModule module(String name) {
        return MODULES.getModuleByName(name).orElseThrow();
    }

    private static Set<String> declared(String name) {
        return module(name).getAllowedDependencies(MODULES).stream()
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    private static Set<String> published(String name) {
        return module(name).getNamedInterfaces().stream()
                .flatMap(namedInterface -> namedInterface.asJavaClasses()
                        .map(type -> namedInterface.getName() + " -> " + type.getSimpleName()))
                .collect(Collectors.toSet());
    }
}
