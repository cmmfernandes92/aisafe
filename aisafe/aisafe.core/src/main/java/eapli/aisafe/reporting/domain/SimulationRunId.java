package eapli.aisafe.reporting.domain;

import java.io.Serializable;
import java.util.UUID;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Surrogate identity of a {@link SimulationRun}. A run has no natural key
 * (several runs can share area, scenario and timestamp), so a UUID is minted.
 */
@Embeddable
public class SimulationRunId implements ValueObject, Comparable<SimulationRunId>, Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "simulation_run_id", nullable = false, updatable = false)
    private UUID id;

    public static SimulationRunId newId() {
        final SimulationRunId rid = new SimulationRunId();
        rid.id = UUID.randomUUID();
        return rid;
    }

    public static SimulationRunId valueOf(final String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Simulation run id must not be empty");
        }
        final SimulationRunId rid = new SimulationRunId();
        rid.id = UUID.fromString(id);
        return rid;
    }

    protected SimulationRunId() {
        // for ORM
    }

    public UUID id() {
        return this.id;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SimulationRunId)) {
            return false;
        }
        return this.id.equals(((SimulationRunId) o).id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public int compareTo(final SimulationRunId other) {
        return this.id.compareTo(other.id);
    }

    @Override
    public String toString() {
        return this.id.toString();
    }
}