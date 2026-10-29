package eapli.aisafe.persistence.impl.jpa;

import eapli.aisafe.Application;
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
import eapli.aisafe.infrastructure.persistence.RepositoryFactory;
import eapli.aisafe.makermanagement.repositories.MakerRepository;
import eapli.aisafe.pilotmanagement.repositories.PilotRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.authz.domain.repositories.UserRepository;
import eapli.framework.infrastructure.authz.repositories.impl.jpa.JpaAutoTxUserRepository;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

public class JpaRepositoryFactory implements RepositoryFactory {

	@Override
	public UserRepository users(final TransactionalContext autoTx) {
		return new JpaAutoTxUserRepository(autoTx);
	}

	@Override
	public UserRepository users() {
		return new JpaAutoTxUserRepository(Application.settings().getPersistenceUnitName(),
				Application.settings().getExtendedPersistenceProperties());
	}

	@Override
	public TransactionalContext newTransactionalContext() {
		return JpaAutoTxRepository.buildTransactionalContext(Application.settings().getPersistenceUnitName(),
				Application.settings().getExtendedPersistenceProperties());
	}

	@Override
	public eapli.aisafe.usermanagement.repositories.UserRepository aisafeUsers(final TransactionalContext autoTx) {
		return new JpaUserRepository(autoTx);
	}

	@Override
	public eapli.aisafe.usermanagement.repositories.UserRepository aisafeUsers() {
		return new JpaUserRepository(Application.settings().getPersistenceUnitName());
	}

	@Override
	public MakerRepository makers() {
		return new JpaMakerRepository(Application.settings().getPersistenceUnitName());
	}

	@Override
	public EngineModelRepository engineModels() {
		return new JpaEngineModelRepository(Application.settings().getPersistenceUnitName());
	}

	@Override
	public AircraftModelRepository aircraftModels() {
		return new JpaAircraftModelRepository(Application.settings().getPersistenceUnitName());
	}

	@Override
	public AirTransportCompanyRepository airTransportCompanies() {
		return new JpaAirTransportCompanyRepository(Application.settings().getPersistenceUnitName());
	}

	@Override
	public CollaboratorRepository customerCollaborators() {
		return new JpaCollaboratorRepository(Application.settings().getPersistenceUnitName());
	}

	@Override
	public PilotRepository pilots() {
		return new JpaPilotRepository(Application.settings().getPersistenceUnitName());
	}

	@Override
	public AirControlAreaRepository airControlAreas(final TransactionalContext autoTx) {
		return new JpaAirControlAreaRepository(autoTx);
	}

	@Override
	public AirControlAreaRepository airControlAreas() {
		return new JpaAirControlAreaRepository(Application.settings().getPersistenceUnitName());
	}

	@Override
	public CountryRepository countries(final TransactionalContext autoTx) {
		return new JpaCountryRepository(autoTx);
	}

	@Override
	public CountryRepository countries() {
		return new JpaCountryRepository(Application.settings().getPersistenceUnitName());
	}

	@Override
	public AirportRepository airports(final TransactionalContext autoTx) {
		return new JpaAirportRepository(autoTx);
	}

	@Override
	public AirportRepository airports() {
		return new JpaAirportRepository(Application.settings().getPersistenceUnitName());
	}

	@Override
	public AircraftRepository aircraft() {
		return new JpaAircraftRepository(Application.settings().getPersistenceUnitName());
	}

	@Override
	public FlightRepository flights() {
		return new JpaFlightRepository(Application.settings().getPersistenceUnitName());
	}

	@Override
	public FlightRouteRepository flightRoutes() {
		return new JpaFlightRouteRepository(Application.settings().getPersistenceUnitName());
	}

	@Override
	public WeatherDataRepository weatherData() {
		return new JpaWeatherDataRepository(Application.settings().getPersistenceUnitName());
	}

	@Override
	public WeatherDataRepository weatherData(final TransactionalContext autoTx) {
		return new JpaWeatherDataRepository(autoTx);
	}

	@Override
	public eapli.aisafe.reporting.repositories.SimulationRunRepository simulationRuns() {
		return new JpaSimulationRunRepository(Application.settings().getPersistenceUnitName());
	}

	@Override
	public MonthlyStatisticsRepository monthlyStatistics() {
		return new AggregatedMonthlyStatisticsRepository(simulationRuns(), flightRoutes(), airControlAreas());
	}
}
