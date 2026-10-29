package eapli.aisafe.weather.application.importing;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

import eapli.aisafe.weather.application.importing.csv.CsvWeatherDataSourceReader;
import eapli.aisafe.weather.application.importing.openmeteo.OpenMeteoWeatherDataSourceReader;

/**
 * Resolves a {@link SourceType} to its {@link WeatherDataSourceReader}. A plain,
 * instantiable object (no global state, no singleton) so it can be injected and
 * the controller stays testable: a test can supply a registry that returns a
 * stub reader to prove substitutability without changing the controller.
 */
public class WeatherDataSourceReaderFactory {

    private final Map<SourceType, Supplier<WeatherDataSourceReader>> registry;

    /** Default registry: CSV and Open-Meteo bound to their concrete readers. */
    public WeatherDataSourceReaderFactory() {
        this.registry = new EnumMap<>(SourceType.class);
        this.registry.put(SourceType.CSV, CsvWeatherDataSourceReader::new);
        this.registry.put(SourceType.OPEN_METEO, OpenMeteoWeatherDataSourceReader::new);
    }

    /** Custom registry, used for extension and tests. */
    public WeatherDataSourceReaderFactory(final Map<SourceType, Supplier<WeatherDataSourceReader>> registry) {
        this.registry = new EnumMap<>(registry);
    }

    public WeatherDataSourceReader readerFor(final SourceType sourceType) {
        if (sourceType == null) {
            throw new IllegalArgumentException("Source type must not be null");
        }
        final Supplier<WeatherDataSourceReader> supplier = this.registry.get(sourceType);
        if (supplier == null) {
            throw new IllegalArgumentException("No reader registered for source type: " + sourceType);
        }
        return supplier.get();
    }
}