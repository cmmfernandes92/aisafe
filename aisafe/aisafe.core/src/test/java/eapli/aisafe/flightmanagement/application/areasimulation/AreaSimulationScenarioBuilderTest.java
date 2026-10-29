package eapli.aisafe.flightmanagement.application.areasimulation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import eapli.aisafe.aircontrolareamanagement.domain.AirControlArea;
import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircontrolareamanagement.domain.AreaName;
import eapli.aisafe.aircontrolareamanagement.domain.Coordinates;
import eapli.aisafe.aircontrolareamanagement.domain.GeographicBoundary;
import eapli.aisafe.fleetmanagement.domain.Registration;
import eapli.aisafe.flightmanagement.application.AreaCandidateFlightsService.AreaCandidate;
import eapli.aisafe.flightmanagement.application.parsing.ParsedFlightPlan;
import eapli.aisafe.flightmanagement.application.parsing.ParsedSegment;
import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.usermanagement.domain.Email;

public class AreaSimulationScenarioBuilderTest {

    private final AreaSimulationScenarioBuilder builder = new AreaSimulationScenarioBuilder();

    @Test
    public void clipsRouteSegmentToAreaBeforeBuildingScenario() {
        final AirControlArea area = area();
        final Flight flight = new Flight(RouteName.valueOf("TP123"), Email.valueOf("pilot@aisafe.test"),
                Registration.valueOf("CS-TUA"), LocalDateTime.of(2026, 6, 12, 12, 0));
        final ParsedFlightPlan plan = plan(segment(38.5, -10.0, 38.5, -8.0));
        final SurrogateIdMap idMap = new SurrogateIdMap();

        final AreaSimulationScenario scenario = builder.build(area,
                List.of(new AreaCandidate(flight, plan, 1)),
                new SimulationWindow(LocalDateTime.of(2026, 6, 12, 12, 0),
                        LocalDateTime.of(2026, 6, 12, 13, 0)),
                idMap);

        assertEquals(1, scenario.flights().size());
        assertEquals(1, scenario.flights().get(0).id());
        assertEquals(flight.identity(), scenario.idMap().get(1));
        assertEquals(60, scenario.params().totalSteps());
        assertEquals(0, scenario.flights().get(0).entryStep());
        assertTrue(scenario.params().areaXMin() < scenario.params().areaXMax());
        assertTrue(scenario.params().areaYMin() < scenario.params().areaYMax());

        final ScenarioSegment clipped = scenario.flights().get(0).segments().get(0);
        assertEquals(38.5, clipped.startLatitude(), 1e-9);
        assertEquals(-9.5, clipped.startLongitude(), 1e-9);
        assertEquals(38.5, clipped.endLatitude(), 1e-9);
        assertEquals(-8.5, clipped.endLongitude(), 1e-9);
    }

    @Test
    public void staggersEntryStepByDepartureOffset() {
        final AirControlArea area = area();
        final SurrogateIdMap idMap = new SurrogateIdMap();
        // two flights 30 min apart -> second enters at round(1800/60) = 30 steps later
        final Flight first = flight("TP100", LocalDateTime.of(2026, 6, 12, 12, 0));
        final Flight second = flight("TP200", LocalDateTime.of(2026, 6, 12, 12, 30));

        final AreaSimulationScenario scenario = builder.build(area,
                List.of(new AreaCandidate(second, plan(segment(38.5, -10.0, 38.5, -8.0)), 1),
                        new AreaCandidate(first, plan(segment(38.5, -10.0, 38.5, -8.0)), 1)),
                new SimulationWindow(LocalDateTime.of(2026, 6, 12, 12, 0),
                        LocalDateTime.of(2026, 6, 12, 14, 0)),
                idMap);

        assertEquals(2, scenario.flights().size());
        // builder sorts by departure: the earlier flight is index 0 and enters at 0
        assertEquals(0, scenario.flights().get(0).entryStep());
        assertEquals(30, scenario.flights().get(1).entryStep());
    }

    private static Flight flight(final String route, final LocalDateTime departure) {
        return new Flight(RouteName.valueOf(route), Email.valueOf("pilot@aisafe.test"),
                Registration.valueOf("CS-TUA"), departure);
    }

    private static AirControlArea area() {
        return new AirControlArea(AreaCode.valueOf("LPPC"), AreaName.valueOf("Lisboa FIR"),
                new GeographicBoundary(new Coordinates(38.0, -9.5), new Coordinates(39.0, -8.5)));
    }

    private static ParsedSegment segment(final double sLat, final double sLon,
            final double eLat, final double eLon) {
        return new ParsedSegment(1, 0, sLat, sLon, eLat, eLon, 9000.0, 270.0, 5.0);
    }

    private static ParsedFlightPlan plan(final ParsedSegment segment) {
        return new ParsedFlightPlan("TP123", "LPPR", "EDDF",
                LocalDateTime.of(2026, 6, 12, 12, 0), LocalDateTime.of(2026, 6, 12, 15, 0),
                5000.0, ParsedFlightPlan.FuelUnit.KG, List.of(segment));
    }
}
