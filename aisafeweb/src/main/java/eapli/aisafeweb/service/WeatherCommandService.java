package eapli.aisafeweb.service;

import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.stereotype.Service;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircontrolareamanagement.domain.Coordinates;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.aisafe.weather.application.ImportBulkWeatherDataController;
import eapli.aisafe.weather.application.importing.ImportReport;
import eapli.aisafe.weather.application.importing.SourceType;
import eapli.aisafe.weather.application.importing.WeatherDataSourceReaderFactory;
import eapli.aisafe.weather.domain.AltitudeBand;
import eapli.aisafe.weather.domain.DataSource;
import eapli.aisafe.weather.domain.TimePeriod;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherWindDirection;
import eapli.aisafe.weather.domain.WeatherWindSpeed;
import eapli.aisafe.weather.domain.WeatherZone;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

@Service
public class WeatherCommandService {

    public void register(
            final String areaCode,
            final LocalDateTime startTime,
            final LocalDateTime endTime,
            final double zoneSwLat,
            final double zoneSwLon,
            final double zoneNeLat,
            final double zoneNeLon,
            final double altLowerFeet,
            final double altUpperFeet,
            final Double temperature,
            final Double pressure,
            final Double visibility,
            final double windSpeed,
            final double windDirection,
            final String dataSource) {
        AuthzRegistry.authorizationService().ensureAuthenticatedUserHasAnyOf(AisafeRoles.WEATHER_PERSON);

        final AreaCode code = AreaCode.valueOf(areaCode);
        PersistenceContext.repositories().airControlAreas().findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Air control area not found: " + areaCode));

        final WeatherZone zone = new WeatherZone(
                new Coordinates(zoneSwLat, zoneSwLon),
                new Coordinates(zoneNeLat, zoneNeLon),
                new AltitudeBand(altLowerFeet, altUpperFeet));
        final WeatherData data = new WeatherData(
                code,
                new TimePeriod(startTime, endTime),
                zone,
                temperature, pressure, visibility,
                new WeatherWindSpeed(windSpeed),
                new WeatherWindDirection(windDirection),
                new DataSource(dataSource));
        PersistenceContext.repositories().weatherData().save(data);
    }

    public ImportReport importCsv(final String areaCode, final String dataSource, final String csvContent) {
        return newImportController().importFrom(
                areaCode, SourceType.CSV, dataSource, new StringReader(csvContent));
    }

    public ImportReport importOpenMeteo(final String areaCode, final String dataSource,
            final LocalDate day, final LocalTime start, final LocalTime end) {
        return newImportController().importFrom(
                areaCode, SourceType.OPEN_METEO, dataSource, day, start, end);
    }

    private ImportBulkWeatherDataController newImportController() {
        final TransactionalContext tx = PersistenceContext.repositories().newTransactionalContext();
        return new ImportBulkWeatherDataController(
                PersistenceContext.repositories().weatherData(tx),
                PersistenceContext.repositories().airControlAreas(),
                new WeatherDataSourceReaderFactory(),
                tx);
    }
}
