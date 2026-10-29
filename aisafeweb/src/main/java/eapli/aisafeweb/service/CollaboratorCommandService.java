package eapli.aisafeweb.service;

import org.springframework.stereotype.Service;

import eapli.aisafe.customercollaboratormanagement.application.RegisterCollaboratorController;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;

@Service
public class CollaboratorCommandService {

    public void registerCompanyCollaborator(
            final String email,
            final String position,
            final String companyCode) {
        controller().registerCompanyCollaborator(email, position, companyCode);
    }

    public void registerAreaCollaborator(
            final String email,
            final String position,
            final String areaCode) {
        controller().registerAreaCollaborator(email, position, areaCode);
    }

    private RegisterCollaboratorController controller() {
        return new RegisterCollaboratorController(
                PersistenceContext.repositories().customerCollaborators(),
                PersistenceContext.repositories().aisafeUsers(),
                PersistenceContext.repositories().airTransportCompanies(),
                PersistenceContext.repositories().airControlAreas());
    }
}
