package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;
import java.util.Objects;
public record ReadApplicationSecuritySummaryRequest(AdministrationRequest administration){public ReadApplicationSecuritySummaryRequest{Objects.requireNonNull(administration);}}
