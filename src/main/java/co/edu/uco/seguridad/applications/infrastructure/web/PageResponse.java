package co.edu.uco.seguridad.applications.infrastructure.web;
import java.util.List;
public record PageResponse<T>(List<T> content, long total, int offset, int limit) { }
