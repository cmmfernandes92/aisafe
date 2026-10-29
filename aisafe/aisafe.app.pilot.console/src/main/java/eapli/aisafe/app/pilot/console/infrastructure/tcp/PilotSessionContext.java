package eapli.aisafe.app.pilot.console.infrastructure.tcp;

import java.time.LocalDateTime;
import java.util.Map;

import eapli.aisafe.fleetmanagement.domain.Registration;
import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightmanagement.domain.FlightPlanTestResult;
import eapli.aisafe.flightmanagement.domain.SegmentKey;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherDataId;

public interface PilotSessionContext {

    Flight createFlight(RouteName route, LocalDateTime departure, Registration registration);

    Flight importPlanFromDsl(FlightId flightId, String dslContent);

    void attachWeather(FlightId flightId, Map<SegmentKey, WeatherDataId> assignments);

    FlightPlanTestResult validateFlightPlan(FlightId flightId);

    Iterable<Flight> flightsOfAuthenticatedPilot();

    Iterable<WeatherData> allWeatherData();
}
