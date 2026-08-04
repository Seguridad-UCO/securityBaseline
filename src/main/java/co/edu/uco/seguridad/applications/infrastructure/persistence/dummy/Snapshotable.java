package co.edu.uco.seguridad.applications.infrastructure.persistence.dummy;

public interface Snapshotable { Object snapshot(); void restore(Object snapshot); }
