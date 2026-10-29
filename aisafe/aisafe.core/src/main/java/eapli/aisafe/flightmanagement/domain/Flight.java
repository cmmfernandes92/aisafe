package eapli.aisafe.flightmanagement.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Version;

import eapli.aisafe.fleetmanagement.domain.Registration;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;
import java.util.Map;
import eapli.aisafe.weather.domain.WeatherDataId;

/**
 * A flight for a route on a given day. Aggregate root of US080: it owns its set
 * of {@link FlightPlan}s and is identified by route + departure date
 * ({@link FlightId}), which makes "one flight per route per day" (I4) structural.
 *
 * <p>On creation it holds a single {@code DRAFT} plan (I1). The cross-plan
 * invariant "at most one VALIDATED plan at a time" is enforced here.</p>
 */
@Entity
public class Flight implements AggregateRoot<FlightId> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    private FlightId id;

    @Embedded
    @AttributeOverride(name = "email", column = @Column(name = "pilot_email", nullable = false))
    private Email pilot;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "aircraft_registration", nullable = false))
    private Registration aircraft;

    @Column(name = "departure_date_time", nullable = false)
    private LocalDateTime departure;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "plan_index")
    private List<FlightPlan> plans = new ArrayList<>();

    public Flight(final RouteName route, final Email pilot, final Registration aircraft,
            final LocalDateTime departure) {
        Preconditions.noneNull(route, pilot, aircraft, departure);
        this.id = new FlightId(route, departure.toLocalDate());
        this.pilot = pilot;
        this.aircraft = aircraft;
        this.departure = departure;
        this.plans.add(new FlightPlan()); // initial DRAFT plan (I1)
    }

    protected Flight() {
        // for ORM
    }

    @Override
    public FlightId identity() {
        return this.id;
    }

    public RouteName route() {
        return this.id.routeName();
    }

    public Email pilot() {
        return this.pilot;
    }

    public Registration aircraft() {
        return this.aircraft;
    }

    public LocalDateTime departure() {
        return this.departure;
    }

    public int numberOfPlans() {
        return this.plans.size();
    }

    /** The most recently added plan. */
    public FlightPlan currentPlan() {
        return this.plans.get(this.plans.size() - 1);
    }

    public boolean hasValidatedPlan() {
        return this.plans.stream().anyMatch(FlightPlan::isValidated);
    }

    /** Read-only view of this flight's plans, oldest first. */
    java.util.List<FlightPlan> plans() {
        return java.util.Collections.unmodifiableList(this.plans);
    }

    /** Adds a new {@code DRAFT} plan, which becomes the current plan. */
    public void addDraftPlan() {
        this.plans.add(new FlightPlan());
    }

    /**
     * Promotes the current plan to {@code VALIDATED}, enforcing the invariant
     * that a flight has at most one VALIDATED plan at a time.
     */
    public void validateCurrentPlan() {
        ensureNoValidatedPlan();
        currentPlan().markValidated();
    }

    /**
     * Imports validated DSL content as the flight's single {@code VALIDATED} plan
     * (US121). The first import lands on US080's empty draft. On a <em>re-import</em>
     * (the flight already has a VALIDATED plan), that plan is demoted back to
     * {@code DRAFT} — keeping its body as a superseded revision — and the new
     * content is imported into a fresh plan. So the newest import is always the
     * one VALIDATED plan (invariant: at most one VALIDATED at a time) and the
     * previously-validated plan moves to DRAFT.
     *
     * <p>Content is validated before any mutation, so a rejected import leaves the
     * flight untouched.</p>
     */
    public void importDsl(final String rawDslContent, final String importedBy) {
        Preconditions.nonEmpty(rawDslContent, "Flight plan content must not be empty");
        demoteValidatedPlanIfAny();
        // First import reuses US080's empty draft; a re-import gets a fresh plan so
        // the demoted revision keeps its own body.
        if (!currentPlan().isDraft() || currentPlan().hasContent()) {
            addDraftPlan();
        }
        currentPlan().supersede(rawDslContent, importedBy);
        currentPlan().markValidated();
    }

    /**
     * Tests the current plan using the provided service (US085). If the test
     * passes, the plan is promoted {@code VALIDATED -> TESTED}; on failure the
     * plan state is unchanged and the failure result is returned.
     *
     * <p>Throws {@link IllegalStateException} if the flight has no current plan
     * or if the current plan is not in {@code VALIDATED} state, so the test
     * service is never invoked on a plan that cannot be promoted.</p>
     */
    public FlightPlanTestResult testCurrentPlan(final FlightPlanTestService testService) {
        Preconditions.nonNull(testService, "Test service must not be null");
        final FlightPlan plan = currentPlan();
        if (plan == null) {
            throw new IllegalStateException("Flight has no current flight plan");
        }
        if (!plan.isValidated()) {
            throw new IllegalStateException(
                    "Current plan is not VALIDATED; only a VALIDATED plan can be tested");
        }
        final FlightPlanTestResult result = testService.test(this, plan);
        if (result.isSuccess()) {
            plan.markTested();
        }
        return result;
    }


    public void attachWeatherToCurrentPlan(
            final Map<SegmentKey, WeatherDataId> weatherMap) {

        currentPlan().attachWeather(weatherMap);
    }
    

    private void demoteValidatedPlanIfAny() {
        this.plans.stream().filter(FlightPlan::isValidated).forEach(FlightPlan::demoteToDraft);
    }

    private void ensureNoValidatedPlan() {
        if (hasValidatedPlan()) {
            throw new IllegalStateException("Flight already has a VALIDATED plan");
        }
    }

    @Override
    public boolean sameAs(final Object other) {
        return DomainEntities.areEqual(this, other);
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
        return "Flight[" + this.id + ", pilot=" + this.pilot + ", aircraft=" + this.aircraft + "]";
    }
}