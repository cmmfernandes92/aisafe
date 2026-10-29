package eapli.aisafe.persistence.impl.inmemory;

import eapli.aisafe.aircontrolareamanagement.repositories.AirControlAreaRepository;
import eapli.aisafe.aircraftmodelmanagement.repositories.AircraftModelRepository;
import eapli.aisafe.airportmanagement.repositories.AirportRepository;
import eapli.aisafe.airtransportcompanymanagement.repositories.AirTransportCompanyRepository;
import eapli.aisafe.countrymanagement.repositories.CountryRepository;
import eapli.aisafe.customercollaboratormanagement.repositories.CollaboratorRepository;
import eapli.aisafe.enginemodelmanagement.repositories.EngineModelRepository;
import eapli.aisafe.fleetmanagement.repositories.AircraftRepository;
import eapli.aisafe.flightmanagement.repositories.FlightRepository;
import eapli.aisafe.flightroutemanagement.repositories.FlightRouteRepository;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import eapli.aisafe.reporting.infrastructure.AggregatedMonthlyStatisticsRepository;
import eapli.aisafe.reporting.repositories.MonthlyStatisticsRepository;
import eapli.aisafe.infrastructure.bootstrapers.TestDataConstants;
import eapli.aisafe.infrastructure.persistence.RepositoryFactory;
import eapli.aisafe.makermanagement.repositories.MakerRepository;
import eapli.aisafe.pilotmanagement.repositories.PilotRepository;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.aisafe.usermanagement.domain.UserBuilderHelper;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.authz.domain.repositories.UserRepository;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;

public class InMemoryRepositoryFactory implements RepositoryFactory {

	@Override
	public UserRepository users(final TransactionalContext tx) {
		final var repo = new InMemoryUserRepository();
		final var userBuilder = UserBuilderHelper.builder();
		userBuilder.withUsername(TestDataConstants.POWER_USER).withPassword(TestDataConstants.POWER_USER_PASSWORD)
				.withName("joe", "power").withEmail(TestDataConstants.POWER_USER_EMAIL).withRoles(AisafeRoles.POWER_USER);
		final var newUser = userBuilder.build();
		repo.save(newUser);
		return repo;
	}

	@Override
	public UserRepository users() {
		return users(null);
	}

	@Override
	public eapli.aisafe.usermanagement.repositories.UserRepository aisafeUsers(final TransactionalContext tx) {
		return new InMemoryAisafeUserRepository();
	}

	@Override
	public eapli.aisafe.usermanagement.repositories.UserRepository aisafeUsers() {
		return aisafeUsers(null);
	}

	@Override
	public TransactionalContext newTransactionalContext() {
		return null;
	}

	@Override
	public MakerRepository makers() {
		return new InMemoryMakerRepository();
	}

	@Override
	public EngineModelRepository engineModels() {
		return new InMemoryEngineModelRepository();
	}

	@Override
	public AircraftModelRepository aircraftModels() {
		return new InMemoryAircraftModelRepository();
	}

	@Override
	public AirTransportCompanyRepository airTransportCompanies() {
		return new InMemoryAirTransportCompanyRepository();
	}

	@Override
	public CollaboratorRepository customerCollaborators() {
		return new InMemoryCollaboratorRepository();
	}

	@Override
	public PilotRepository pilots() {
		return new InMemoryPilotRepository();
	}

	@Override
	public AirControlAreaRepository airControlAreas(final TransactionalContext autoTx) {
		return new InMemoryAirControlAreaRepository();
	}

	@Override
	public AirControlAreaRepository airControlAreas() {
		return airControlAreas(null);
	}

	@Override
	public CountryRepository countries(final TransactionalContext autoTx) {
		return new InMemoryCountryRepository();
	}

	@Override
	public CountryRepository countries() {
		return countries(null);
	}

	@Override
	public AirportRepository airports(final TransactionalContext autoTx) {
		return new InMemoryAirportRepository();
	}

	@Override
	public AirportRepository airports() {
		return airports(null);
	}

	@Override
	public AircraftRepository aircraft() {
		return new InMemoryAircraftRepository();
	}

	@Override
	public FlightRepository flights() {
		return new InMemoryFlightRepository();
	}

	@Override
	public FlightRouteRepository flightRoutes() {
		return new InMemoryFlightRouteRepository();
	}

	@Override
	public WeatherDataRepository weatherData() {
		return new InMemoryWeatherDataRepository();
	}

	@Override
	public WeatherDataRepository weatherData(final TransactionalContext autoTx) {
		return new InMemoryWeatherDataRepository();
	}

	@Override
	public eapli.aisafe.reporting.repositories.SimulationRunRepository simulationRuns() {
		return new InMemorySimulationRunRepository();
	}

	@Override
	public MonthlyStatisticsRepository monthlyStatistics() {
		return new AggregatedMonthlyStatisticsRepository(simulationRuns(), flightRoutes(), airControlAreas());
	}
}
