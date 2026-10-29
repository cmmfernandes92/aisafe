package eapli.aisafe.flightmanagement.application;

import java.util.Optional;

import eapli.aisafe.aircraftmodelmanagement.repositories.AircraftModelRepository;
import eapli.aisafe.fleetmanagement.repositories.AircraftRepository;
import eapli.aisafe.flightmanagement.application.parsing.FlightPlanCompiler;
import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightmanagement.domain.FlightPlan;
import eapli.aisafe.flightmanagement.domain.FlightPlanStatus;
import eapli.aisafe.flightmanagement.domain.physics.FlightSimulationModel;
import eapli.aisafe.flightmanagement.repositories.FlightRepository;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import eapli.framework.application.UseCaseController;
import eapli.framework.validations.Preconditions;

/**
 * US085 read-only companion to {@link TestFlightPlanController}: produces the
 * {@link FlightSimulationModel} for one of a pilot's own flights — recompiled DSL,
 * weather-resolved winds, §3.3 physics and the test verdict — <em>without</em>
 * changing plan state. It is what the web simulator and a console report consume,
 * so they read the physics from {@code aisafe.core} instead of carrying a copy.
 *
 * <p>The same {@link SimulatorV1FlightPlanTestService} backs both: the state-changing
 * test (via {@code Flight.testCurrentPlan}) and this read-only view.</p>
 */
@UseCaseController
public class SimulateFlightPlanController {

    private final FlightRepository flightRepository;
    private final SimulatorV1FlightPlanTestService simulator;

    public SimulateFlightPlanController(final FlightRepository flightRepository,
            final FlightPlanCompiler compiler,
            final WeatherDataRepository weatherRepository,
            final AircraftRepository aircraftRepository,
            final AircraftModelRepository aircraftModelRepository) {
        Preconditions.noneNull(flightRepository, compiler, weatherRepository,
                aircraftRepository, aircraftModelRepository);
        this.flightRepository = flightRepository;
        this.simulator = new SimulatorV1FlightPlanTestService(
                compiler, weatherRepository, aircraftRepository, aircraftModelRepository);
    }

    /**
     * The simulation model for the pilot's own VALIDATED/TESTED flight, or empty if
     * it doesn't exist, isn't theirs, or has no DSL content yet. Read-only.
     */
    public Optional<FlightSimulationModel> simulateFor(final String pilotEmail, final FlightId flightId) {
        if (pilotEmail == null || flightId == null) {
            return Optional.empty();
        }
        final Flight flight = flightRepository.ofIdentity(flightId).orElse(null);
        if (flight == null || !flight.pilot().equals(Email.valueOf(pilotEmail))) {
            return Optional.empty();
        }
        final FlightPlan plan = flight.currentPlan();
        final FlightPlanStatus status = plan.status();
        if (status != FlightPlanStatus.VALIDATED && status != FlightPlanStatus.TESTED) {
            return Optional.empty();
        }
        if (plan.rawDslContent() == null || plan.rawDslContent().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(simulator.simulate(flight, plan));
    }
}