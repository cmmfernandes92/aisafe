package eapli.aisafe.flightmanagement.application.parsing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

/**
 * Unit tests for the US085 DSL-free DTOs the {@link FlightPlanCompiler} port
 * exchanges with {@code aisafe.core}: their construction-time validation
 * (Preconditions), the {@link FlightPlanIssue#describe()} type+location rendering
 * (US085.4), and the {@link FlightPlanCompilationResult} ok/failed factories.
 */
public class ParsingDtoTest {

    private static final LocalDateTime DEP = LocalDateTime.parse("2026-06-12T13:00");
    private static final LocalDateTime ARR = LocalDateTime.parse("2026-06-12T14:00");

    private static ParsedSegment segment() {
        return new ParsedSegment(1, 0, 41.0, -8.0, 40.0, -9.0, 9000, 350, 8.0);
    }

    private static ParsedFlightPlan plan() {
        return new ParsedFlightPlan("FR401", "LPPR", "LPPT", DEP, ARR, 3300,
                ParsedFlightPlan.FuelUnit.KG, List.of(segment()));
    }

    // ---- ParsedFlightPlan ----

    @Test
    public void ensureValidParsedFlightPlanIsBuilt() {
        final ParsedFlightPlan p = plan();
        assertEquals("FR401", p.designator());
        assertEquals(1, p.segments().size());
        assertEquals(ParsedFlightPlan.FuelUnit.KG, p.fuelUnit());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureBlankDesignatorRejected() {
        new ParsedFlightPlan(" ", "LPPR", "LPPT", DEP, ARR, 3300, ParsedFlightPlan.FuelUnit.KG, List.of(segment()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureNullDepartureRejected() {
        new ParsedFlightPlan("FR401", "LPPR", "LPPT", null, ARR, 3300, ParsedFlightPlan.FuelUnit.KG, List.of(segment()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureNonPositiveFuelRejected() {
        new ParsedFlightPlan("FR401", "LPPR", "LPPT", DEP, ARR, 0, ParsedFlightPlan.FuelUnit.KG, List.of(segment()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureNullFuelUnitRejected() {
        new ParsedFlightPlan("FR401", "LPPR", "LPPT", DEP, ARR, 3300, null, List.of(segment()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureEmptySegmentsRejected() {
        new ParsedFlightPlan("FR401", "LPPR", "LPPT", DEP, ARR, 3300, ParsedFlightPlan.FuelUnit.KG, List.of());
    }

    // ---- ParsedSegment ----

    @Test(expected = IllegalArgumentException.class)
    public void ensureNegativeOrdinalSegmentRejected() {
        new ParsedSegment(-1, 0, 41.0, -8.0, 40.0, -9.0, 9000, 350, 8.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureNonPositiveAltitudeSegmentRejected() {
        new ParsedSegment(1, 0, 41.0, -8.0, 40.0, -9.0, 0, 350, 8.0);
    }

    // ---- FlightPlanIssue ----

    @Test
    public void ensureIssueDescribeIncludesPhaseAndLocation() {
        final FlightPlanIssue issue = new FlightPlanIssue("SEMANTIC", "SEM01", "wind speed must be positive", 27, 14);
        assertEquals("SEMANTIC [27:14] wind speed must be positive", issue.describe());
    }

    @Test
    public void ensureIssueDescribeOmitsLocationWhenUnknown() {
        final FlightPlanIssue issue = new FlightPlanIssue("LEXICAL", "LEX01", "unexpected token", -1, -1);
        assertEquals("LEXICAL unexpected token", issue.describe());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureBlankIssuePhaseRejected() {
        new FlightPlanIssue(" ", "C", "msg", 1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureBlankIssueMessageRejected() {
        new FlightPlanIssue("SEMANTIC", "C", " ", 1, 1);
    }

    // ---- FlightPlanCompilationResult ----

    @Test
    public void ensureOkResultCarriesPlanAndNoIssues() {
        final FlightPlanCompilationResult r = FlightPlanCompilationResult.ok(plan());
        assertTrue(r.ok());
        assertTrue(r.plan().isPresent());
        assertTrue(r.issues().isEmpty());
    }

    @Test
    public void ensureFailedResultCarriesIssuesAndNoPlan() {
        final FlightPlanCompilationResult r = FlightPlanCompilationResult.failed(
                List.of(new FlightPlanIssue("SYNTACTIC", "SYN01", "missing brace", 3, 0)));
        assertFalse(r.ok());
        assertFalse(r.plan().isPresent());
        assertEquals(1, r.issues().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureNullPlanOptionalRejected() {
        new FlightPlanCompilationResult(null, List.of());
    }
}