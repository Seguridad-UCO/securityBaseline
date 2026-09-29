package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow; import java.util.Objects;
public record ListApplicationProfileAssignmentsRequest(AdministrationRequest administration, PageWindow window) { public ListApplicationProfileAssignmentsRequest {Objects.requireNonNull(administration);Objects.requireNonNull(window);} }
