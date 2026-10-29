package lapr4.lprog.flightdsl.semantic;

import lapr4.lprog.flightdsl.ast.Flight;
import lapr4.lprog.flightdsl.ast.FlightType;
import lapr4.lprog.flightdsl.ast.Position;
import lapr4.lprog.flightdsl.ast.Route;
import lapr4.lprog.flightdsl.semantic.rules.LegSequenceRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static lapr4.lprog.flightdsl.semantic.AstFixtures.airport;
import static lapr4.lprog.flightdsl.semantic.AstFixtures.fileOf;
import static lapr4.lprog.flightdsl.semantic.AstFixtures.simpleLeg;
import static lapr4.lprog.flightdsl.semantic.AstFixtures.validTwoLegFlight;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("§3.4.5 — Leg sequence coherence")
class LegSequenceRuleTest {

    private final LegSequenceRule rule = new LegSequenceRule();

    @Test
    @DisplayName("Coherent two-leg flight passes")
    void twoLegsHappyPath() {
        ErrorCollector c = new ErrorCollector();
        rule.check(fileOf(validTwoLegFlight("TP200")), c);
        assertTrue(c.errors().isEmpty(), () -> "Unexpected: " + c.errors());
    }

    @Test
    @DisplayName("Arrival airport of leg n must match departure airport of leg n+1")
    void airportMismatch() {
        Flight f = new Flight(
                "TP200", FlightType.REGULAR,
                new Route(airport("OPO"), airport("LIS"), Position.UNKNOWN),
                List.of(
                        simpleLeg(1, "OPO", "2026-06-01T08:00", "MAD", "2026-06-01T10:00"),
                        // BCN, not MAD — discontinuity
                        simpleLeg(2, "BCN", "2026-06-01T11:30", "LIS", "2026-06-01T12:30")),
                Position.UNKNOWN);
        ErrorCollector c = new ErrorCollector();
        rule.check(fileOf(f), c);
        assertTrue(c.errors().stream()
                .anyMatch(e -> e.code() == SemanticErrorCode.LEG_SEQUENCE_AIRPORT_MISMATCH));
    }

    @Test
    @DisplayName("Arrival time of leg n must strictly precede departure time of leg n+1")
    void timeNotOrdered() {
        Flight f = new Flight(
                "TP200", FlightType.REGULAR,
                new Route(airport("OPO"), airport("LIS"), Position.UNKNOWN),
                List.of(
                        simpleLeg(1, "OPO", "2026-06-01T08:00", "MAD", "2026-06-01T10:00"),
                        // departure of leg 2 is BEFORE arrival of leg 1
                        simpleLeg(2, "MAD", "2026-06-01T09:30", "LIS", "2026-06-01T11:00")),
                Position.UNKNOWN);
        ErrorCollector c = new ErrorCollector();
        rule.check(fileOf(f), c);
        assertTrue(c.errors().stream()
                .anyMatch(e -> e.code() == SemanticErrorCode.LEG_SEQUENCE_TIME_NOT_ORDERED));
    }

    @Test
    @DisplayName("Equal arrival and next-departure times are flagged (strict precedence)")
    void timesEqual() {
        Flight f = new Flight(
                "TP200", FlightType.REGULAR,
                new Route(airport("OPO"), airport("LIS"), Position.UNKNOWN),
                List.of(
                        simpleLeg(1, "OPO", "2026-06-01T08:00", "MAD", "2026-06-01T10:00"),
                        simpleLeg(2, "MAD", "2026-06-01T10:00", "LIS", "2026-06-01T11:00")),
                Position.UNKNOWN);
        ErrorCollector c = new ErrorCollector();
        rule.check(fileOf(f), c);
        assertTrue(c.errors().stream()
                .anyMatch(e -> e.code() == SemanticErrorCode.LEG_SEQUENCE_TIME_NOT_ORDERED));
    }

    @Test
    @DisplayName("Single-leg flight skips sequence checks (no pair to compare)")
    void singleLegNoChecks() {
        ErrorCollector c = new ErrorCollector();
        rule.check(fileOf(simpleFlight("TP200")), c);
        assertTrue(c.errors().isEmpty());
    }

    private static Flight simpleFlight(String id) {
        return new Flight(
                id, FlightType.REGULAR,
                new Route(airport("OPO"), airport("LIS"), Position.UNKNOWN),
                List.of(simpleLeg(1, "OPO", "2026-06-01T08:00", "LIS", "2026-06-01T09:00")),
                Position.UNKNOWN);
    }
}