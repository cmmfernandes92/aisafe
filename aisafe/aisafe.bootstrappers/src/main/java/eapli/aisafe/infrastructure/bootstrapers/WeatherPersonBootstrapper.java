package eapli.aisafe.infrastructure.bootstrapers;

import java.time.LocalDate;
import java.util.Set;

import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.framework.actions.Action;

public class WeatherPersonBootstrapper extends AbstractUserBootstrapper implements Action {

    @Override
    public boolean execute() {
        registerUser("weatherperson", TestDataConstants.PASSWORD1, "Weather", "Person",
                "weather.person@aisafe.pt", "+351910000300",
                Set.of(AisafeRoles.WEATHER_PERSON),
                LocalDate.now().plusYears(2), LocalDate.now().minusMonths(6));
        return true;
    }
}
