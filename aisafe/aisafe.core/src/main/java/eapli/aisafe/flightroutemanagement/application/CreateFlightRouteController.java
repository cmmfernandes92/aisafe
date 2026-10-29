package eapli.aisafe.flightroutemanagement.application;

import java.util.Optional;

import eapli.aisafe.airportmanagement.domain.Airport;
import eapli.aisafe.airportmanagement.domain.AirportIATACode;
import eapli.aisafe.airportmanagement.domain.AirportICAOCode;
import eapli.aisafe.airportmanagement.repositories.AirportRepository;
import eapli.aisafe.airtransportcompanymanagement.domain.AirTransportCompany;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.airtransportcompanymanagement.repositories.AirTransportCompanyRepository;
import eapli.aisafe.customercollaboratormanagement.repositories.CollaboratorRepository;
import eapli.aisafe.flightroutemanagement.domain.FlightRoute;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.flightroutemanagement.repositories.FlightRouteRepository;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafe.usermanagement.repositories.UserRepository;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.validations.Preconditions;

@UseCaseController
public class CreateFlightRouteController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final FlightRouteRepository routeRepository;
    private final AirportRepository airportRepository;
    private final AirTransportCompanyRepository companyRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final UserRepository userRepository;

    public CreateFlightRouteController(final FlightRouteRepository routeRepository,
            final AirportRepository airportRepository,
            final AirTransportCompanyRepository companyRepository,
            final CollaboratorRepository collaboratorRepository,
            final UserRepository userRepository) {
        Preconditions.noneNull(routeRepository, airportRepository, companyRepository,
                collaboratorRepository, userRepository);
        this.routeRepository = routeRepository;
        this.airportRepository = airportRepository;
        this.companyRepository = companyRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.userRepository = userRepository;
    }

    public FlightRoute createFlightRoute(final String routeName,
            final String originCode, final String destinationCode) {
        authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);

        final AirTransportCompany company = resolveCompany();
        final RouteName name = RouteName.valueOf(routeName);

        if (!name.value().startsWith(company.iataCode().value())) {
            throw new IllegalArgumentException(
                    "Route name prefix must match your company's IATA code: " + company.iataCode());
        }
        if (routeRepository.existsByName(name)) {
            throw new IllegalArgumentException("Route name already exists: " + routeName);
        }

        final Airport origin = resolveAirport(originCode);
        final Airport destination = resolveAirport(destinationCode);

        final FlightRoute route = new FlightRoute(name, company, origin, destination);
        return routeRepository.save(route);
    }

    private AirTransportCompany resolveCompany() {
        final var systemUser = authz.session()
                .orElseThrow(() -> new IllegalStateException("No active session"))
                .authenticatedUser();
        final Email email = userRepository.findBySystemUser(systemUser)
                .orElseThrow(() -> new IllegalStateException("No user profile found"))
                .identity();
        final CompanyIATACode companyCode = collaboratorRepository
                .findCompanyCollaboratorByEmail(email)
                .orElseThrow(() -> new IllegalStateException("No company collaborator profile found"))
                .companyCode();
        return companyRepository.findByIataCode(companyCode)
                .orElseThrow(() -> new IllegalStateException("Company not found: " + companyCode));
    }

    private Airport resolveAirport(final String code) {
        final String upper = code.strip().toUpperCase();
        Optional<Airport> found = Optional.empty();
        if (upper.length() == 4) {
            found = airportRepository.findByIcaoCode(new AirportICAOCode(upper));
        } else if (upper.length() == 3) {
            found = airportRepository.findByIataCode(new AirportIATACode(upper));
        }
        return found.orElseThrow(
                () -> new IllegalArgumentException("Airport not found for code: " + code));
    }
}
