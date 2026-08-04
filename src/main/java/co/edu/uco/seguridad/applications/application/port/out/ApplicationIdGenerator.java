package co.edu.uco.seguridad.applications.application.port.out;
import co.edu.uco.seguridad.applications.domain.ProtectedApplicationId;
public interface ApplicationIdGenerator { ProtectedApplicationId next(); }
