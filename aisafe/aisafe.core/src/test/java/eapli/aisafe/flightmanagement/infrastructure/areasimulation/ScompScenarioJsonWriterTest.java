package eapli.aisafe.flightmanagement.infrastructure.areasimulation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import eapli.aisafe.flightmanagement.application.areasimulation.AreaSimulationScenario;
import eapli.aisafe.flightmanagement.application.areasimulation.FlightProfile;
import eapli.aisafe.flightmanagement.application.areasimulation.ScenarioFlight;
import eapli.aisafe.flightmanagement.application.areasimulation.ScenarioSegment;
import eapli.aisafe.flightmanagement.application.areasimulation.SimParams;
import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightroutemanagement.domain.RouteName;

public class ScompScenarioJsonWriterTest {

    @Test
    public void emitsNativeScenarioShape() {
        final AreaSimulationScenario scenario = new AreaSimulationScenario(
                new SimParams("LPPC-202606121200", 10, 60.0, 2, 3, 8.0, 600.0,
                        -1.0, 1.0, -2.0, 2.0, 0.0, 20000.0, 38.5, -9.0),
                List.of(new ScenarioFlight(1, FlightProfile.defaultJet(),
                        List.of(new ScenarioSegment("cruise",
                                38.5, -9.0, 9000.0,
                                38.6, -8.9, 9000.0)), 0)),
                Map.of(1, new FlightId(RouteName.valueOf("TP123"), LocalDate.of(2026, 6, 12))));

        final String json = new ScompScenarioJsonWriter().toJson(scenario);

        assertTrue(json.contains("\"sim_params\""));
        assertTrue(json.contains("\"flights\""));
        assertTrue(json.contains("\"ID\": 1"));
        assertTrue(json.contains("\"Flight Profile\""));
        assertTrue(json.contains("\"RateClimb\""));
        assertTrue(json.contains("\"Segments\""));
        assertTrue(json.contains("\"Latitude\": 38.500000"));
        assertTrue(json.contains("\"Quantity\": 9000.000000"));
        assertFalse("EntryStep is omitted when 0", json.contains("EntryStep"));
    }

    @Test
    public void emitsEntryStepWhenStaggered() {
        final AreaSimulationScenario scenario = new AreaSimulationScenario(
                new SimParams("LPPC-202606121200", 60, 60.0, 2, 3, 8.0, 600.0,
                        -1.0, 1.0, -2.0, 2.0, 0.0, 20000.0, 38.5, -9.0),
                List.of(new ScenarioFlight(7, FlightProfile.defaultJet(),
                        List.of(new ScenarioSegment("cruise",
                                38.5, -9.0, 9000.0,
                                38.6, -8.9, 9000.0)), 5)),
                Map.of(7, new FlightId(RouteName.valueOf("TP777"), LocalDate.of(2026, 6, 12))));

        final String json = new ScompScenarioJsonWriter().toJson(scenario);

        assertTrue(json.contains("\"EntryStep\": 5"));
    }
}
