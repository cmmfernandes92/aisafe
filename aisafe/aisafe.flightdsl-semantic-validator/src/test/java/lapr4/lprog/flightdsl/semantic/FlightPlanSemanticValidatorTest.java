package lapr4.lprog.flightdsl.semantic;

import lapr4.lprog.flightdsl.ast.AirportRef;
import lapr4.lprog.flightdsl.ast.Coordinate;
import lapr4.lprog.flightdsl.ast.Departure;
import lapr4.lprog.flightdsl.ast.Flight;
import lapr4.lprog.flightdsl.ast.FlightPlanFile;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static lapr4.lprog.flightdsl.semantic.AstFixtures.airport;
import static lapr4.lprog.flightdsl.semantic.AstFixtures.arrival;
import static lapr4.lprog.flightdsl.semantic.AstFixtures.departure;
import static lapr4.lprog.flightdsl.semantic.AstFixtures.fileOf;
import static lapr4.lprog.flightdsl.semantic.AstFixtures.validSingleLegFlight;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests for {@link FlightPlanSemanticValidator}. The key property under test
 * is the §3.4.5 guarantee that <em>all semantic errors must be collected and reported
 * in a single execution</em>.
 */
@DisplayName("FlightPlanSemanticValidator — integration")
class FlightPlanSemanticValidatorTest {

    private final FlightPlanSemanticValidator validator = new FlightPlanSemanticValidator();

    @Test
    @DisplayName("Valid file: 0 errors, isValid() = true")
    void happyPath() {
        FlightPlanFile file = fileOf(validSingleLegFlight("TP123"));
        SemanticValidationResult result = validator.validate(file);
        assertTrue(result.isValid(), () -> "Unexpected findings: " + result.render());
    }

    @Test
    @DisplayName("Null AST: returns empty result without throwing")
    void nullAst() {
        SemanticValidationResult result = validator.validate(null);
        assertTrue(result.isValid());
        assertTrue(result.findings().isEmpty());
    }

    @Test
    @DisplayName("Multiple violations are all collected in a single execution")
    void allErrorsCollectedAtOnce() {
        // Four planted violations:
        //  (a) negative fuel           → FUEL_NOT_POSITIVE
        //  (b) invalid datetime literal → DATETIME_INVALID_CALENDAR
        //  (c) segment start == end    → SEGMENT_START_EQUALS_END
        //  (d) malformed airport code  → AIRPORT_CODE_BAD_FORMAT
        Segment seg = new Segment(
                new Coordinate(0, 0, Position.UNKNOWN),
                new Coordinate(0, 0, Position.UNKNOWN),
                List.of(new Slot(new Length(10000, Position.UNKNOWN), new Length(500, Position.UNKNOWN),
                        Position.UNKNOWN)),
                new Wind(180, 5, Position.UNKNOWN),
                Position.UNKNOWN);

        Leg leg = new Leg(1,
                new Departure(new AirportRef("LongCode", Position.UNKNOWN),
                        TemporalLiteral.of("2026-02-30T08:00", Position.UNKNOWN),
                        Position.UNKNOWN),
                arrival("LIS", "2026-06-01T09:00"),
                new Fuel(-500, FuelUnit.KG, Position.UNKNOWN),
                List.of(seg),
                Position.UNKNOWN);

        Flight f = new Flight("TP999", FlightType.REGULAR,
                new Route(airport("OPO"), airport("LIS"), Position.UNKNOWN),
                List.of(leg), Position.UNKNOWN);

        SemanticValidationResult result = validator.validate(fileOf(f));
        assertFalse(result.isValid());
        assertTrue(result.has(SemanticErrorCode.FUEL_NOT_POSITIVE));
        assertTrue(result.has(SemanticErrorCode.DATETIME_INVALID_CALENDAR));
        assertTrue(result.has(SemanticErrorCode.SEGMENT_START_EQUALS_END));
        assertTrue(result.has(SemanticErrorCode.AIRPORT_CODE_BAD_FORMAT));
    }

    @Test
    @DisplayName("Findings carry line/column so error reports are actionable (§3.4.4)")
    void findingsHaveLocation() {
        Position bad = new Position(42, 7);
        Flight f = new Flight("TP999", FlightType.REGULAR,
                new Route(airport("OPO"), airport("LIS"), Position.UNKNOWN),
                List.of(new Leg(1,
                        departure("OPO", "2026-06-01T08:00"), arrival("LIS", "2026-06-01T09:00"),
                        new Fuel(-1, FuelUnit.KG, bad),
                        List.of(new Segment(
                                new Coordinate(0, 0, Position.UNKNOWN),
                                new Coordinate(1, 1, Position.UNKNOWN),
                                List.of(new Slot(new Length(10000, Position.UNKNOWN),
                                        new Length(500, Position.UNKNOWN), Position.UNKNOWN)),
                                new Wind(0, 5, Position.UNKNOWN), Position.UNKNOWN)),
                        Position.UNKNOWN)),
                Position.UNKNOWN);

        SemanticValidationResult result = validator.validate(fileOf(f));
        SemanticError fuelErr = result.findings().stream()
                .filter(e -> e.code() == SemanticErrorCode.FUEL_NOT_POSITIVE)
                .findFirst().orElseThrow();
        assertTrue(fuelErr.position().isKnown());
        assertTrue(fuelErr.render().contains("line 42"));
    }
}