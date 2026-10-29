package co.edu.uco.seguridad.pdp.commons;

import co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.ProfileAssignmentRepository;
import co.edu.uco.seguridad.pdp.identity.application.secondaryport.repository.SecurityUserRepository;
import co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository.ProfileRepository;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.tenants.application.secondaryport.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import reactor.test.StepVerifier;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Los puertos secundarios traen capacidades opcionales como métodos {@code default}: un adaptador que
 * no las implementa debe fallar de forma explícita ({@link UnsupportedOperationException}) o devolver
 * un resultado vacío documentado, nunca otro error ni un valor inventado. El doble es un proxy sin
 * lógica: solo los {@code default} se ejecutan de verdad.
 */
class UnimplementedRepositoryCapabilitiesTests {

    private static final List<Class<?>> PORTS = List.of(
            ApplicationRepository.class, AssignmentRepository.class, ProfileAssignmentRepository.class,
            ProfileRepository.class, ProtectedResourceRepository.class, RoleRepository.class,
            SecurityUserRepository.class, TenantRepository.class);

    @Test
    void every_default_capability_fails_explicitly_or_completes_empty() {
        for (Class<?> port : PORTS) {
            Object proxy = unimplemented(port);
            List<Method> defaults = Arrays.stream(port.getMethods()).filter(Method::isDefault).toList();
            assertThat(defaults).as("métodos default de %s", port.getSimpleName()).isNotEmpty();
            for (Method method : defaults) {
                Object result = invoke(proxy, method);
                if (result instanceof Publisher<?> publisher) {
                    StepVerifier.create(Flux.from(publisher).materialize().collectList())
                            .assertNext(signals -> assertThat(signals)
                                    .as("%s.%s", port.getSimpleName(), method.getName())
                                    .allSatisfy(UnimplementedRepositoryCapabilitiesTests::isExplicitOrValue))
                            .verifyComplete();
                }
            }
        }
    }

    private static void isExplicitOrValue(Signal<?> signal) {
        if (signal.isOnError()) {
            assertThat(signal.getThrowable()).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    private static Object invoke(Object proxy, Method method) {
        Object[] args = Arrays.stream(method.getParameterTypes()).map(UnimplementedRepositoryCapabilitiesTests::zero).toArray();
        try {
            return method.invoke(proxy, args);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("No se pudo invocar " + method, error);
        }
    }

    private static Object unimplemented(Class<?> port) {
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, args);
            }
            Class<?> type = method.getReturnType();
            if (Flux.class.isAssignableFrom(type)) {
                return Flux.empty();
            }
            if (Mono.class.isAssignableFrom(type)) {
                return Mono.empty();
            }
            return zero(type);
        };
        return Proxy.newProxyInstance(port.getClassLoader(), new Class<?>[]{port}, handler);
    }

    private static Object zero(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == double.class) {
            return 0d;
        }
        return (byte) 0;
    }
}
