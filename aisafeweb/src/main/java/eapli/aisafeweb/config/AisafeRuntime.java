package eapli.aisafeweb.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eapli.aisafe.infrastructure.bootstrapers.AisafeBootstrapper;
import eapli.aisafe.infrastructure.bootstrapers.FlightRouteBootstrapper;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.usermanagement.domain.AisafePasswordPolicy;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import jakarta.annotation.PostConstruct;

@Component
public class AisafeRuntime {

    private static final Logger LOGGER = LoggerFactory.getLogger(AisafeRuntime.class);

    private final boolean bootstrapEnabled;

    public AisafeRuntime(@Value("${aisafeweb.bootstrap.enabled:false}") final boolean bootstrapEnabled) {
        this.bootstrapEnabled = bootstrapEnabled;
    }

    @PostConstruct
    public void initializeAisafeRuntime() {
        AuthzRegistry.configure(
                PersistenceContext.repositories().users(),
                new AisafePasswordPolicy(),
                new PlainTextEncoder());

        if (bootstrapEnabled) {
            LOGGER.info("Running AISafe reusable bootstrapper from aisafeweb startup");
            new AisafeBootstrapper().execute();
        }

        seedDemoRoutes();
    }

    private void seedDemoRoutes() {
        try {
            final var routes = PersistenceContext.repositories().flightRoutes().findAll();
            if (!routes.iterator().hasNext()) {
                LOGGER.info("Seeding demo flight routes");
                new FlightRouteBootstrapper().execute();
            }
        } catch (final Exception e) {
            LOGGER.debug("Skipping route seeding (table may not exist yet): {}", e.getMessage());
        }
    }
}
