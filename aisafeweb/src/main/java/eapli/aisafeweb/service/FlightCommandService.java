package eapli.aisafeweb.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import eapli.aisafe.fleetmanagement.domain.Registration;
import eapli.aisafe.flightmanagement.application.CreateFlightController;
import eapli.aisafe.flightmanagement.application.ImportFlightPlanController;
import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;

/**
 * Web adapter for US080 (Create Flight) and US121 (Import Flight Plan). It
 * builds the AISafe core controllers with the shared repositories and delegates;
 * all business rules (ownership, DRAFT-only import, one VALIDATED plan) stay
 * inside the core controllers and domain. The pilot performing the use case is
 * the authenticated user, resolved by the core controller from the active
 * session.
 */
@Service
public class FlightCommandService {

    private CreateFlightController controller() {
        return new CreateFlightController(
                PersistenceContext.repositories().flights(),
                PersistenceContext.repositories().flightRoutes(),
                PersistenceContext.repositories().aircraft(),
                PersistenceContext.repositories().aircraftModels(),
                PersistenceContext.repositories().pilots(),
                PersistenceContext.repositories().aisafeUsers());
    }

    public Flight createDraft(final String routeName, final LocalDateTime departure, final String registration) {
        return controller().createDraftFlight(
                RouteName.valueOf(routeName), departure, Registration.valueOf(registration));
    }

    /**
     * US121: imports already-validated DSL content into the pilot's own flight,
     * promoting its draft plan {@code DRAFT -> VALIDATED}. The caller (web route)
     * runs the US120 file validation first; the core controller re-checks
     * existence, ownership, and plan state.
     */
    public Flight importPlan(final String routeName, final LocalDate departureDate, final String rawDslContent) {
        return new ImportFlightPlanController(PersistenceContext.repositories().flights())
                .importFlightPlan(new FlightId(RouteName.valueOf(routeName), departureDate), rawDslContent);
    }
}