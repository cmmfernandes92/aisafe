package lapr4.lprog.flightdsl.semantic;

import lapr4.lprog.flightdsl.ast.Flight;
import lapr4.lprog.flightdsl.ast.FlightPlanFile;
import lapr4.lprog.flightdsl.ast.FlightType;
import lapr4.lprog.flightdsl.ast.Fuel;
import lapr4.lprog.flightdsl.ast.FuelUnit;
import lapr4.lprog.flightdsl.ast.Leg;
import lapr4.lprog.flightdsl.ast.Position;
import lapr4.lprog.flightdsl.ast.Route;
import lapr4.lprog.flightdsl.semantic.rules.LegLevelRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static lapr4.lprog.flightdsl.semantic.AstFixtures.airport;
import static lapr4.lprog.flightdsl.semantic.AstFixtures.arrival;
import static lapr4.lprog.flightdsl.semantic.AstFixtures.coord;
import static lapr4.lprog.flightdsl.semantic.AstFixtures.departure;
import static lapr4.lprog.flightdsl.semantic.AstFixtures.fileOf;
import static lapr4.lprog.flightdsl.semantic.AstFixtures.segment;
import static lapr4.lprog.flightdsl.semantic.AstFixtures.validSingleLegFlight;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("§3.4.5 — Leg-level rules")
class LegLevelRuleTest {

    private final LegLevelRule rule = new LegLevelRule();

    @Test
    @DisplayName("Valid leg passes")
    void validLeg() {
        ErrorCollector c = new ErrorCollector();
        rule.check(fileOf(validSingleLegFlight("TP100")), c);
        assertTrue(c.errors().isEmpty(), () -> "Unexpected: " + c.errors());
    }

    @Test
    @DisplayName("Flight without legs is flagged with FLIGHT_MISSING_LEGS")
    void flightWithoutLegs() {
        Flight f = new Flight(
                "TP100",
                FlightType.REGULAR,
                new Route(airport("OPO"), airport("LIS"), Position.UNKNOWN),
                List.of(),
                Position.UNKNOWN);
        ErrorCollector c = new ErrorCollector();
        rule.check(fileOf(f), c);
        assertEquals(1, c.errors().stream()
                .filter(e -> e.code() == SemanticErrorCode.FLIGHT_MISSING_LEGS).count());
    }

    @Test
    @DisplayName("Leg without departure is flagged with LEG_MISSING_DEPARTURE")
    void missingDeparture() {
        Leg badLeg = new Leg(
                1, null, arrival("LIS", "2026-06-01T09:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN),
                List.of(segment(coord(0, 0), coord(1, 1))),
                Position.UNKNOWN);
        Flight f = wrap(badLeg);
        ErrorCollector c = new ErrorCollector();
        rule.check(fileOf(f), c);
        assertTrue(c.errors().stream().anyMatch(e -> e.code() == SemanticErrorCode.LEG_MISSING_DEPARTURE));
    }

    @Test
    @DisplayName("Leg without arrival is flagged with LEG_MISSING_ARRIVAL")
    void missingArrival() {
        Leg badLeg = new Leg(
                1, departure("OPO", "2026-06-01T08:00"), null,
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN),
                List.of(segment(coord(0, 0), coord(1, 1))),
                Position.UNKNOWN);
        Flight f = wrap(badLeg);
        ErrorCollector c = new ErrorCollector();
        rule.check(fileOf(f), c);
        assertTrue(c.errors().stream().anyMatch(e -> e.code() == SemanticErrorCode.LEG_MISSING_ARRIVAL));
    }

    @Test
    @DisplayName("Leg without segments is flagged with LEG_MISSING_SEGMENTS")
    void missingSegments() {
        Leg badLeg = new Leg(
                1, departure("OPO", "2026-06-01T08:00"), arrival("LIS", "2026-06-01T09:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN),
                List.of(),
                Position.UNKNOWN);
        Flight f = wrap(badLeg);
        ErrorCollector c = new ErrorCollector();
        rule.check(fileOf(f), c);
        assertTrue(c.errors().stream().anyMatch(e -> e.code() == SemanticErrorCode.LEG_MISSING_SEGMENTS));
    }

    @Test
    @DisplayName("Leg without fuel block is flagged with LEG_MISSING_FUEL")
    void missingFuel() {
        Leg badLeg = new Leg(
                1, departure("OPO", "2026-06-01T08:00"), arrival("LIS", "2026-06-01T09:00"),
                null,
                List.of(segment(coord(0, 0), coord(1, 1))),
                Position.UNKNOWN);
        Flight f = wrap(badLeg);
        ErrorCollector c = new ErrorCollector();
        rule.check(fileOf(f), c);
        assertTrue(c.errors().stream().anyMatch(e -> e.code() == SemanticErrorCode.LEG_MISSING_FUEL));
    }

    @ParameterizedTest(name = "Fuel quantity {0} is rejected as not strictly positive")
    @ValueSource(doubles = {0.0, -0.1, -100, -5000})
    @DisplayName("Fuel ≤ 0 is flagged with FUEL_NOT_POSITIVE")
    void fuelNotPositive(double bad) {
        Leg badLeg = new Leg(
                1, departure("OPO", "2026-06-01T08:00"), arrival("LIS", "2026-06-01T09:00"),
                new Fuel(bad, FuelUnit.KG, Position.UNKNOWN),
                List.of(segment(coord(0, 0), coord(1, 1))),
                Position.UNKNOWN);
        Flight f = wrap(badLeg);
        ErrorCollector c = new ErrorCollector();
        rule.check(fileOf(f), c);
        assertTrue(c.errors().stream().anyMatch(e -> e.code() == SemanticErrorCode.FUEL_NOT_POSITIVE),
                "Expected FUEL_NOT_POSITIVE for quantity " + bad);
    }

    private static Flight wrap(Leg leg) {
        return new Flight(
                "TP100", FlightType.REGULAR,
                new Route(airport("OPO"), airport("LIS"), Position.UNKNOWN),
                List.of(leg), Position.UNKNOWN);
    }
}