package eapli.aisafe.enginemodelmanagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import eapli.aisafe.makermanagement.domain.Maker;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"engine_model_name", "maker_name"}))
public class EngineModel implements AggregateRoot<Long> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Embedded
    private EngineModelName engineModelName;

    @ManyToOne(optional = false)
    @JoinColumn(name = "maker_name", nullable = false)
    private Maker maker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MotorizationType motorizationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FuelType fuelType;

    /**
     * Maximum (sea-level, static) thrust in N — the first of the two thrust
     * points the §3.3 model uses.
     */
    @Column(nullable = false)
    private double power;

    /**
     * Thrust at cruise/maximum speed in N — the second §3.3 thrust point (US085).
     * Thrust is assumed linear between {@link #power} (static) and this value, so
     * climb thrust can be interpolated. Added in Sprint&nbsp;3: the model used to
     * store a single thrust value, which is not enough to derive the thrust-vs-speed
     * slope.
     */
    @Column(nullable = false)
    private double cruiseThrust;

    @Column(nullable = false)
    private double efficiency;

    /**
     * Default ratio of cruise thrust to static thrust used when a caller does not
     * supply a cruise-thrust value (legacy/convenience constructor). Turbofan
     * cruise thrust is a small fraction of static thrust (~20%).
     */
    private static final double DEFAULT_CRUISE_THRUST_FRACTION = 0.2;

    /**
     * Full constructor (US085): includes the cruise-thrust point so the §3.3
     * climb-thrust interpolation can be derived.
     */
    public EngineModel(final EngineModelName engineModelName, final Maker maker,
            final MotorizationType motorizationType, final FuelType fuelType,
            final double power, final double cruiseThrust, final double efficiency) {
        Preconditions.noneNull(engineModelName, maker, motorizationType, fuelType);
        if (power <= 0) {
            throw new IllegalArgumentException("Power must be strictly positive");
        }
        if (cruiseThrust <= 0) {
            throw new IllegalArgumentException("Cruise thrust must be strictly positive");
        }
        if (cruiseThrust > power) {
            throw new IllegalArgumentException("Cruise thrust cannot exceed static (max) thrust");
        }
        if (efficiency <= 0) {
            throw new IllegalArgumentException("Efficiency must be strictly positive");
        }
        this.engineModelName = engineModelName;
        this.maker = maker;
        this.motorizationType = motorizationType;
        this.fuelType = fuelType;
        this.power = power;
        this.cruiseThrust = cruiseThrust;
        this.efficiency = efficiency;
    }

    /**
     * Legacy/convenience constructor without an explicit cruise-thrust point: it
     * is derived from {@link #DEFAULT_CRUISE_THRUST_FRACTION} so existing callers
     * and tests keep compiling. Prefer the full constructor where the real cruise
     * thrust is known (registration, bootstrap).
     */
    public EngineModel(final EngineModelName engineModelName, final Maker maker,
            final MotorizationType motorizationType, final FuelType fuelType,
            final double power, final double efficiency) {
        this(engineModelName, maker, motorizationType, fuelType, power,
                power * DEFAULT_CRUISE_THRUST_FRACTION, efficiency);
    }

    protected EngineModel() {
        // for ORM
    }

    public EngineModelName engineModelName() {
        return this.engineModelName;
    }

    public Maker maker() {
        return this.maker;
    }

    public MotorizationType motorizationType() {
        return this.motorizationType;
    }

    public FuelType fuelType() {
        return this.fuelType;
    }

    public double power() {
        return this.power;
    }

    /** Thrust at cruise/maximum speed in N — the second §3.3 thrust point (US085). */
    public double cruiseThrust() {
        return this.cruiseThrust;
    }

    public double efficiency() {
        return this.efficiency;
    }

    @Override
    public Long identity() {
        return this.id;
    }

    @Override
    public boolean sameAs(final Object other) {
        if (!(other instanceof EngineModel)) return false;
        final EngineModel that = (EngineModel) other;
        return this.engineModelName.equals(that.engineModelName) && this.maker.equals(that.maker);
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
    public String toString() {
        return engineModelName + " (" + maker + ")";
    }
}
