package eapli.aisafe.flightroutemanagement.application;

import java.time.LocalDate;

import eapli.aisafe.airtransportcompanymanagement.domain.AirTransportCompany;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.airtransportcompanymanagement.repositories.AirTransportCompanyRepository;
import eapli.aisafe.customercollaboratormanagement.repositories.CollaboratorRepository;
import eapli.aisafe.flightmanagement.repositories.FlightRepository;
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
public class DeactivateFlightRouteController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final FlightRouteRepository routeRepository;
    private final FlightRepository flightRepository;
    private final AirTransportCompanyRepository companyRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final UserRepository userRepository;

    public DeactivateFlightRouteController(final FlightRouteRepository routeRepository,
                                           final FlightRepository flightRepository,
                                           final AirTransportCompanyRepository companyRepository,
                                           final CollaboratorRepository collaboratorRepository,
                                           final UserRepository userRepository) {
        Preconditions.noneNull(routeRepository, flightRepository, companyRepository,
                collaboratorRepository, userRepository);
        this.routeRepository = routeRepository;
        this.flightRepository = flightRepository;
        this.companyRepository = companyRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.userRepository = userRepository;
    }

    public Iterable<FlightRoute> activeRoutesOfMyCompany() {
        authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
        return routeRepository.findActiveByCompany(resolveCompany());
    }

    public FlightRoute deactivateFlightRoute(final String routeName, final LocalDate effectiveDate) {
        authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
        Preconditions.noneNull(effectiveDate);

        final AirTransportCompany company = resolveCompany();
        final RouteName name = RouteName.valueOf(routeName);

        final FlightRoute route = routeRepository.ofIdentity(name)
                .orElseThrow(() -> new IllegalArgumentException("Flight route not found: " + routeName));

        if (!route.company().sameAs(company)) {
            throw new IllegalStateException(
                    "You can only deactivate routes belonging to your own company");
        }

        if (flightRepository.existsForRouteOnOrAfter(name, effectiveDate)) {
            throw new IllegalStateException(
                    "Cannot deactivate route " + name + ": there are planned flights on or after "
                            + effectiveDate);
        }

        route.deactivate(effectiveDate);
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
}
