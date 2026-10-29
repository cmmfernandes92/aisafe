package eapli.aisafe.infrastructure.bootstrapers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eapli.aisafe.fleetmanagement.domain.Registration;
import eapli.aisafe.flightmanagement.application.parsing.FlightPlanCompilationResult;
import eapli.aisafe.flightmanagement.application.parsing.FlightPlanCompiler;
import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightmanagement.domain.FlightPlanStatus;
import eapli.aisafe.flightmanagement.domain.FlightPlanTestResult;
import eapli.aisafe.flightmanagement.domain.FlightPlanTestService;
import eapli.aisafe.flightmanagement.repositories.FlightRepository;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.framework.actions.Action;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;

public class FlightBootstrapper implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlightBootstrapper.class);
    private static final DateTimeFormatter DSL_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final FlightRepository flightRepository = PersistenceContext.repositories().flights();
    private final FlightPlanCompiler compiler = new BootstrapFlightPlanCompiler();

    // These demo flights feed the FCO area-movement simulation only, not the US085
    // physics model, so they are promoted straight to TESTED. The DSL is still compiled
    // below as a seed-time sanity check so a malformed plan is caught here, not silently
    // dropped from the candidate list at demo time.
    private final FlightPlanTestService testService =
            (flight, plan) -> FlightPlanTestResult.success("bootstrap demo flight (US085 physics skipped)");

    @Override
    public boolean execute() {
        final LocalDateTime base = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        boolean ok = true;
        for (final DemoFlight demo : demoFlights()) {
            ok &= registerTestedFlight(demo, base.plusMinutes(demo.departureOffsetMinutes()));
        }
        ok &= registerImportDemoDraftFlight(base);
        return ok;
    }

    // US121 "import flight plan from file" demo: one flight left as a plain DRAFT (US080
    // state, no plan imported) so the pilot ruipedro can demonstrate the import end-to-end
    // (DRAFT -> VALIDATED). It is deliberately NOT promoted to TESTED, so it stays out of
    // the FCO area-simulation candidate list and does not interfere with the other demos.
    private boolean registerImportDemoDraftFlight(final LocalDateTime base) {
        final String routeName = "FR401";
        final String pilotEmail = "rui.pedro@ryanair.com";
        final String registration = "CS-TNA";
        try {
            final RouteName route = RouteName.valueOf(routeName);
            final LocalDateTime departure = base.plusDays(1);
            final FlightId id = new FlightId(route, departure.toLocalDate());
            if (flightRepository.ofIdentity(id).isPresent()) {
                return true;
            }
            final Flight flight = new Flight(route, Email.valueOf(pilotEmail),
                    Registration.valueOf(registration), departure);
            flightRepository.save(flight);
            LOGGER.debug("Registered US121 import-demo DRAFT flight {} for {}", routeName, pilotEmail);
            return true;
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            LOGGER.debug("Assuming US121 import-demo flight '{}' already exists", routeName);
            return true;
        } catch (final Exception e) {
            LOGGER.warn("Failed to register US121 import-demo flight '{}': {}", routeName, e.getMessage());
            LOGGER.trace("US121 import-demo flight bootstrap failure", e);
            return false;
        }
    }

    // Five TESTED flights converging on the LPPC (Lisboa FIR) area from four directions
    // for the FCO area simulation.
    //
    // Departure offsets are ~2 h out from bootstrap so every flight stays inside the FCO's
    // "[now, now+24h]" candidate window for a generous demo window after seeding (the filter
    // drops flights whose departure is already in the past). The absolute offset does NOT
    // affect the simulation: SCOMP entry steps are computed relative to the EARLIEST
    // candidate's departure, so only the relative spacing (0/5/8/12/15 min) matters, keeping
    // several aircraft airborne together.
    //
    // Each flight cruises at a distinct altitude >= 1000 m apart so the SCOMP detector
    // (violation = h_dist < 8 m AND v_dist < 600 m) can never fire, even after flights park
    // on their destinations (SCOMP never deactivates an arrived flight).
    private List<DemoFlight> demoFlights() {
        return List.of(
                new DemoFlight("TP201", "carlos.andrade@tap.pt", "CS-TNA",
                        "TP201-OPO-LIS.flight", 120, 50),
                new DemoFlight("TP441", "maria.costa@tap.pt", "CS-TUK",
                        "TP441-CDG-OPO.flight", 125, 130),
                new DemoFlight("TP1015", "carlos.andrade@tap.pt", "CS-TNB",
                        "TP1015-MAD-LIS.flight", 128, 80),
                new DemoFlight("TP1692", "maria.costa@tap.pt", "CS-TNC",
                        "TP1692-FNC-LIS.flight", 132, 100),
                new DemoFlight("TP218", "carlos.andrade@tap.pt", "CS-TND",
                        "TP218-LIS-OPO.flight", 135, 50));
    }

    private boolean registerTestedFlight(final DemoFlight demo, final LocalDateTime targetDeparture) {
        try {
            final Flight flight = existingOrNew(demo, targetDeparture);
            final LocalDateTime departure = flight.departure().truncatedTo(ChronoUnit.MINUTES);
            final String dsl = loadPlan(demo.resourceName(), departure, departure.plusMinutes(demo.durationMinutes()));

            flight.importDsl(dsl, "bootstrap");
            final FlightPlanCompilationResult compiled = compiler.compile(dsl);
            if (!compiled.ok()) {
                LOGGER.warn("Demo flight {} DSL did not compile: {}", demo.routeName(), compiled.issues());
                return false;
            }
            final FlightPlanTestResult result = flight.testCurrentPlan(testService);
            if (flight.currentPlan().status() != FlightPlanStatus.TESTED) {
                LOGGER.warn("Demo flight {} did not reach TESTED: {}", demo.routeName(), result.message());
                return false;
            }

            flightRepository.save(flight);
            LOGGER.debug("Registered TESTED demo flight {} for {}", demo.routeName(), demo.pilotEmail());
            return true;
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            LOGGER.debug("Assuming demo flight '{}' already exists", demo.routeName());
            return true;
        } catch (final Exception e) {
            LOGGER.warn("Failed to register TESTED demo flight '{}': {}", demo.routeName(), e.getMessage());
            LOGGER.trace("Demo flight bootstrap failure", e);
            return false;
        }
    }

    private Flight existingOrNew(final DemoFlight demo, final LocalDateTime departure) {
        final RouteName route = RouteName.valueOf(demo.routeName());
        final FlightId id = new FlightId(route, departure.toLocalDate());
        removeConflictingDemoSeed(demo, route, id, departure.truncatedTo(ChronoUnit.MINUTES));
        return flightRepository.ofIdentity(id)
                .orElseGet(() -> new Flight(
                        route,
                        Email.valueOf(demo.pilotEmail()),
                        Registration.valueOf(demo.registration()),
                        departure));
    }

    private void removeConflictingDemoSeed(final DemoFlight demo, final RouteName route,
            final FlightId targetId, final LocalDateTime targetDeparture) {
        final Email demoPilot = Email.valueOf(demo.pilotEmail());
        final Registration demoAircraft = Registration.valueOf(demo.registration());
        for (final Flight existing : flightRepository.findAll()) {
            if (existing.route().equals(route)
                    && existing.pilot().equals(demoPilot)
                    && existing.aircraft().equals(demoAircraft)
                    && (!existing.identity().equals(targetId)
                            || !existing.departure().truncatedTo(ChronoUnit.MINUTES).equals(targetDeparture))) {
                flightRepository.delete(existing);
                LOGGER.debug("Removed conflicting bootstrap flight {} before reseeding {}", existing.identity(), targetId);
                return;
            }
        }
    }

    private String loadPlan(final String resourceName,
            final LocalDateTime departure, final LocalDateTime arrival) throws IOException {
        final String path = "/demo-flightplans/" + resourceName;
        try (InputStream in = FlightBootstrapper.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("Missing flight plan resource " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("__DEPARTURE__", DSL_DATETIME.format(departure))
                    .replace("__ARRIVAL__", DSL_DATETIME.format(arrival));
        }
    }

    private record DemoFlight(String routeName, String pilotEmail, String registration,
            String resourceName, int departureOffsetMinutes, int durationMinutes) {
    }
}
