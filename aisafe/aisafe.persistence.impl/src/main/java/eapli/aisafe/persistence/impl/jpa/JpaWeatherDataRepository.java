package eapli.aisafe.persistence.impl.jpa;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import eapli.aisafe.Application;
import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherDataId;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

class JpaWeatherDataRepository
        extends JpaAutoTxRepository<WeatherData, WeatherDataId, WeatherDataId>
        implements WeatherDataRepository {

    public JpaWeatherDataRepository(final TransactionalContext autoTx) {
        super(autoTx, "id");
    }

    public JpaWeatherDataRepository(final String puname) {
        super(puname, Application.settings().getExtendedPersistenceProperties(), "id");
    }

    @Override
    public Iterable<WeatherData> findByAreaCodeAndDay(final AreaCode areaCode, final LocalDate day) {
        final LocalDateTime dayStart = day.atStartOfDay();
        final LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();
        final Map<String, Object> params = new HashMap<>();
        params.put("areaCode", areaCode);
        params.put("dayStart", dayStart);
        params.put("dayEnd", dayEnd);
        return match(
                "e.areaCode = :areaCode"
                + " AND e.timePeriod.startTime >= :dayStart"
                + " AND e.timePeriod.startTime < :dayEnd",
                params);
    }
}
