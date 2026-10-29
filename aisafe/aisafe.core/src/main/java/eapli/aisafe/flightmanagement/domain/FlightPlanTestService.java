package eapli.aisafe.flightmanagement.domain;

/**
 * Domain service that tests a FlightPlan in the context of its owning Flight
 * before the aggregate promotes it to TESTED (US085).
 *
 * The basic implementation is intentionally minimal. The full Simulator v1
 * physics model can replace it later.
 */
@FunctionalInterface
public interface FlightPlanTestService {
    FlightPlanTestResult test(Flight flight, FlightPlan plan);
}