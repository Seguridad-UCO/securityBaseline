package co.edu.uco.seguridad.applications.application.port.in;

import co.edu.uco.seguridad.applications.application.port.out.ApplicationPage;
import co.edu.uco.seguridad.applications.application.port.out.PageWindow;
import co.edu.uco.seguridad.applications.domain.ProtectedApplication;
import co.edu.uco.seguridad.applications.domain.ProtectedApplicationCriteria;
import reactor.core.publisher.Mono;

public interface SearchProtectedApplicationsUseCase {
    Mono<ApplicationPage<ProtectedApplication>> search(ProtectedApplicationCriteria criteria, PageWindow window);
}
