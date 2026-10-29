package eapli.aisafe.weather.repositories;

import java.time.LocalDate;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherDataId;
import eapli.framework.domain.repositories.DomainRepository;

public interface WeatherDataRepository extends DomainRepository<WeatherDataId, WeatherData> {

    Iterable<WeatherData> findByAreaCodeAndDay(AreaCode areaCode, LocalDate day);
}
