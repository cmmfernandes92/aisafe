package eapli.aisafe.app.pilot.console.infrastructure.tcp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eapli.aisafe.app.pilot.console.presentation.flightplan.ConsoleFlightPlanCompiler;
import eapli.aisafe.fleetmanagement.domain.Registration;
import eapli.aisafe.flightmanagement.application.AttachWeatherToPlanController;
import eapli.aisafe.flightmanagement.application.CreateFlightController;
import eapli.aisafe.flightmanagement.application.ImportFlightPlanController;
import eapli.aisafe.flightmanagement.application.SimulatorV1FlightPlanTestService;
import eapli.aisafe.flightmanagement.application.TestFlightPlanController;
import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightmanagement.domain.FlightPlanTestResult;
import eapli.aisafe.flightmanagement.domain.SegmentKey;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherDataId;

public class DefaultPilotSessionContext implements PilotSessionContext {

    @Override
    public Flight createFlight(final RouteName route, final LocalDateTime departure,
            final Registration registration) {
        return new CreateFlightController(
                PersistenceContext.repositories().flights(),
                PersistenceContext.repositories().flightRoutes(),
                PersistenceContext.repositories().aircraft(),
                PersistenceContext.repositories().aircraftModels(),
                PersistenceContext.repositories().pilots(),
                PersistenceContext.repositories().aisafeUsers())
                .createDraftFlight(route, departure, registration);
    }

    @Override
    public Flight importPlanFromDsl(final FlightId flightId, final String dslContent) {
        return new ImportFlightPlanController(
                PersistenceContext.repositories().flights())
                .importFlightPlan(flightId, dslContent);
    }

    @Override
    public void attachWeather(final FlightId flightId,
            final Map<SegmentKey, WeatherDataId> assignments) {
        new AttachWeatherToPlanController().attachWeather(flightId, assignments);
    }

    @Override
    public FlightPlanTestResult validateFlightPlan(final FlightId flightId) {
        return new TestFlightPlanController(
                PersistenceContext.repositories().flights(),
                new SimulatorV1FlightPlanTestService(
                        new ConsoleFlightPlanCompiler(),
                        PersistenceContext.repositories().weatherData(),
                        PersistenceContext.repositories().aircraft(),
                        PersistenceContext.repositories().aircraftModels()))
                .testFlightPlan(flightId);
    }

    @Override
    public Iterable<Flight> flightsOfAuthenticatedPilot() {
        return new ImportFlightPlanController(
                PersistenceContext.repositories().flights())
                .flightsOfAuthenticatedPilot();
    }

    @Override
    public Iterable<WeatherData> allWeatherData() {
        final List<WeatherData> list = new ArrayList<>();
        PersistenceContext.repositories().weatherData().findAll().forEach(list::add);
        return list;
    }
}
