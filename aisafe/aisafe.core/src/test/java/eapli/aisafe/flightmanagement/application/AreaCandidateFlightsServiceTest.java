package eapli.aisafe.flightmanagement.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import eapli.aisafe.aircontrolareamanagement.domain.AirControlArea;
import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircontrolareamanagement.domain.AreaName;
import eapli.aisafe.aircontrolareamanagement.domain.Coordinates;
import eapli.aisafe.aircontrolareamanagement.domain.GeographicBoundary;
import eapli.aisafe.aircontrolareamanagement.repositories.AirControlAreaRepository;
import eapli.aisafe.fleetmanagement.domain.Registration;
import eapli.aisafe.flightmanagement.application.AreaCandidateFlightsService.CandidateFlight;
import eapli.aisafe.flightmanagement.application.parsing.FlightPlanCompilationResult;
import eapli.aisafe.flightmanagement.application.parsing.FlightPlanCompiler;
import eapli.aisafe.flightmanagement.application.parsing.ParsedFlightPlan;
import eapli.aisafe.flightmanagement.application.parsing.ParsedSegment;
import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightmanagement.domain.FlightPlanTestResult;
import eapli.aisafe.flightmanagement.repositories.FlightRepository;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.usermanagement.domain.Email;

public class AreaCandidateFlightsServiceTest {

    private static final AreaCode AREA = AreaCode.valueOf("LPPC");
    private static final Email PILOT = Email.valueOf("pilot@aisafe.test");
    // DSL markers the fake compiler routes on (Flight.importDsl stores raw content verbatim).
    private static final String INSIDE = "INSIDE";
    private static final String OUTSIDE = "OUTSIDE";
    private static final ParsedSegment SEG_INSIDE = seg(38.5, -9.0, 38.6, -9.1);
    private static final ParsedSegment SEG_OUTSIDE = seg(50.0, 2.0, 51.0, 3.0);

    private final FakeFlightRepository flights = new FakeFlightRepository();
    private final FakeAreaRepository areas = new FakeAreaRepository();
    private final FlightPlanCompiler compiler = dsl ->
            FlightPlanCompilationResult.ok(plan(INSIDE.equals(dsl) ? SEG_INSIDE : SEG_OUTSIDE));
    private final AreaCandidateFlightsService service =
            new AreaCandidateFlightsService(flights, areas, compiler);

    @Test
    public void includesTestedCrossingFlightInWindow() {
        areas.add(area());
        flights.save(tested(RouteName.valueOf("TP123"), LocalDate.of(2026, 6, 12), INSIDE));

        final List<CandidateFlight> result = service.candidates(AREA,
                LocalDateTime.of(2026, 6, 12, 0, 0), LocalDateTime.of(2026, 6, 13, 0, 0));

        assertEquals(1, result.size());
        final CandidateFlight c = result.get(0);
        assertEquals("TP123", c.route());
        assertEquals("CS-TUA", c.aircraft());
        assertEquals("2026-06-12", c.departureDate());
        assertEquals(1, c.segmentsInArea());
    }

    @Test
    public void candidatePlansReturnsFlightAndRecompiledPlan() {
        areas.add(area());
        final Flight flight = tested(RouteName.valueOf("TP123"), LocalDate.of(2026, 6, 12), INSIDE);
        flights.save(flight);

        final List<AreaCandidateFlightsService.AreaCandidate> result =
                service.candidatePlans(AREA, null, null);

        assertEquals(1, result.size());
        assertEquals(flight.identity(), result.get(0).flight().identity());
        assertEquals("PLAN", result.get(0).plan().designator());
        assertEquals(1, result.get(0).segmentsInArea());
    }

    @Test
    public void excludesNonTestedFlight() {
        areas.add(area());
        flights.save(validated(RouteName.valueOf("TP123"), LocalDate.of(2026, 6, 12), INSIDE));

        assertTrue(service.candidates(AREA, null, null).isEmpty());
    }

    @Test
    public void excludesTestedFlightNotCrossingArea() {
        areas.add(area());
        flights.save(tested(RouteName.valueOf("TP123"), LocalDate.of(2026, 6, 12), OUTSIDE));

        assertTrue(service.candidates(AREA, null, null).isEmpty());
    }

    @Test
    public void excludesFlightDepartingOutsideWindow() {
        areas.add(area());
        flights.save(tested(RouteName.valueOf("TP123"), LocalDate.of(2026, 6, 10), INSIDE));

        assertTrue(service.candidates(AREA,
                LocalDateTime.of(2026, 6, 12, 0, 0), LocalDateTime.of(2026, 6, 13, 0, 0)).isEmpty());
    }

    @Test
    public void nullWindowBoundsAreUnbounded() {
        areas.add(area());
        flights.save(tested(RouteName.valueOf("TP999"), LocalDate.of(2030, 1, 1), INSIDE));

        assertEquals(1, service.candidates(AREA, null, null).size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsOnUnknownArea() {
        service.candidates(AREA, null, null); // area never added
    }

    // ── fixtures ─────────────────────────────────────────────────────────────

    private static AirControlArea area() {
        return new AirControlArea(AREA, AreaName.valueOf("Lisbon ACC"),
                new GeographicBoundary(new Coordinates(38.0, -9.5), new Coordinates(39.0, -8.5)));
    }

    /** A flight whose current plan is TESTED, carrying the given DSL marker. */
    private static Flight tested(final RouteName route, final LocalDate date, final String dsl) {
        final Flight flight = validated(route, date, dsl);
        flight.testCurrentPlan((f, p) -> FlightPlanTestResult.success("ok"));
        return flight;
    }

    /** A flight whose current plan is VALIDATED (DSL imported, not yet tested). */
    private static Flight validated(final RouteName route, final LocalDate date, final String dsl) {
        final Flight flight = new Flight(route, PILOT, Registration.valueOf("CS-TUA"), date.atTime(13, 0));
        flight.importDsl(dsl, "tester");
        return flight;
    }

    private static ParsedSegment seg(final double sLat, final double sLon,
            final double eLat, final double eLon) {
        return new ParsedSegment(1, 0, sLat, sLon, eLat, eLon, 9000.0, 270.0, 5.0);
    }

    private static ParsedFlightPlan plan(final ParsedSegment segment) {
        return new ParsedFlightPlan("PLAN", "LPPR", "LPPT",
                LocalDateTime.of(2026, 6, 12, 13, 0), LocalDateTime.of(2026, 6, 12, 15, 0),
                5000.0, ParsedFlightPlan.FuelUnit.KG, List.of(segment));
    }

    // ── fakes (no Mockito) ───────────────────────────────────────────────────

    private static final class FakeFlightRepository implements FlightRepository {

        @Override
        public boolean existsForRouteOnOrAfter(eapli.aisafe.flightroutemanagement.domain.RouteName route, java.time.LocalDate date) {
            return false;
        }
        private final Map<FlightId, Flight> data = new HashMap<>();

        @Override
        public <S extends Flight> S save(final S entity) {
            data.put(entity.identity(), entity);
            return entity;
        }

        @Override
        public boolean existsForRouteOnDate(final RouteName route, final LocalDate date) {
            return data.containsKey(new FlightId(route, date));
        }

        @Override
        public Iterable<Flight> flightsOf(final Email pilot) {
            return data.values().stream().filter(f -> f.pilot().equals(pilot)).toList();
        }

        @Override
        public Optional<Flight> ofIdentity(final FlightId id) {
            return Optional.ofNullable(data.get(id));
        }

        @Override
        public Iterable<Flight> findAll() {
            return data.values();
        }

        @Override
        public long count() {
            return data.size();
        }

        @Override
        public void delete(final Flight entity) {
            data.remove(entity.identity());
        }

        @Override
        public void deleteOfIdentity(final FlightId id) {
            data.remove(id);
        }
    }

    private static final class FakeAreaRepository implements AirControlAreaRepository {
        private final Map<AreaCode, AirControlArea> data = new HashMap<>();

        void add(final AirControlArea area) {
            data.put(area.code(), area);
        }

        @Override
        public Optional<AirControlArea> findByCode(final AreaCode code) {
            return Optional.ofNullable(data.get(code));
        }

        @Override
        public Optional<AirControlArea> findByName(final AreaName name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterable<AirControlArea> findOverlapping(final GeographicBoundary boundary) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends AirControlArea> S save(final S entity) {
            data.put(entity.code(), entity);
            return entity;
        }

        @Override
        public Optional<AirControlArea> ofIdentity(final AreaCode id) {
            return findByCode(id);
        }

        @Override
        public Iterable<AirControlArea> findAll() {
            return data.values();
        }

        @Override
        public long count() {
            return data.size();
        }

        @Override
        public void delete(final AirControlArea entity) {
            data.remove(entity.code());
        }

        @Override
        public void deleteOfIdentity(final AreaCode id) {
            data.remove(id);
        }
    }
}
