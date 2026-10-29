package lapr4.lprog.flightdsl.semantic;

import lapr4.lprog.flightdsl.ast.AirportRef;
import lapr4.lprog.flightdsl.ast.Departure;
import lapr4.lprog.flightdsl.ast.Flight;
import lapr4.lprog.flightdsl.ast.FlightType;
import lapr4.lprog.flightdsl.ast.Fuel;
import lapr4.lprog.flightdsl.ast.FuelUnit;
import lapr4.lprog.flightdsl.ast.Leg;
import lapr4.lprog.flightdsl.ast.Length;
import lapr4.lprog.flightdsl.ast.Position;
import lapr4.lprog.flightdsl.ast.Route;
import lapr4.lprog.flightdsl.ast.Segment;
import lapr4.lprog.flightdsl.ast.Slot;
import lapr4.lprog.flightdsl.ast.TemporalLiteral;
import lapr4.lprog.flightdsl.ast.Wind;
import lapr4.lprog.flightdsl.semantic.rules.DomainConsistencyRule;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("§3.4.5 — Domain consistency")
class DomainConsistencyRuleTest {

    private final DomainConsistencyRule rule = new DomainConsistencyRule();

    @Test
    @DisplayName("Happy path produces no findings")
    void happyPath() {
        ErrorCollector c = new ErrorCollector();
        rule.check(fileOf(validSingleLegFlight("TP500")), c);
        assertTrue(c.errors().isEmpty(), () -> "Unexpected: " + c.errors());
    }

    @ParameterizedTest(name = "Airport code '{0}' is rejected (not IATA/ICAO uppercase)")
    @ValueSource(strings = {"op", "opo", "OPO123", "LongName", "AB", "ABCDE"})
    @DisplayName("Airport codes outside IATA/ICAO are flagged with AIRPORT_CODE_BAD_FORMAT")
    void badAirportCode(String bad) {
        Flight f = new Flight(
                "TP500", FlightType.REGULAR,
                new Route(new AirportRef(bad, Position.UNKNOWN), airport("LIS"), Position.UNKNOWN),
                List.of(new Leg(1,
                        new Departure(new AirportRef(bad, Position.UNKNOWN),
                                TemporalLiteral.of("2026-06-01T08:00", Position.UNKNOWN), Position.UNKNOWN),
                        arrival("LIS", "2026-06-01T09:00"),
                        new Fuel(5000, FuelUnit.KG, Position.UNKNOWN),
                        List.of(segment(coord(0, 0), coord(1, 1))),
                        Position.UNKNOWN)),
                Position.UNKNOWN);
        ErrorCollector c = new ErrorCollector();
        rule.check(fileOf(f), c);
        assertTrue(c.errors().stream().anyMatch(e -> e.code() == SemanticErrorCode.AIRPORT_CODE_BAD_FORMAT),
                "Expected AIRPORT_CODE_BAD_FORMAT for '" + bad + "'");
    }

    @ParameterizedTest(name = "Datetime '{0}' is rejected as not a valid calendar value")
    @ValueSource(strings = {"2026-02-30T08:00", "2026-13-01T08:00", "2026-06-01T25:00", "not-a-date"})
    @DisplayName("Invalid calendar dates are flagged with DATETIME_INVALID_CALENDAR")
    void badDateTime(String bad) {
        Flight f = new Flight(
                "TP500", FlightType.REGULAR,
                new Route(airport("OPO"), airport("LIS"), Position.UNKNOWN),
                List.of(new Leg(1,
                        new Departure(airport("OPO"),
                                TemporalLiteral.of(bad, Position.UNKNOWN), Position.UNKNOWN),
                        arrival("LIS", "2026-06-01T09:00"),
                        new Fuel(5000, FuelUnit.KG, Position.UNKNOWN),
                        List.of(segment(coord(0, 0), coord(1, 1))),
                        Position.UNKNOWN)),
                Position.UNKNOWN);
        ErrorCollector c = new ErrorCollector();
        rule.check(fileOf(f), c);
        assertTrue(c.errors().stream().anyMatch(e -> e.code() == SemanticErrorCode.DATETIME_INVALID_CALENDAR));
    }

    @Test
    @DisplayName("Altitude ≤ 0 is flagged with NUMERIC_NOT_POSITIVE")
    void badAltitude() {
        Segment bad = new Segment(coord(0, 0), coord(1, 1),
                List.of(new Slot(new Length(-10, Position.UNKNOWN), new Length(500, Position.UNKNOWN),
                        Position.UNKNOWN)),
                new Wind(180, 5, Position.UNKNOWN), Position.UNKNOWN);
        ErrorCollector c = new ErrorCollector();
        rule.check(fileOf(withSegment(bad)), c);
        assertTrue(c.errors().stream().anyMatch(e -> e.code() == SemanticErrorCode.NUMERIC_NOT_POSITIVE));
    }

    @Test
    @DisplayName("Width ≤ 0 is flagged with NUMERIC_NOT_POSITIVE")
    void badWidth() {
        Segment bad = new Segment(coord(0, 0), coord(1, 1),
                List.of(new Slot(new Length(10000, Position.UNKNOWN), new Length(0, Position.UNKNOWN),
                        Position.UNKNOWN)),
                new Wind(180, 5, Position.UNKNOWN), Position.UNKNOWN);
        ErrorCollector c = new ErrorCollector();
        rule.check(fileOf(withSegment(bad)), c);
        assertTrue(c.errors().stream().anyMatch(e -> e.code() == SemanticErrorCode.NUMERIC_NOT_POSITIVE));
    }

    @Test
    @DisplayName("Wind speed ≤ 0 is flagged with NUMERIC_NOT_POSITIVE")
    void badWindSpeed() {
        Segment bad = new Segment(coord(0, 0), coord(1, 1),
                List.of(new Slot(new Length(10000, Position.UNKNOWN), new Length(500, Position.UNKNOWN),
                        Position.UNKNOWN)),
                new Wind(180, -1, Position.UNKNOWN), Position.UNKNOWN);
        ErrorCollector c = new ErrorCollector();
        rule.check(fileOf(withSegment(bad)), c);
        assertTrue(c.errors().stream().anyMatch(e -> e.code() == SemanticErrorCode.NUMERIC_NOT_POSITIVE));
    }

    private static Flight withSegment(Segment segment) {
        Leg leg = new Leg(1,
                departure("OPO", "2026-06-01T08:00"), arrival("LIS", "2026-06-01T09:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN),
                List.of(segment), Position.UNKNOWN);
        return new Flight("TP500", FlightType.REGULAR,
                new Route(airport("OPO"), airport("LIS"), Position.UNKNOWN),
                List.of(leg), Position.UNKNOWN);
    }
}