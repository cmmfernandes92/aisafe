package eapli.aisafeweb.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import eapli.aisafe.aircontrolareamanagement.domain.AirControlArea;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.weather.application.ConsultWeatherDataController;
import eapli.aisafe.weather.domain.WeatherData;

@Service
public class WeatherConsultService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public List<AreaOption> areas() {
        return toList(controller().listAirControlAreas()).stream()
                .sorted(Comparator.comparing(area -> area.code().toString()))
                .map(WeatherConsultService::toAreaOption)
                .toList();
    }

    public List<WeatherRow> consult(final String areaCode, final LocalDate day) {
        if (areaCode == null || areaCode.isBlank()) {
            throw new IllegalArgumentException("Select an air control area.");
        }
        if (day == null) {
            throw new IllegalArgumentException("Select a day.");
        }

        return toList(controller().consultWeatherData(areaCode.trim(), day)).stream()
                .sorted(Comparator.comparing((WeatherData data) -> data.timePeriod().startTime())
                        .thenComparing(data -> data.timePeriod().endTime())
                        .thenComparing(data -> data.areaCode().toString()))
                .map(WeatherConsultService::toWeatherRow)
                .toList();
    }

    private ConsultWeatherDataController controller() {
        return new ConsultWeatherDataController(
                PersistenceContext.repositories().weatherData(),
                PersistenceContext.repositories().airControlAreas());
    }

    private static AreaOption toAreaOption(final AirControlArea area) {
        return new AreaOption(area.code().toString(), area.name().toString());
    }

    private static WeatherRow toWeatherRow(final WeatherData data) {
        return new WeatherRow(
                DATE_TIME.format(data.timePeriod().startTime()) + " -> "
                        + DATE_TIME.format(data.timePeriod().endTime()),
                data.areaCode().toString(),
                optional(data.temperature(), 1),
                optional(data.pressure(), 1),
                optional(data.visibility(), 0),
                format(data.windSpeed().value(), 1),
                format(data.windDirection().value(), 0),
                data.dataSource().value());
    }

    private static String optional(final Double value, final int decimals) {
        return value == null ? "-" : format(value, decimals);
    }

    private static String format(final double value, final int decimals) {
        return String.format(Locale.US, "%." + decimals + "f", value);
    }

    private static <T> List<T> toList(final Iterable<T> source) {
        final List<T> result = new ArrayList<>();
        source.forEach(result::add);
        return result;
    }

    public record AreaOption(String code, String name) {
    }

    public record WeatherRow(
            String period,
            String areaCode,
            String temperature,
            String pressure,
            String visibility,
            String windSpeed,
            String windDirection,
            String dataSource) {
    }
}
