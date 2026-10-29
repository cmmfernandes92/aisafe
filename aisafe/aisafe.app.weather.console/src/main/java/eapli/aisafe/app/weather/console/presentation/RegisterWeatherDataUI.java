package eapli.aisafe.app.weather.console.presentation;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import eapli.aisafe.aircontrolareamanagement.domain.AirControlArea;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.weather.application.RegisterWeatherDataController;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class RegisterWeatherDataUI extends AbstractUI {

    private static final DateTimeFormatter DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final RegisterWeatherDataController controller = new RegisterWeatherDataController(
            PersistenceContext.repositories().weatherData(),
            PersistenceContext.repositories().airControlAreas());

    @Override
    protected boolean doShow() {
        System.out.println("\nAvailable Air Control Areas:");
        for (final AirControlArea area : controller.listAirControlAreas()) {
            System.out.printf("  %-8s  %s%n", area.code(), area.name());
        }

        final String areaCode = Console.readNonEmptyLine("Area Code", "Area code must not be blank.");

        final LocalDateTime start = readDateTime("Period Start (yyyy-MM-dd HH:mm)");
        if (start == null) {
            return false;
        }
        final LocalDateTime end = readDateTime("Period End   (yyyy-MM-dd HH:mm)");
        if (end == null) {
            return false;
        }

        System.out.println("Weather zone (sub-area bounding box within the area):");
        final double zoneSwLat = readDouble("  SW Latitude  (-90..90)");
        final double zoneSwLon = readDouble("  SW Longitude (-180..180)");
        final double zoneNeLat = readDouble("  NE Latitude  (-90..90)");
        final double zoneNeLon = readDouble("  NE Longitude (-180..180)");
        final double altLowerFeet = readDouble("  Altitude band lower (ft, >= 0)");
        final double altUpperFeet = readDouble("  Altitude band upper (ft, > lower)");

        final double temperature = readDouble("Temperature (°C)");
        final double pressure = readDouble("Pressure (hPa)");
        final double visibility = readDouble("Visibility (m)");
        final double windSpeed = readDouble("Wind Speed (kt, >= 0)");
        final double windDirection = readDouble("Wind Direction (0-360°)");
        final String dataSource = Console.readNonEmptyLine("Data Source", "Data source must not be blank.");

        try {
            final WeatherData result = controller.registerWeatherData(
                    areaCode, start, end,
                    zoneSwLat, zoneSwLon, zoneNeLat, zoneNeLon, altLowerFeet, altUpperFeet,
                    temperature, pressure, visibility,
                    windSpeed, windDirection, dataSource);
            System.out.println("Weather data registered: " + result);
            return true;
        } catch (final IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Registration failed: " + ex.getMessage());
            return false;
        }
    }

    private LocalDateTime readDateTime(final String prompt) {
        while (true) {
            final String raw = Console.readNonEmptyLine(prompt, "Date/time must not be blank.");
            try {
                return LocalDateTime.parse(raw.trim(), DATETIME_FORMAT);
            } catch (final DateTimeParseException e) {
                System.out.println("Invalid format — expected: yyyy-MM-dd HH:mm");
            }
        }
    }

    private double readDouble(final String prompt) {
        while (true) {
            final String raw = Console.readLine(prompt);
            try {
                return Double.parseDouble(raw.trim());
            } catch (final NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    @Override
    public String headline() {
        return "Register Weather Data";
    }
}
