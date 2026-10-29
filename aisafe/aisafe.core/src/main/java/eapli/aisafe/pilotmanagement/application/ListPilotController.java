package eapli.aisafe.pilotmanagement.application;

import eapli.framework.application.UseCaseController;
import eapli.aisafe.pilotmanagement.domain.Pilot;
import eapli.aisafe.pilotmanagement.repositories.PilotRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;


@UseCaseController
public class ListPilotController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();


    private final PilotRepository pilotRepository =
            PersistenceContext.repositories().pilots();

    public Iterable<Pilot> listPilots() {

        authz.ensureAuthenticatedUserHasAnyOf(
                AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);

        return pilotRepository.findAll();
    }
}