package eapli.aisafe.reporting.domain;

import java.io.Serializable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * One participant of a safety violation (US111.3): a single aircraft's surrogate
 * id and sidecar-resolved reference, plus its position and velocity vector at the
 * moment of the event.
 */
@Embeddable
public class InvolvedFlight implements ValueObject, Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "surrogate_id")
    private int surrogateId;

    @Column(name = "flight_ref")
    private String flightRef;

    @Column(name = "x")
    private double x;

    @Column(name = "y")
    private double y;

    @Column(name = "alt")
    private double alt;

    @Column(name = "vx")
    private double vx;

    @Column(name = "vy")
    private double vy;

    @Column(name = "vz")
    private double vz;

    public InvolvedFlight(final int surrogateId, final String flightRef,
            final double x, final double y, final double alt,
            final double vx, final double vy, final double vz) {
        Preconditions.nonEmpty(flightRef);
        this.surrogateId = surrogateId;
        this.flightRef = flightRef;
        this.x = x;
        this.y = y;
        this.alt = alt;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
    }

    protected InvolvedFlight() {
        // for ORM
    }

    public int surrogateId() {
        return this.surrogateId;
    }

    public String flightRef() {
        return this.flightRef;
    }

    public double x() {
        return this.x;
    }

    public double y() {
        return this.y;
    }

    public double alt() {
        return this.alt;
    }

    public double vx() {
        return this.vx;
    }

    public double vy() {
        return this.vy;
    }

    public double vz() {
        return this.vz;
    }

    /** This aircraft's position at the moment of the violation. */
    public Position position() {
        return new Position(x, y, alt);
    }
}