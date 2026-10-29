package eapli.aisafe.infrastructure.bootstrapers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eapli.aisafe.countrymanagement.domain.Country;
import eapli.aisafe.countrymanagement.repositories.CountryRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.actions.Action;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;

@SuppressWarnings("squid:S106")
public class CountryBootstrapper implements Action {
    private static final Logger LOGGER = LoggerFactory.getLogger(CountryBootstrapper.class);

    private final CountryRepository repository = PersistenceContext.repositories().countries();

    @Override
    public boolean execute() {
        registerCountry("351", "Portugal");
        registerCountry("34", "Spain");
        registerCountry("33", "France");
        registerCountry("44", "United Kingdom");
        registerCountry("49", "Germany");
        return true;
    }

    private void registerCountry(final String code, final String name) {
        try {
            final Country country = Country.valueOf(code, name);
            if (repository.findByCode(country.code()).isPresent()
                    || repository.findByName(country.name()).isPresent()) {
                LOGGER.warn("Assuming country {} already registered (activate trace log for details)", code);
                return;
            }
            repository.save(country);
        } catch (IllegalArgumentException | ConcurrencyException | IntegrityViolationException e) {
            LOGGER.warn("Assuming country {} already registered (activate trace log for details)", code);
            LOGGER.trace("Assuming existing country", e);
        }
    }
}
