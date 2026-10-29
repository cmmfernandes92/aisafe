package eapli.aisafe.aircraftmodelmanagement.domain;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import eapli.aisafe.enginemodelmanagement.domain.EngineModel;
import eapli.aisafe.enginemodelmanagement.domain.MotorizationType;
import eapli.aisafe.makermanagement.domain.Maker;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"model_name", "maker_name"}))
public class AircraftModel implements AggregateRoot<Long> {

    private static final long serialVersionUID = 1L;

    /**
     * Nominal aspect ratio used to derive a wing span when a caller does not
     * supply one (legacy/convenience constructor and registration overload).
     * 9.0 is a representative narrow-body value; production registration paths
     * should pass the real span.
     */
    private static final double DEFAULT_ASPECT_RATIO = 9.0;

    @Version
    private Long version;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Embedded
    private AircraftModelName modelName;

    @ManyToOne(optional = false)
    @JoinColumn(name = "maker_name", nullable = false)
    private Maker maker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AircraftType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MotorizationType motorizationType;

    @Column(nullable = false)
    private double emptyWeight;

    @Column(nullable = false)
    private double mtow;

    @Column(nullable = false)
    private double mzfw;

    @Column(nullable = false)
    private double maxFuelCapacity;

    @Column(nullable = false)
    private double serviceCeiling;

    @Column(nullable = false)
    private double cruiseSpeed;

    @Column(nullable = false)
    private double wingArea;

    /**
     * Wing span (m). Together with {@link #wingArea} it yields the aspect ratio
     * {@code AR = span²/area} that the §3.3 induced-drag term needs (US085). Added
     * in Sprint&nbsp;3: the model previously stored only wing area, which is not
     * enough to compute induced drag.
     */
    @Column(nullable = false)
    private double wingSpan;

    @Column(nullable = false)
    private double dragCoefficient;

    @Column(nullable = false)
    private double liftCoefficient;

    @Column(nullable = false)
    private double maximumRange;

    @ManyToMany
    @JoinTable(
        name = "aircraft_model_certified_engines",
        joinColumns = @JoinColumn(name = "aircraft_model_id"),
        inverseJoinColumns = @JoinColumn(name = "engine_model_id")
    )
    private Set<EngineModel> certifiedEngines = new HashSet<>();

    @Column(nullable = false)
    private int maxPassengerCapacity;

    /**
     * Full constructor (US085): includes the {@code wingSpan} needed to compute
     * the aspect ratio for the §3.3 induced-drag term.
     */
    public AircraftModel(final AircraftModelName modelName, final Maker maker,
            final AircraftType type, final MotorizationType motorizationType,
            final double emptyWeight, final double mtow, final double mzfw,
            final double maxFuelCapacity, final double serviceCeiling, final double cruiseSpeed,
            final double wingArea, final double wingSpan, final double dragCoefficient,
            final double liftCoefficient, final double maximumRange,
            final Set<EngineModel> initialEngines) {
        Preconditions.noneNull(modelName, maker, type, motorizationType);
        Preconditions.nonNull(initialEngines, "Certified engines must not be null");
        if (initialEngines.isEmpty()) {
            throw new IllegalArgumentException("At least one certified engine model must be provided");
        }
        validatePositive(emptyWeight, "emptyWeight");
        validatePositive(mtow, "mtow");
        validatePositive(mzfw, "mzfw");
        validatePositive(maxFuelCapacity, "maxFuelCapacity");
        validatePositive(serviceCeiling, "serviceCeiling");
        validatePositive(cruiseSpeed, "cruiseSpeed");
        validatePositive(wingArea, "wingArea");
        validatePositive(wingSpan, "wingSpan");
        validatePositive(dragCoefficient, "dragCoefficient");
        validatePositive(liftCoefficient, "liftCoefficient");
        validatePositive(maximumRange, "maximumRange");

        for (final EngineModel engine : initialEngines) {
            if (engine.motorizationType() != motorizationType) {
                throw new IllegalArgumentException(
                    "Engine " + engine.engineModelName() + " has incompatible motorization type");
            }
        }

        this.modelName = modelName;
        this.maker = maker;
        this.type = type;
        this.motorizationType = motorizationType;
        this.emptyWeight = emptyWeight;
        this.mtow = mtow;
        this.mzfw = mzfw;
        this.maxFuelCapacity = maxFuelCapacity;
        this.serviceCeiling = serviceCeiling;
        this.cruiseSpeed = cruiseSpeed;
        this.wingArea = wingArea;
        this.wingSpan = wingSpan;
        this.dragCoefficient = dragCoefficient;
        this.liftCoefficient = liftCoefficient;
        this.maximumRange = maximumRange;
        this.certifiedEngines.addAll(initialEngines);
    }

    /**
     * Legacy/convenience constructor without an explicit wing span: the span is
     * derived from {@link #DEFAULT_ASPECT_RATIO} so existing callers and tests
     * keep compiling. Prefer the full constructor in any path that has the real
     * span (registration, bootstrap), so the §3.3 aspect ratio is accurate.
     */
    public AircraftModel(final AircraftModelName modelName, final Maker maker,
            final AircraftType type, final MotorizationType motorizationType,
            final double emptyWeight, final double mtow, final double mzfw,
            final double maxFuelCapacity, final double serviceCeiling, final double cruiseSpeed,
            final double wingArea, final double dragCoefficient, final double liftCoefficient,
            final double maximumRange, final Set<EngineModel> initialEngines) {
        this(modelName, maker, type, motorizationType, emptyWeight, mtow, mzfw, maxFuelCapacity,
                serviceCeiling, cruiseSpeed, wingArea, Math.sqrt(DEFAULT_ASPECT_RATIO * wingArea),
                dragCoefficient, liftCoefficient, maximumRange, initialEngines);
    }

    protected AircraftModel() {
        // for ORM
    }

    public void addCertifiedEngine(final EngineModel engine) {
        Preconditions.nonNull(engine, "Engine model must not be null");
        if (engine.motorizationType() != this.motorizationType) {
            throw new IllegalArgumentException(
                "Engine motorization type is incompatible with this aircraft model");
        }
        if (certifiedEngines.stream().anyMatch(engine::sameAs)) {
            throw new IllegalArgumentException("Engine model is already certified for this aircraft model");
        }
        certifiedEngines.add(engine);
    }

    public void removeCertifiedEngine(final EngineModel engine) {
        Preconditions.nonNull(engine, "Engine model must not be null");

        final boolean exists = certifiedEngines.stream().anyMatch(engine::sameAs);
        if (!exists) {
            throw new IllegalArgumentException(
                    "Engine model " + engine.engineModelName() + " is not certified for this aircraft model");
        }

        if (certifiedEngines.size() <= 1) {
            throw new IllegalStateException(
                    "Cannot remove the only certified engine — an AircraftModel must retain at least one");
        }

        certifiedEngines.removeIf(engine::sameAs);
    }

    public AircraftModelName modelName() {
        return this.modelName;
    }

    public Maker maker() {
        return this.maker;
    }

    public AircraftType type() {
        return this.type;
    }

    public MotorizationType motorizationType() {
        return this.motorizationType;
    }

    public double emptyWeight() { return this.emptyWeight; }
    public double mtow() { return this.mtow; }
    public double mzfw() { return this.mzfw; }
    public double maxFuelCapacity() { return this.maxFuelCapacity; }
    public double serviceCeiling() { return this.serviceCeiling; }
    public double cruiseSpeed() { return this.cruiseSpeed; }
    public double wingArea() { return this.wingArea; }
    public double wingSpan() { return this.wingSpan; }

    /**
     * Aspect ratio {@code AR = span²/area} (spec §3.3), the geometry factor in the
     * induced-drag term {@code CL²/(π·AR·e)} used by the US085 flight-plan test.
     */
    public double aspectRatio() { return (this.wingSpan * this.wingSpan) / this.wingArea; }

    public double dragCoefficient() { return this.dragCoefficient; }
    public double liftCoefficient() { return this.liftCoefficient; }
    public double maximumRange() { return this.maximumRange; }

    public int maxPassengerCapacity() { return this.maxPassengerCapacity; }

    public Set<EngineModel> certifiedEngines() {
        return Collections.unmodifiableSet(this.certifiedEngines);
    }

    @Override
    public Long identity() {
        return this.id;
    }

    @Override
    public boolean sameAs(final Object other) {
        if (!(other instanceof AircraftModel)) return false;
        final AircraftModel that = (AircraftModel) other;
        return this.modelName.equals(that.modelName) && this.maker.equals(that.maker);
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
        return modelName + " (" + maker + ")";
    }

    private static void validatePositive(final double value, final String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be strictly positive");
        }
    }
}
