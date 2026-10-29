package eapli.aisafe.infrastructure.bootstrapers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.makermanagement.domain.Maker;
import eapli.aisafe.makermanagement.repositories.MakerRepository;
import eapli.framework.actions.Action;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;

public class MakerBootstrapper implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(MakerBootstrapper.class);

    private final MakerRepository repository = PersistenceContext.repositories().makers();

    @Override
    public boolean execute() {
        register("Airbus");
        register("Boeing");
        register("Embraer");
        register("CFM International");
        register("Rolls-Royce");
        register("Pratt & Whitney");
        register("GE Aviation");
        return true;
    }

    private void register(final String name) {
        try {
            repository.save(Maker.valueOf(name));
            LOGGER.debug("Registered maker: {}", name);
        } catch (final ConcurrencyException | IntegrityViolationException e) {
            LOGGER.debug("Assuming maker '{}' already exists", name);
        }
    }
}
