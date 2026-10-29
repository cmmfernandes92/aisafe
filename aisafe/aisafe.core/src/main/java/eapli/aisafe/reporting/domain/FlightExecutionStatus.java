package eapli.aisafe.reporting.domain;

import java.io.Serializable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * One flight's execution result within a {@link SimulationRun} (US111.2):
 * the surrogate id the C engine used, the resolved domain flight reference
 * (route@date from the {@code flights.csv} sidecar, or {@code #id} when the run
 * folder carried no mapping), the {@link FlightOutcome}, and the raw C exit code.
 */
@Embeddable
public class FlightExecutionStatus implements ValueObject, Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "surrogate_id")
    private int surrogateId;

    @Column(name = "flight_ref")
    private String flightRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome")
    private FlightOutcome outcome;

    @Column(name = "exit_code")
    private int exitCode;

    public FlightExecutionStatus(final int surrogateId, final String flightRef,
            final FlightOutcome outcome, final int exitCode) {
        Preconditions.nonNull(outcome);
        Preconditions.nonEmpty(flightRef);
        this.surrogateId = surrogateId;
        this.flightRef = flightRef;
        this.outcome = outcome;
        this.exitCode = exitCode;
    }

    protected FlightExecutionStatus() {
        // for ORM
    }

    public int surrogateId() {
        return this.surrogateId;
    }

    public String flightRef() {
        return this.flightRef;
    }

    public FlightOutcome outcome() {
        return this.outcome;
    }

    public int exitCode() {
        return this.exitCode;
    }

    @Override
    public String toString() {
        return flightRef + " -> " + outcome + " (exit " + exitCode + ")";
    }
}