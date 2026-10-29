package lapr4.lprog.flightdsl.semantic;

import lapr4.lprog.flightdsl.ast.Flight;
import lapr4.lprog.flightdsl.ast.FlightType;
import lapr4.lprog.flightdsl.ast.Fuel;
import lapr4.lprog.flightdsl.ast.FuelUnit;
import lapr4.lprog.flightdsl.ast.Leg;
import lapr4.lprog.flightdsl.ast.Position;
import lapr4.lprog.flightdsl.ast.Route;
import lapr4.lprog.flightdsl.ast.Segment;
import lapr4.lprog.flightdsl.semantic.rules.SegmentCoherenceRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static lapr4.lprog.flightdsl.semantic.AstFixtures.airport;
import static lapr4.lprog.flightdsl.semantic.AstFixtures.arrival;
import static lapr4.lprog.flightdsl.semantic.AstFixtures.coord;
import static lapr4.lprog.flightdsl.semantic.AstFixtures.departure;
import static lapr4.lprog.flightdsl.semantic.AstFixtures.fileOf;
import static lapr4.lprog.flightdsl.semantic.AstFixtures.slot;
import static lapr4.lprog.flightdsl.semantic.AstFixtures.validSingleLegFlight;
import static lapr4.lprog.flightdsl.semantic.AstFixtures.wind;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("§3.4.5 — Segment coherence")
class SegmentCoherenceRuleTest {

    private final SegmentCoherenceRule rule = new SegmentCoherenceRule();

    @Test
    @DisplayName("Valid segment with start ≠ end passes")
    void happyPath() {
        ErrorCollector c = new ErrorCollector();
        rule.check(fileOf(validSingleLegFlight("TP400")), c);
        assertTrue(c.errors().isEmpty(), () -> "Unexpected: " + c.errors());
    }

    @Test
    @DisplayName("Segment with identical start and end coordinates is flagged with SEGMENT_START_EQUALS_END")
    void startEqualsEnd() {
        Segment bad = new Segment(coord(41.2, -8.7), coord(41.2, -8.7),
                List.of(slot(10000, 500)), wind(0, 5), Position.UNKNOWN);
        Leg leg = new Leg(1,
                departure("OPO", "2026-06-01T08:00"), arrival("LIS", "2026-06-01T09:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN),
                List.of(bad), Position.UNKNOWN);
        Flight f = new Flight("TP400", FlightType.REGULAR,
                new Route(airport("OPO"), airport("LIS"), Position.UNKNOWN),
                List.of(leg), Position.UNKNOWN);

        ErrorCollector c = new ErrorCollector();
        rule.check(fileOf(f), c);
        assertTrue(c.errors().stream().anyMatch(e -> e.code() == SemanticErrorCode.SEGMENT_START_EQUALS_END));
    }
}
