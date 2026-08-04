package co.edu.uco.seguridad.applications.application.port.out;

public record PageWindow(int offset, int limit) {
    public PageWindow {
        if (offset < 0) throw new IllegalArgumentException("offset must not be negative");
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("limit must be between 1 and 100");
    }
    public static PageWindow page(int page, int size) {
        if (page < 0) throw new IllegalArgumentException("page must not be negative");
        return new PageWindow(Math.multiplyExact(page, size), size);
    }
}
