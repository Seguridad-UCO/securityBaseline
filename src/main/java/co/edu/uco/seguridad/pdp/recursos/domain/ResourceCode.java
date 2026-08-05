package co.edu.uco.seguridad.pdp.recursos.domain;
public record ResourceCode(String value) { public ResourceCode { if (value == null || !value.matches("[a-z][a-z0-9-]{1,63}")) throw new IllegalArgumentException("resource code must be lowercase kebab-case"); } }
