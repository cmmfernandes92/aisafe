package eapli.aisafe.infrastructure.bootstrapers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModel;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.pilotmanagement.domain.Pilot;
import eapli.aisafe.pilotmanagement.repositories.PilotRepository;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.framework.actions.Action;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;

public class PilotBootstrapper extends AbstractUserBootstrapper implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(PilotBootstrapper.class);

    private final PilotRepository pilotRepository = PersistenceContext.repositories().pilots();

    @Override
    public boolean execute() {
        registerUser("tp.pilot1", TestDataConstants.PASSWORD1, "Carlos", "Andrade",
                "carlos.andrade@tap.pt", "+351910000200",
                Set.of(AisafeRoles.PILOT),
                LocalDate.now().plusYears(2), LocalDate.now().minusMonths(6));
        registerUser("tp.pilot2", TestDataConstants.PASSWORD1, "Maria", "Costa",
                "maria.costa@tap.pt", "+351910000201",
                Set.of(AisafeRoles.PILOT),
                LocalDate.now().plusYears(2), LocalDate.now().minusMonths(6));
        registerUser("fr.pilot1", TestDataConstants.PASSWORD1, "João", "Ferreira",
                "joao.ferreira@ryanair.com", "+351910000202",
                Set.of(AisafeRoles.PILOT),
                LocalDate.now().plusYears(2), LocalDate.now().minusMonths(6));
        // Pilot for the US121 "import flight plan from file" demo (owns FR401 — see FlightBootstrapper).
        registerUser("ruipedro", TestDataConstants.PASSWORD1, "Rui", "Pedro",
                "rui.pedro@ryanair.com", "+351910000203",
                Set.of(AisafeRoles.PILOT),
                LocalDate.now().plusYears(2), LocalDate.now().minusMonths(6));

        final List<AircraftModel> models = new ArrayList<>();
        PersistenceContext.repositories().aircraftModels().findAll().forEach(models::add);
        if (models.isEmpty()) {
            LOGGER.warn("No aircraft models found; skipping pilot registration");
            return false;
        }
        final Set<AircraftModel> allModels = new HashSet<>(models);

        registerPilot(TestDataConstants.POWER_USER_EMAIL, "TP", allModels);
        registerPilot("carlos.andrade@tap.pt", "TP", allModels);
        registerPilot("maria.costa@tap.pt", "TP", allModels);
        registerPilot("joao.ferreira@ryanair.com", "FR", allModels);
        registerPilot("rui.pedro@ryanair.com", "FR", allModels);

        return true;
    }

    private void registerPilot(final String emailStr, final String companyCode,
            final Set<AircraftModel> certifications) {
        try {
            final Pilot pilot = new Pilot(Email.valueOf(emailStr),
                    CompanyIATACode.valueOf(companyCode), certifications);
            pilotRepository.save(pilot);
            LOGGER.debug("Registered pilot: {}", emailStr);
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            LOGGER.debug("Assuming pilot '{}' already exists", emailStr);
        } catch (final Exception e) {
            LOGGER.warn("Failed to register pilot '{}': {}", emailStr, e.getMessage());
        }
    }
}
