package eapli.aisafe.weather.application;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import eapli.aisafe.aircontrolareamanagement.domain.AirControlArea;
import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircontrolareamanagement.domain.Coordinates;
import eapli.aisafe.aircontrolareamanagement.domain.GeographicBoundary;
import eapli.aisafe.aircontrolareamanagement.repositories.AirControlAreaRepository;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.aisafe.weather.application.importing.ImportReport;
import eapli.aisafe.weather.application.importing.RawWeatherReading;
import eapli.aisafe.weather.application.importing.RowError;
import eapli.aisafe.weather.application.importing.SourceType;
import eapli.aisafe.weather.application.importing.WeatherDataSourceReader;
import eapli.aisafe.weather.application.importing.WeatherDataSourceReaderFactory;
import eapli.aisafe.weather.application.importing.WeatherImportRequest;
import eapli.aisafe.weather.domain.AltitudeBand;
import eapli.aisafe.weather.domain.DataSource;
import eapli.aisafe.weather.domain.TimePeriod;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherWindDirection;
import eapli.aisafe.weather.domain.WeatherWindSpeed;
import eapli.aisafe.weather.domain.WeatherZone;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import eapli.framework.application.UseCaseController;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.validations.Preconditions;

/**
 * Imports bulk weather data for one operator-selected air control area and one
 * data source (provider), both supplied at import time. The selected
 * {@link WeatherDataSourceReader} turns the source into raw readings; this
 * controller attributes the area and source, rebuilds each value object (which
 * re-validates every cell), and persists the valid records in a single
 * transaction. Invalid records are skipped and reported (fail-soft); the batch
 * still commits the valid ones.
 */
@UseCaseController
public class ImportBulkWeatherDataController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final WeatherDataRepository weatherDataRepository;
    private final AirControlAreaRepository airControlAreaRepository;
    private final WeatherDataSourceReaderFactory readerFactory;
    private final TransactionalContext transaction;

    public ImportBulkWeatherDataController(final WeatherDataRepository weatherDataRepository,
            final AirControlAreaRepository airControlAreaRepository,
            final WeatherDataSourceReaderFactory readerFactory,
            final TransactionalContext transaction) {
        Preconditions.noneNull(weatherDataRepository, airControlAreaRepository, readerFactory);
        this.weatherDataRepository = weatherDataRepository;
        this.airControlAreaRepository = airControlAreaRepository;
        this.readerFactory = readerFactory;
        this.transaction = transaction; // may be null (e.g. in-memory persistence / tests)
    }

    public Iterable<AirControlArea> listAirControlAreas() {
        authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.WEATHER_PERSON);
        return airControlAreaRepository.findAll();
    }

    /** Imports from a file source (e.g. CSV); the period is carried in the file. */
    public ImportReport importFrom(final String areaCodeValue, final SourceType sourceType,
            final String dataSourceValue, final Reader source) {
        return importUsing(areaCodeValue, sourceType, dataSourceValue,
                bounds -> WeatherImportRequest.ofFile(source));
    }

    /**
     * Imports from an API source (e.g. Open-Meteo); the adapter grids the area and
     * samples it over the operator's day/time window.
     */
    public ImportReport importFrom(final String areaCodeValue, final SourceType sourceType,
            final String dataSourceValue, final LocalDate day, final LocalTime start, final LocalTime end) {
        return importUsing(areaCodeValue, sourceType, dataSourceValue,
                bounds -> WeatherImportRequest.ofApi(bounds, day, start, end));
    }

    private ImportReport importUsing(final String areaCodeValue, final SourceType sourceType,
            final String dataSourceValue,
            final java.util.function.Function<GeographicBoundary, WeatherImportRequest> requestBuilder) {
        authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.WEATHER_PERSON);

        final AreaCode areaCode = AreaCode.valueOf(areaCodeValue);
        final AirControlArea area = airControlAreaRepository.findByCode(areaCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Air control area not found: " + areaCodeValue));

        final DataSource dataSource = new DataSource(dataSourceValue);
        final WeatherDataSourceReader reader = readerFactory.readerFor(sourceType);
        final WeatherImportRequest request = requestBuilder.apply(area.boundary());

        final List<RawWeatherReading> readings;
        try {
            readings = reader.read(request);
        } catch (final IOException e) {
            throw new UncheckedIOException("Could not read weather source", e);
        }

        final List<RowError> errors = new ArrayList<>();
        int imported = 0;
        if (transaction != null) {
            transaction.beginTransaction();
        }
        for (final RawWeatherReading reading : readings) {
            try {
                weatherDataRepository.save(assemble(areaCode, dataSource, reading));
                imported++;
            } catch (final IllegalArgumentException ex) {
                errors.add(new RowError(reading.sourceLine(), ex.getMessage()));
            }
        }
        if (transaction != null) {
            transaction.commit();
        }
        return new ImportReport(imported, errors);
    }

    private WeatherData assemble(final AreaCode areaCode, final DataSource dataSource,
            final RawWeatherReading r) {
        final WeatherZone zone = new WeatherZone(
                new Coordinates(r.swLat(), r.swLon()),
                new Coordinates(r.neLat(), r.neLon()),
                new AltitudeBand(r.altLowerFeet(), r.altUpperFeet()));
        final TimePeriod period = new TimePeriod(r.start(), r.end());
        return new WeatherData(areaCode, period, zone,
                new WeatherWindSpeed(r.windSpeedKnots()),
                new WeatherWindDirection(r.windDirectionDegrees()),
                dataSource);
    }
}