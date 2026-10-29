package eapli.aisafe.reporting.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * A safety violation event (US111.3). One {@code [violations]} row carries the
 * simulation {@code step} plus, for each involved aircraft, a position and
 * velocity vector. A violation involves either ONE flight (an altitude
 * ceiling/floor bust) or TWO flights (a loss of separation). The wall-clock
 * {@code timestamp} is derived by the assembler as
 * {@code run_timestamp + step × seconds_per_step}.
 */
@Embeddable
public class SafetyViolation implements ValueObject, Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "step")
    private int step;

    @Column(name = "violation_timestamp")
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "violation_kind")
    private ViolationKind kind;

    @Column(name = "horizontal_distance")
    private double horizontalDistance;

    @Column(name = "vertical_distance")
    private double verticalDistance;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "surrogateId", column = @Column(name = "surrogate_id_1")),
        @AttributeOverride(name = "flightRef", column = @Column(name = "flight_ref_1")),
        @AttributeOverride(name = "x", column = @Column(name = "x1")),
        @AttributeOverride(name = "y", column = @Column(name = "y1")),
        @AttributeOverride(name = "alt", column = @Column(name = "alt1")),
        @AttributeOverride(name = "vx", column = @Column(name = "vx1")),
        @AttributeOverride(name = "vy", column = @Column(name = "vy1")),
        @AttributeOverride(name = "vz", column = @Column(name = "vz1"))
    })
    private InvolvedFlight first;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "surrogateId", column = @Column(name = "surrogate_id_2")),
        @AttributeOverride(name = "flightRef", column = @Column(name = "flight_ref_2")),
        @AttributeOverride(name = "x", column = @Column(name = "x2")),
        @AttributeOverride(name = "y", column = @Column(name = "y2")),
        @AttributeOverride(name = "alt", column = @Column(name = "alt2")),
        @AttributeOverride(name = "vx", column = @Column(name = "vx2")),
        @AttributeOverride(name = "vy", column = @Column(name = "vy2")),
        @AttributeOverride(name = "vz", column = @Column(name = "vz2"))
    })
    private InvolvedFlight second;

    private SafetyViolation(final int step, final LocalDateTime timestamp, final ViolationKind kind,
            final InvolvedFlight first, final InvolvedFlight second,
            final double horizontalDistance, final double verticalDistance) {
        this.step = step;
        this.timestamp = timestamp;
        this.kind = kind;
        this.first = first;
        this.second = second;
        this.horizontalDistance = horizontalDistance;
        this.verticalDistance = verticalDistance;
    }

    protected SafetyViolation() {
        // for ORM
    }

    /** A loss-of-separation event between two distinct flights. */
    public static SafetyViolation separation(final int step, final LocalDateTime timestamp,
            final InvolvedFlight first, final InvolvedFlight second,
            final double horizontalDistance, final double verticalDistance) {
        Preconditions.noneNull(timestamp, first, second);
        if (step < 0) {
            throw new IllegalArgumentException("Violation step must not be negative");
        }
        if (first.surrogateId() == second.surrogateId()) {
            throw new IllegalArgumentException("a separation involves two distinct flights");
        }
        return new SafetyViolation(step, timestamp, ViolationKind.SEPARATION,
                first, second, horizontalDistance, verticalDistance);
    }

    /** An altitude ceiling/floor bust by a single flight, {@code metresPastBound} past the breached bound. */
    public static SafetyViolation altitude(final int step, final LocalDateTime timestamp,
            final ViolationKind kind, final InvolvedFlight flight, final double metresPastBound) {
        Preconditions.noneNull(timestamp, kind, flight);
        if (!kind.isAltitude()) {
            throw new IllegalArgumentException("an altitude violation requires an altitude kind");
        }
        if (step < 0) {
            throw new IllegalArgumentException("Violation step must not be negative");
        }
        return new SafetyViolation(step, timestamp, kind, flight, null, 0.0, metresPastBound);
    }

    public int step() {
        return this.step;
    }

    public LocalDateTime timestamp() {
        return this.timestamp;
    }

    public ViolationKind kind() {
        return this.kind;
    }

    public double horizontalDistance() {
        return this.horizontalDistance;
    }

    /**
     * For a SEPARATION, the inter-aircraft vertical separation (m); for an altitude
     * violation, the metres past the breached bound.
     */
    public double verticalDistance() {
        return this.verticalDistance;
    }

    /** The flights involved in this violation: one for an altitude bust, two for a loss of separation. */
    public List<InvolvedFlight> involvedFlights() {
        if (kind != null && kind.isAltitude()) {
            return List.of(first);
        }
        return second == null ? List.of(first) : List.of(first, second);
    }

    /** The position of each involved aircraft at the moment of the violation. */
    public List<Position> positions() {
        return involvedFlights().stream().map(InvolvedFlight::position).toList();
    }

    @Override
    public String toString() {
        return "step " + step + " @" + timestamp + ": " + kind;
    }
}