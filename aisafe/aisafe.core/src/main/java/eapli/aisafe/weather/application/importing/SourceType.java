package eapli.aisafe.weather.application.importing;

/**
 * Registered weather data source types. CSV is the mandatory initial format;
 * new types (e.g. an external API) are added here and bound to a reader in the
 * {@link WeatherDataSourceReaderFactory}, without changing the import use case.
 */
public enum SourceType {
    CSV,
    OPEN_METEO
}