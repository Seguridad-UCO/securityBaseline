package co.edu.uco.seguridad.pdp.recursos.domain;
public record ActionCode(String value) { public ActionCode { if (value == null || !value.matches("[a-z][a-z0-9-]{1,63}")) throw new IllegalArgumentException("action code must be lowercase kebab-case"); } }
