package lapr4.lprog.flightdsl.semantic;

import lapr4.lprog.flightdsl.ast.Flight;
import lapr4.lprog.flightdsl.ast.FlightPlanFile;
import lapr4.lprog.flightdsl.ast.Position;
import lapr4.lprog.flightdsl.ast.Route;
import lapr4.lprog.flightdsl.semantic.rules.FlightLevelRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static lapr4.lprog.flightdsl.semantic.AstFixtures.airport;
import static lapr4.lprog.flightdsl.semantic.AstFixtures.fileOf;
import static lapr4.lprog.flightdsl.semantic.AstFixtures.simpleLeg;
import static lapr4.lprog.flightdsl.semantic.AstFixtures.validSingleLegFlight;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("§3.4.5 — Flight-level rules")
class FlightLevelRuleTest {

    private final FlightLevelRule rule = new FlightLevelRule();

    @Test
    @DisplayName("A valid flight produces no findings")
    void validFlight() {
        FlightPlanFile file = fileOf(validSingleLegFlight("TP123"));
        ErrorCollector c = new ErrorCollector();
        rule.check(file, c);
        assertTrue(c.errors().isEmpty(), "Expected no findings, got: " + c.errors());
    }

    @Test
    @DisplayName("Duplicate flight identifiers are flagged with FLIGHT_ID_NOT_UNIQUE on each duplicate")
    void duplicateIdentifiers() {
        FlightPlanFile file = fileOf(validSingleLegFlight("TP123"), validSingleLegFlight("TP123"));
        ErrorCollector c = new ErrorCollector();
        rule.check(file, c);
        assertEquals(2, c.errors().stream()
                .filter(e -> e.code() == SemanticErrorCode.FLIGHT_ID_NOT_UNIQUE)
                .count(), "Two duplicates should yield two findings");
    }

    @Test
    @DisplayName("Unique identifiers across multiple flights are accepted")
    void uniqueIdentifiers() {
        FlightPlanFile file = fileOf(validSingleLegFlight("TP123"), validSingleLegFlight("TP124"));
        ErrorCollector c = new ErrorCollector();
        rule.check(file, c);
        assertFalse(c.errors().stream().anyMatch(e -> e.code() == SemanticErrorCode.FLIGHT_ID_NOT_UNIQUE));
    }

    @Test
    @DisplayName("Null flight type is flagged with FLIGHT_TYPE_INVALID")
    void invalidFlightType() {
        Flight f = new Flight(
                "TP999",
                null,
                new Route(airport("OPO"), airport("LIS"), Position.UNKNOWN),
                List.of(simpleLeg(1, "OPO", "2026-06-01T08:00", "LIS", "2026-06-01T09:00")),
                Position.UNKNOWN);
        ErrorCollector c = new ErrorCollector();
        rule.check(fileOf(f), c);
        assertTrue(c.errors().stream().anyMatch(e -> e.code() == SemanticErrorCode.FLIGHT_TYPE_INVALID));
    }
}
