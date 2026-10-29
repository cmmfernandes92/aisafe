package eapli.aisafe.flightmanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Map;
import eapli.aisafe.weather.domain.WeatherDataId;

import org.junit.Test;

public class FlightPlanTest {

    // --- creation: empty DRAFT plan (I1) ---

    @Test
    public void ensureNewPlanIsDraft() {
        assertEquals(FlightPlanStatus.DRAFT, new FlightPlan().status());
    }

    @Test
    public void ensureNewPlanHasNoContent() {
        assertNull(new FlightPlan().rawDslContent());
    }

    @Test
    public void ensureNewPlanIsNotValidated() {
        assertFalse(new FlightPlan().isValidated());
    }

    // --- transition: DRAFT -> VALIDATED ---

    @Test
    public void ensureMarkValidatedTransitionsDraftToValidated() {
        final FlightPlan plan = new FlightPlan();

        plan.markValidated();

        assertEquals(FlightPlanStatus.VALIDATED, plan.status());
        assertTrue(plan.isValidated());
    }

    @Test(expected = IllegalStateException.class)
    public void ensureMarkValidatedRejectsAnAlreadyValidatedPlan() {
        final FlightPlan plan = new FlightPlan();
        plan.markValidated();

        plan.markValidated();
    }

    // --- supersede: import a validated DSL body (US121) ---

    @Test
    public void ensureSupersedeSetsContentAndImportMetadataAndKeepsDraft() {
        final FlightPlan plan = new FlightPlan();

        plan.supersede("flight TP123 { ... }", "pilot@tap.pt");

        assertEquals("flight TP123 { ... }", plan.rawDslContent());
        assertEquals("pilot@tap.pt", plan.importedBy());
        assertNotNull(plan.importedAt());
        assertEquals(FlightPlanStatus.DRAFT, plan.status()); // promotion is a separate step
    }

    @Test(expected = IllegalStateException.class)
    public void ensureSupersedeRejectsANonDraftPlan() {
        final FlightPlan plan = new FlightPlan();
        plan.markValidated();

        plan.supersede("flight TP123 { ... }", "pilot@tap.pt");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureSupersedeRejectsEmptyContent() {
        new FlightPlan().supersede("", "pilot@tap.pt");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureSupersedeRejectsMissingImporter() {
        new FlightPlan().supersede("flight TP123 { ... }", null);
    }


    @Test
    public void ensureAttachWeatherAddsAssignments() {
        final FlightPlan plan = new FlightPlan();
        final WeatherDataId w1 = WeatherDataId.newId();

        plan.attachWeather(Map.of(new SegmentKey(1, 0), w1));

        assertFalse(plan.weatherAssignments().isEmpty());
        assertEquals(w1, plan.weatherAssignmentsBySegment().get(SegmentKey.of(1, 0)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAttachWeatherRejectsEmptyMap() {
        new FlightPlan().attachWeather(Map.of());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAttachWeatherRejectsNullWeatherId() {
        final Map<SegmentKey, WeatherDataId> map = new java.util.HashMap<>();
        map.put(new SegmentKey(1, 0), null);
        new FlightPlan().attachWeather(map);
    }

    @Test
    public void ensureAttachWeatherStoresOneAssignmentPerSegment() {
        final FlightPlan plan = new FlightPlan();
        final WeatherDataId w0 = WeatherDataId.newId();
        final WeatherDataId w1 = WeatherDataId.newId();

        final Map<SegmentKey, WeatherDataId> map = new java.util.HashMap<>();
        map.put(new SegmentKey(1, 0), w0);
        map.put(new SegmentKey(1, 1), w1);
        plan.attachWeather(map);

        assertEquals(2, plan.weatherAssignments().size());
        assertEquals(w0, plan.weatherAssignmentsBySegment().get(SegmentKey.of(1, 0)));
        assertEquals(w1, plan.weatherAssignmentsBySegment().get(SegmentKey.of(1, 1)));
    }

    @Test
    public void ensureReattachingSameSegmentReplacesAndDoesNotAccumulate() {
        final FlightPlan plan = new FlightPlan();
        final WeatherDataId first = WeatherDataId.newId();
        final WeatherDataId second = WeatherDataId.newId();

        plan.attachWeather(Map.of(new SegmentKey(1, 0), first));
        plan.attachWeather(Map.of(new SegmentKey(1, 0), second)); // re-attach same cell

        assertEquals("latest-wins: one row per segment", 1, plan.weatherAssignments().size());
        assertEquals(second, plan.weatherAssignmentsBySegment().get(SegmentKey.of(1, 0)));
    }

    @Test
    public void ensureAttachWeatherKeepsValidatedStateValidated() {
        final FlightPlan plan = new FlightPlan();
        plan.markValidated();

        plan.attachWeather(Map.of(new SegmentKey(1, 0), WeatherDataId.newId()));

        assertEquals(FlightPlanStatus.VALIDATED, plan.status());
    }

    @Test
    public void ensureAttachWeatherDemotesTestedToValidated() {
        final FlightPlan plan = new FlightPlan();
        plan.markValidated();
        plan.markTested();

        plan.attachWeather(Map.of(new SegmentKey(1, 0), WeatherDataId.newId()));

        // re-attaching weather voids the test, not the DSL → TESTED -> VALIDATED
        assertEquals(FlightPlanStatus.VALIDATED, plan.status());
    }

    // --- transition: VALIDATED -> TESTED (US085) ---

    @Test
    public void ensureMarkTestedTransitionsValidatedToTested() {
        final FlightPlan plan = new FlightPlan();
        plan.markValidated();

        plan.markTested();

        assertEquals(FlightPlanStatus.TESTED, plan.status());
    }

    @Test(expected = IllegalStateException.class)
    public void ensureMarkTestedRejectsDraftPlan() {
        new FlightPlan().markTested();
    }

    @Test(expected = IllegalStateException.class)
    public void ensureMarkTestedRejectsAlreadyTestedPlan() {
        final FlightPlan plan = new FlightPlan();
        plan.markValidated();
        plan.markTested();

        plan.markTested();
    }

}