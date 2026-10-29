package eapli.aisafe.infrastructure.bootstrapers;

import java.util.EnumMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eapli.aisafe.fleetmanagement.application.AddAircraftController;
import eapli.aisafe.fleetmanagement.domain.SeatClass;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.actions.Action;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;

@SuppressWarnings("squid:S106")
public class AircraftBootstrapper implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(AircraftBootstrapper.class);

    private final AddAircraftController controller = new AddAircraftController(
            PersistenceContext.repositories().aircraft(),
            PersistenceContext.repositories().customerCollaborators(),
            PersistenceContext.repositories().aircraftModels(),
            PersistenceContext.repositories().aisafeUsers());

    @Override
    public boolean execute() {
        final Long a320Id = findModelId("A320neo");
        final Long b789Id = findModelId("787-9");

        if (a320Id != null) {
            register("CS-TNA", a320Id, economy(150), "Portugal", "TP");
            register("CS-TNB", a320Id, economyAndBusiness(130, 20), "Portugal", "TP");
            register("CS-TNC", a320Id, economy(180), "Portugal", "TP");
            register("CS-TND", a320Id, economyAndBusiness(150, 12), "Portugal", "TP");
        }
        if (b789Id != null) {
            register("CS-TUK", b789Id, economyAndBusiness(200, 48), "Portugal", "TP");
        }
        return true;
    }

    private Long findModelId(final String modelName) {
        for (final var model : PersistenceContext.repositories().aircraftModels().findAll()) {
            if (model.modelName().toString().equalsIgnoreCase(modelName)) {
                return model.identity();
            }
        }
        LOGGER.warn("Aircraft model '{}' not found; skipping related aircraft", modelName);
        return null;
    }

    private Map<SeatClass, Integer> economy(final int count) {
        final Map<SeatClass, Integer> seats = new EnumMap<>(SeatClass.class);
        seats.put(SeatClass.ECONOMY, count);
        return seats;
    }

    private Map<SeatClass, Integer> economyAndBusiness(final int economy, final int business) {
        final Map<SeatClass, Integer> seats = new EnumMap<>(SeatClass.class);
        seats.put(SeatClass.ECONOMY, economy);
        seats.put(SeatClass.BUSINESS, business);
        return seats;
    }

    private void register(final String registration, final Long modelId,
            final Map<SeatClass, Integer> seats, final String country, final String companyIataCode) {
        try {
            controller.addAircraftForCompany(registration, modelId, seats, country, companyIataCode);
            LOGGER.debug("Registered aircraft: {}", registration);
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            LOGGER.debug("Assuming aircraft '{}' already exists", registration);
        } catch (final IllegalArgumentException e) {
            LOGGER.warn("Failed to register aircraft '{}': {}", registration, e.getMessage());
        }
    }
}
