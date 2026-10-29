package eapli.aisafe.infrastructure.persistence;

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
import eapli.aisafe.makermanagement.repositories.MakerRepository;
import eapli.aisafe.pilotmanagement.repositories.PilotRepository;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import eapli.aisafe.reporting.repositories.MonthlyStatisticsRepository;
import eapli.aisafe.reporting.repositories.SimulationRunRepository;
import eapli.aisafe.usermanagement.repositories.UserRepository;
import eapli.framework.domain.repositories.TransactionalContext;

public interface RepositoryFactory {

	TransactionalContext newTransactionalContext();

	eapli.framework.infrastructure.authz.domain.repositories.UserRepository users(TransactionalContext autoTx);

	eapli.framework.infrastructure.authz.domain.repositories.UserRepository users();

	UserRepository aisafeUsers(TransactionalContext autoTx);

	UserRepository aisafeUsers();

	MakerRepository makers();

	EngineModelRepository engineModels();

	AircraftModelRepository aircraftModels();

	AirTransportCompanyRepository airTransportCompanies();

	CollaboratorRepository customerCollaborators();

	PilotRepository pilots();

	AirControlAreaRepository airControlAreas(TransactionalContext autoTx);

	AirControlAreaRepository airControlAreas();

	CountryRepository countries(TransactionalContext autoTx);

	CountryRepository countries();

	AirportRepository airports(TransactionalContext autoTx);

	AirportRepository airports();

	AircraftRepository aircraft();

	FlightRepository flights();

	FlightRouteRepository flightRoutes();

	WeatherDataRepository weatherData();

	WeatherDataRepository weatherData(TransactionalContext autoTx);

	SimulationRunRepository simulationRuns();

	MonthlyStatisticsRepository monthlyStatistics();
}
