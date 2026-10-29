package eapli.aisafe.flightmanagement.domain;

/**
 * Lifecycle of a {@link FlightPlan} inside a {@link Flight}.
 * US080 creates a plan in {@code DRAFT}; US121 promotes it to {@code VALIDATED};
 * US085 promotes it to {@code TESTED}.
 */
public enum FlightPlanStatus {
    DRAFT,
    VALIDATED,
    TESTED
}
