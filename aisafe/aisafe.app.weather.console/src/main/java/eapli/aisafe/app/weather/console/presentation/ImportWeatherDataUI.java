package eapli.aisafe.app.weather.console.presentation;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;

import eapli.aisafe.aircontrolareamanagement.domain.AirControlArea;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.weather.application.ImportBulkWeatherDataController;
import eapli.aisafe.weather.application.importing.ImportReport;
import eapli.aisafe.weather.application.importing.RowError;
import eapli.aisafe.weather.application.importing.SourceType;
import eapli.aisafe.weather.application.importing.WeatherDataSourceReaderFactory;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class ImportWeatherDataUI extends AbstractUI {

    private final ImportBulkWeatherDataController controller = buildController();

    private static ImportBulkWeatherDataController buildController() {
        final TransactionalContext tx = PersistenceContext.repositories().newTransactionalContext();
        return new ImportBulkWeatherDataController(
                PersistenceContext.repositories().weatherData(tx),
                PersistenceContext.repositories().airControlAreas(),
                new WeatherDataSourceReaderFactory(),
                tx);
    }

    @Override
    protected boolean doShow() {
        System.out.println("\nAvailable Air Control Areas:");
        for (final AirControlArea area : controller.listAirControlAreas()) {
            System.out.printf("  %-8s  %s%n", area.code(), area.name());
        }

        final String areaCode = Console.readNonEmptyLine("Target Area Code", "Area code must not be blank.");
        final String dataSource = Console.readNonEmptyLine("Data Source (provider)", "Data source must not be blank.");

        System.out.println("\nSource types:");
        System.out.println("  1. CSV file");
        System.out.println("  2. Open-Meteo API");
        final int choice = Console.readInteger("Source type");

        try {
            if (choice == 2) {
                return importFromOpenMeteo(areaCode, dataSource);
            }
            return importFromCsv(areaCode, dataSource);
        } catch (final IllegalArgumentException | IllegalStateException e) {
            System.out.println("Import failed: " + e.getMessage());
            return false;
        }
    }

    private boolean importFromCsv(final String areaCode, final String dataSource) {
        final String path = Console.readNonEmptyLine("CSV file path", "File path must not be blank.");
        try (Reader source = Files.newBufferedReader(Path.of(path.trim()))) {
            printReport(controller.importFrom(areaCode, SourceType.CSV, dataSource, source));
            return true;
        } catch (final IOException e) {
            System.out.println("Could not open file: " + e.getMessage());
            return false;
        }
    }

    private boolean importFromOpenMeteo(final String areaCode, final String dataSource) {
        final LocalDate day = LocalDate.parse(
                Console.readNonEmptyLine("Day (yyyy-MM-dd)", "Day must not be blank."));
        final LocalTime start = LocalTime.parse(
                Console.readNonEmptyLine("Start time (HH:mm)", "Start time must not be blank."));
        final LocalTime end = LocalTime.parse(
                Console.readNonEmptyLine("End time (HH:mm)", "End time must not be blank."));
        printReport(controller.importFrom(areaCode, SourceType.OPEN_METEO, dataSource, day, start, end));
        return true;
    }

    private void printReport(final ImportReport report) {
        System.out.println("\nImport finished: " + report.imported() + " imported, "
                + report.skipped() + " skipped.");
        for (final RowError error : report.errors()) {
            System.out.println("  skipped " + error);
        }
    }

    @Override
    public String headline() {
        return "Import Bulk Weather Data";
    }
}