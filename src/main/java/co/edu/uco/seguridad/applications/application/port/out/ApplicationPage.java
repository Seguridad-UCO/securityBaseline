package co.edu.uco.seguridad.applications.application.port.out;

import java.util.List;

public record ApplicationPage<T>(List<T> content, long total, int offset, int limit) {
    public ApplicationPage { content = List.copyOf(content); }
}
