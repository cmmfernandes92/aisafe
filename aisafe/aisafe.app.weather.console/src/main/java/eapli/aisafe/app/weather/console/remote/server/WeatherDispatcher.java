package eapli.aisafe.app.weather.console.remote.server;

import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import eapli.aisafe.aircontrolareamanagement.domain.AirControlArea;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.weather.application.ConsultWeatherDataController;
import eapli.aisafe.weather.application.ImportBulkWeatherDataController;
import eapli.aisafe.weather.application.RegisterWeatherDataController;
import eapli.aisafe.weather.application.importing.ImportReport;
import eapli.aisafe.weather.application.importing.SourceType;
import eapli.aisafe.weather.application.importing.WeatherDataSourceReaderFactory;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.framework.domain.repositories.TransactionalContext;

/**
 * Executes weather business commands by delegating to application controllers.
 * Results are materialised inside the call so that the caller (AuthGate) can
 * clear the session before the caller uses the returned objects.
 */
public class WeatherDispatcher {

    private ConsultWeatherDataController consultController() {
        return new ConsultWeatherDataController(
                PersistenceContext.repositories().weatherData(),
                PersistenceContext.repositories().airControlAreas());
    }

    private RegisterWeatherDataController registerController() {
        return new RegisterWeatherDataController(
                PersistenceContext.repositories().weatherData(),
                PersistenceContext.repositories().airControlAreas());
    }

    private ImportBulkWeatherDataController importController() {
        final TransactionalContext tx =
                PersistenceContext.repositories().newTransactionalContext();
        return new ImportBulkWeatherDataController(
                PersistenceContext.repositories().weatherData(tx),
                PersistenceContext.repositories().airControlAreas(),
                new WeatherDataSourceReaderFactory(),
                tx);
    }

    public List<AirControlArea> listAreas() {
        final List<AirControlArea> result = new ArrayList<>();
        consultController().listAirControlAreas().forEach(result::add);
        return result;
    }

    public List<WeatherData> consultWeather(final String areaCode, final LocalDate day) {
        final List<WeatherData> result = new ArrayList<>();
        consultController().consultWeatherData(areaCode, day).forEach(result::add);
        return result;
    }

    public WeatherData registerWeather(
            final String areaCode,
            final LocalDateTime startTime, final LocalDateTime endTime,
            final double zoneSwLat, final double zoneSwLon,
            final double zoneNeLat, final double zoneNeLon,
            final double altLowerFeet, final double altUpperFeet,
            final double temperature, final double pressure, final double visibility,
            final double windSpeed, final double windDirection,
            final String dataSource) {
        return registerController().registerWeatherData(
                areaCode, startTime, endTime,
                zoneSwLat, zoneSwLon, zoneNeLat, zoneNeLon,
                altLowerFeet, altUpperFeet,
                temperature, pressure, visibility,
                windSpeed, windDirection, dataSource);
    }

    public ImportReport importFromCsv(
            final String areaCode,
            final String dataSource,
            final String csvBody) {
        return importController().importFrom(
                areaCode, SourceType.CSV, dataSource, new StringReader(csvBody));
    }
}
