package eapli.aisafe.persistence.impl.inmemory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;

import org.junit.Test;

import eapli.aisafe.fleetmanagement.domain.Registration;
import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.usermanagement.domain.Email;

public class InMemoryFlightRepositoryTest {

    private static final LocalDate JULY_1 = LocalDate.of(2026, 7, 1);

    private Flight flight(final String route, final LocalDate date) {
        return new Flight(RouteName.valueOf(route), Email.valueOf("pilot@tap.pt"),
                Registration.valueOf("CS-TUA"), date.atTime(9, 30));
    }

    @Test
    public void ensureSavedFlightIsFoundByIdentity() {
        final var repository = new InMemoryFlightRepository();
        final Flight saved = repository.save(flight("TP123", JULY_1));

        assertTrue(repository.ofIdentity(saved.identity()).isPresent());
    }

    @Test
    public void ensureExistsForRouteOnDateIsTrueForSavedFlight() {
        final var repository = new InMemoryFlightRepository();
        repository.save(flight("TP123", JULY_1));

        assertTrue(repository.existsForRouteOnDate(RouteName.valueOf("TP123"), JULY_1));
    }

    @Test
    public void ensureExistsForRouteOnDateIsFalseForDifferentDate() {
        final var repository = new InMemoryFlightRepository();
        repository.save(flight("TP123", JULY_1));

        assertFalse(repository.existsForRouteOnDate(RouteName.valueOf("TP123"), JULY_1.plusDays(1)));
    }

    @Test
    public void ensureExistsForRouteOnDateIsFalseForUnknownRoute() {
        final var repository = new InMemoryFlightRepository();
        repository.save(flight("TP123", JULY_1));

        assertFalse(repository.existsForRouteOnDate(RouteName.valueOf("TP999"), JULY_1));
    }

    @Test
    public void ensureExistsForRouteOnDateIsFalseWhenEmpty() {
        final var repository = new InMemoryFlightRepository();

        assertFalse(repository.existsForRouteOnDate(RouteName.valueOf("TP123"), JULY_1));
    }
}