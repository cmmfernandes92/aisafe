package eapli.aisafe.app.backoffice.console.presentation.airportmanagement;

import eapli.aisafe.aircontrolareamanagement.domain.AirControlArea;
import eapli.aisafe.aircontrolareamanagement.repositories.AirControlAreaRepository;
import eapli.aisafe.airportmanagement.application.RegisterAirportController;
import eapli.aisafe.airportmanagement.domain.Airport;
import eapli.aisafe.airportmanagement.repositories.AirportRepository;
import eapli.aisafe.countrymanagement.domain.Country;
import eapli.aisafe.countrymanagement.repositories.CountryRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class RegisterAirportUI extends AbstractUI {

    private final AirportRepository airportRepository = PersistenceContext.repositories().airports();
    private final AirControlAreaRepository airControlAreaRepository =
            PersistenceContext.repositories().airControlAreas();
    private final CountryRepository countryRepository = PersistenceContext.repositories().countries();

    private final RegisterAirportController controller = new RegisterAirportController(
            airportRepository, airControlAreaRepository, countryRepository);

    @Override
    protected boolean doShow() {
        showReferenceData();

        final String icaoCode = Console.readNonEmptyLine("ICAO Code", "ICAO code must not be blank.");
        final String iataCode = Console.readNonEmptyLine("IATA Code", "IATA code must not be blank.");
        final String airportName = Console.readNonEmptyLine("Airport Name", "Airport name must not be blank.");
        final String town = Console.readNonEmptyLine("Town", "Town must not be blank.");
        final String countryCode = Console.readNonEmptyLine("Country Code", "Country code must not be blank.");
        final String areaCode = Console.readNonEmptyLine("Air Control Area Code", "Area code must not be blank.");

        try {
            final double elevation = parseDouble(Console.readLine("Elevation in metres"), "Elevation");
            final double latitude = parseDouble(Console.readLine("Latitude"), "Latitude");
            final double longitude = parseDouble(Console.readLine("Longitude"), "Longitude");

            final Airport airport = controller.registerAirport(icaoCode, iataCode, airportName, town,
                    elevation, latitude, longitude, countryCode, areaCode);
            System.out.println("Airport registered: " + airport);
            return true;
        } catch (final IllegalArgumentException ex) {
            System.out.println("Registration failed: " + ex.getMessage());
            return false;
        }
    }

    private void showReferenceData() {
        System.out.println("Available countries:");
        for (final Country country : countryRepository.findAll()) {
            System.out.println("  " + country.code() + " - " + country.name());
        }
        System.out.println("Available air control areas:");
        for (final AirControlArea area : airControlAreaRepository.findAll()) {
            System.out.println("  " + area.code() + " - " + area.name());
        }
    }

    private double parseDouble(final String rawValue, final String fieldName) {
        try {
            return Double.parseDouble(rawValue);
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid number");
        }
    }

    @Override
    public String headline() {
        return "Register Airport";
    }
}
