package eapli.aisafe.persistence.impl.inmemory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherDataId;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

public class InMemoryWeatherDataRepository
        extends InMemoryDomainRepository<WeatherData, WeatherDataId>
        implements WeatherDataRepository {

    @Override
    public Iterable<WeatherData> findByAreaCodeAndDay(final AreaCode areaCode, final LocalDate day) {
        final List<WeatherData> result = new ArrayList<>();
        for (final WeatherData wd : findAll()) {
            if (wd.areaCode().equals(areaCode)
                    && wd.timePeriod().startTime().toLocalDate().equals(day)) {
                result.add(wd);
            }
        }
        return result;
    }
}
