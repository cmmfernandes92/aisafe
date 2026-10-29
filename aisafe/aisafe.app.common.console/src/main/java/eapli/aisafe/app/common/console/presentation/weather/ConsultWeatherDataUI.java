package eapli.aisafe.app.common.console.presentation.weather;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import eapli.aisafe.aircontrolareamanagement.domain.AirControlArea;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.weather.application.ConsultWeatherDataController;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class ConsultWeatherDataUI extends AbstractUI {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ConsultWeatherDataController controller = new ConsultWeatherDataController(
            PersistenceContext.repositories().weatherData(),
            PersistenceContext.repositories().airControlAreas());

    @Override
    protected boolean doShow() {
        System.out.println("\nAvailable Air Control Areas:");
        for (final AirControlArea area : controller.listAirControlAreas()) {
            System.out.printf("  %-8s  %s%n", area.code(), area.name());
        }

        final String areaCode = Console.readNonEmptyLine("Area Code", "Area code must not be blank.");
        final LocalDate day = readDate("Day (yyyy-MM-dd)");

        try {
            final Iterable<WeatherData> results = controller.consultWeatherData(areaCode, day);
            displayResults(results);
            return true;
        } catch (final IllegalArgumentException ex) {
            System.out.println("Error: " + ex.getMessage());
            return false;
        }
    }

    private LocalDate readDate(final String prompt) {
        while (true) {
            final String raw = Console.readNonEmptyLine(prompt, "Date must not be blank.");
            try {
                return LocalDate.parse(raw.trim(), DATE_FORMAT);
            } catch (final DateTimeParseException e) {
                System.out.println("Invalid format — expected: yyyy-MM-dd");
            }
        }
    }

    private void displayResults(final Iterable<WeatherData> results) {
        boolean hasData = false;
        for (final WeatherData wd : results) {
            if (!hasData) {
                System.out.println();
                System.out.printf("  %-36s  %-8s  %7s  %9s  %8s  %8s  %6s  %s%n",
                        "Period", "Area", "Temp°C", "Pres hPa", "Vis m", "Wind kt", "Dir°", "Source");
                System.out.println("  " + "-".repeat(100));
                hasData = true;
            }
            System.out.printf("  %s → %s  %-8s  %7.1f  %9.1f  %8.0f  %8.1f  %6.0f  %s%n",
                    wd.timePeriod().startTime(),
                    wd.timePeriod().endTime(),
                    wd.areaCode(),
                    wd.temperature(),
                    wd.pressure(),
                    wd.visibility(),
                    wd.windSpeed().value(),
                    wd.windDirection().value(),
                    wd.dataSource());
        }
        if (!hasData) {
            System.out.println("\nNo weather data available for the selected area and day.");
        }
    }

    @Override
    public String headline() {
        return "Consult Weather Data";
    }
}
