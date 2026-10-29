package eapli.aisafe.reporting.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Aggregate root for one ingested simulation run (US111). Assembled from a parsed
 * C report and stamped with the air control area of the Flight Control Operator
 * who generated it. This is the persisted record US112 aggregates across a month.
 *
 * <p>Invariants enforced in the constructor:
 * <ul>
 *   <li>the number of execution statuses equals the reported total flight count;</li>
 *   <li>the PASS/FAIL verdict mirrors the report's overall validation result.</li>
 * </ul>
 */
@Entity
@Table(name = "SimulationRun")
public class SimulationRun implements AggregateRoot<SimulationRunId> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    private SimulationRunId id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "area_code", nullable = false))
    private AreaCode areaCode;

    @Column(name = "run_timestamp", nullable = false)
    private LocalDateTime runTimestamp;

    @Column(name = "scenario")
    private String scenario;

    @Column(name = "total_flights")
    private int totalFlights;

    @Column(name = "seconds_per_step")
    private double secondsPerStep;

    @Column(name = "end_reason")
    private String endReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_outcome", nullable = false)
    private ValidationOutcome validationOutcome;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "SimulationRunFlightStatus", joinColumns = @JoinColumn(name = "run_id"))
    private List<FlightExecutionStatus> flightStatuses;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "SimulationRunViolation", joinColumns = @JoinColumn(name = "run_id"))
    private List<SafetyViolation> violations;

    // CHECKSTYLE:OFF a simulation run aggregates the full parsed report in one write
    public SimulationRun(final AreaCode areaCode, final LocalDateTime runTimestamp,
            final String scenario, final int totalFlights, final double secondsPerStep,
            final String endReason, final ValidationOutcome validationOutcome,
            final List<FlightExecutionStatus> flightStatuses,
            final List<SafetyViolation> violations) {
        // CHECKSTYLE:ON
        Preconditions.noneNull(areaCode, runTimestamp, validationOutcome, flightStatuses, violations);
        if (totalFlights < 0) {
            throw new IllegalArgumentException("Total flights must not be negative");
        }
        // Invariant (US111.2): one execution status per reported flight.
        if (flightStatuses.size() != totalFlights) {
            throw new IllegalStateException("Execution status count (" + flightStatuses.size()
                    + ") does not match the reported total flights (" + totalFlights + ")");
        }
        this.id = SimulationRunId.newId();
        this.areaCode = areaCode;
        this.runTimestamp = runTimestamp;
        this.scenario = scenario;
        this.totalFlights = totalFlights;
        this.secondsPerStep = secondsPerStep;
        this.endReason = endReason;
        this.validationOutcome = validationOutcome;
        this.flightStatuses = new ArrayList<>(flightStatuses);
        this.violations = new ArrayList<>(violations);
    }

    protected SimulationRun() {
        // for ORM
    }

    @Override
    public SimulationRunId identity() {
        return this.id;
    }

    public AreaCode areaCode() {
        return this.areaCode;
    }

    public LocalDateTime runTimestamp() {
        return this.runTimestamp;
    }

    public String scenario() {
        return this.scenario;
    }

    public int totalFlights() {
        return this.totalFlights;
    }

    public double secondsPerStep() {
        return this.secondsPerStep;
    }

    public String endReason() {
        return this.endReason;
    }

    public ValidationOutcome validationOutcome() {
        return this.validationOutcome;
    }

    public List<FlightExecutionStatus> flightStatuses() {
        return Collections.unmodifiableList(this.flightStatuses);
    }

    public List<SafetyViolation> violations() {
        return Collections.unmodifiableList(this.violations);
    }

    @Override
    public boolean equals(final Object o) {
        return DomainEntities.areEqual(this, o);
    }

    @Override
    public int hashCode() {
        return DomainEntities.hashCode(this);
    }

    @Override
    public boolean sameAs(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public String toString() {
        return "SimulationRun[" + areaCode + " " + scenario + " @" + runTimestamp
                + " -> " + validationOutcome + "]";
    }
}