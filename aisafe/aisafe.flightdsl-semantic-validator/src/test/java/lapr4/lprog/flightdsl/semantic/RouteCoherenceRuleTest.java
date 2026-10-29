package lapr4.lprog.flightdsl.semantic;

import lapr4.lprog.flightdsl.ast.Flight;
import lapr4.lprog.flightdsl.ast.FlightType;
import lapr4.lprog.flightdsl.ast.Position;
import lapr4.lprog.flightdsl.ast.Route;
import lapr4.lprog.flightdsl.semantic.rules.RouteCoherenceRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static lapr4.lprog.flightdsl.semantic.AstFixtures.airport;
import static lapr4.lprog.flightdsl.semantic.AstFixtures.fileOf;
import static lapr4.lprog.flightdsl.semantic.AstFixtures.simpleLeg;
import static lapr4.lprog.flightdsl.semantic.AstFixtures.validSingleLegFlight;
import static lapr4.lprog.flightdsl.semantic.AstFixtures.validTwoLegFlight;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("§3.4.5 — Route coherence")
class RouteCoherenceRuleTest {

    private final RouteCoherenceRule rule = new RouteCoherenceRule();

    @Test
    @DisplayName("Coherent route passes")
    void happyPath() {
        ErrorCollector c = new ErrorCollector();
        rule.check(fileOf(validSingleLegFlight("TP300")), c);
        assertTrue(c.errors().isEmpty(), () -> "Unexpected: " + c.errors());
    }

    @Test
    @DisplayName("Route origin must match the first leg's departure airport")
    void originMismatch() {
        Flight f = new Flight(
                "TP300", FlightType.REGULAR,
                new Route(airport("BCN"), airport("LIS"), Position.UNKNOWN), // origin BCN ≠ first dep OPO
                List.of(simpleLeg(1, "OPO", "2026-06-01T08:00", "LIS", "2026-06-01T09:00")),
                Position.UNKNOWN);
        ErrorCollector c = new ErrorCollector();
        rule.check(fileOf(f), c);
        assertTrue(c.errors().stream().anyMatch(e -> e.code() == SemanticErrorCode.ROUTE_ORIGIN_MISMATCH));
    }

    @Test
    @DisplayName("Route destination must match the last leg's arrival airport")
    void destinationMismatch() {
        Flight f = new Flight(
                "TP300", FlightType.REGULAR,
                new Route(airport("OPO"), airport("BCN"), Position.UNKNOWN), // destination BCN ≠ last arr LIS
                List.of(simpleLeg(1, "OPO", "2026-06-01T08:00", "LIS", "2026-06-01T09:00")),
                Position.UNKNOWN);
        ErrorCollector c = new ErrorCollector();
        rule.check(fileOf(f), c);
        assertTrue(c.errors().stream().anyMatch(e -> e.code() == SemanticErrorCode.ROUTE_DESTINATION_MISMATCH));
    }

    @Test
    @DisplayName("Two-leg flight with valid intermediate airport passes (OPO -> MAD -> LIS)")
    void twoLegCoherent() {
        ErrorCollector c = new ErrorCollector();
        rule.check(fileOf(validTwoLegFlight("TP301")), c);
        assertTrue(c.errors().isEmpty(), () -> "Unexpected: " + c.errors());
    }

    @Test
    @DisplayName("Airport repeated in the route is flagged with ROUTE_AIRPORT_REPEATED")
    void airportRepeated() {
        // OPO -> LIS -> OPO  (route origin/destination OPO, intermediate LIS, returns to OPO)
        Flight f = new Flight(
                "TP301", FlightType.REGULAR,
                new Route(airport("OPO"), airport("OPO"), Position.UNKNOWN),
                List.of(
                        simpleLeg(1, "OPO", "2026-06-01T08:00", "LIS", "2026-06-01T09:00"),
                        simpleLeg(2, "LIS", "2026-06-01T10:00", "OPO", "2026-06-01T11:00")),
                Position.UNKNOWN);
        ErrorCollector c = new ErrorCollector();
        rule.check(fileOf(f), c);
        assertTrue(c.errors().stream().anyMatch(e -> e.code() == SemanticErrorCode.ROUTE_AIRPORT_REPEATED),
                "Repeated OPO should be flagged. Findings: " + c.errors());
    }

    @Test
    @DisplayName("Intermediate airport repeated in the middle is flagged")
    void intermediateRepeated() {
        // OPO -> MAD -> MAD -> LIS (the middle airport visits MAD twice in a row)
        Flight f = new Flight(
                "TP302", FlightType.REGULAR,
                new Route(airport("OPO"), airport("LIS"), Position.UNKNOWN),
                List.of(
                        simpleLeg(1, "OPO", "2026-06-01T08:00", "MAD", "2026-06-01T10:00"),
                        simpleLeg(2, "MAD", "2026-06-01T11:30", "MAD", "2026-06-01T13:00"),
                        simpleLeg(3, "MAD", "2026-06-01T14:30", "LIS", "2026-06-01T15:30")),
                Position.UNKNOWN);
        ErrorCollector c = new ErrorCollector();
        rule.check(fileOf(f), c);
        assertTrue(c.errors().stream().anyMatch(e -> e.code() == SemanticErrorCode.ROUTE_AIRPORT_REPEATED));
    }
}
