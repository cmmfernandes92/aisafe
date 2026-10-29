package eapli.aisafe.app.backoffice.console.presentation.airtransportcompany;

import eapli.aisafe.airtransportcompanymanagement.application.RegisterAirTransportCompanyController;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class RegisterAirTransportCompanyUI extends AbstractUI {

    private final RegisterAirTransportCompanyController controller = new RegisterAirTransportCompanyController(
            PersistenceContext.repositories().airTransportCompanies());

    @Override
    protected boolean doShow() {
        header();

        section("ICAO / IATA Codes");
        System.out.println("  IATA code: 2-letter designator  (e.g. TP, AA, LH)");
        final String iataCode = Console.readLine("  IATA code");
        System.out.println();
        System.out.println("  ICAO code: 3-letter designator  (e.g. TAP, AAL, DLH)");
        final String icaoCode = Console.readLine("  ICAO code");

        section("Company Details");
        final String companyName = Console.readLine("  Company name");

        section("Summary");
        System.out.printf("  %-18s %s%n", "IATA code:",    iataCode.toUpperCase());
        System.out.printf("  %-18s %s%n", "ICAO code:",    icaoCode.toUpperCase());
        System.out.printf("  %-18s %s%n", "Company name:", companyName);
        System.out.println();

        final String confirm = Console.readLine("  Confirm registration? (y/n)");
        if (!"y".equalsIgnoreCase(confirm.trim())) {
            System.out.println("\n  Registration cancelled.\n");
            return false;
        }

        try {
            controller.register(iataCode, icaoCode, companyName);
            ok("Air transport company \"" + companyName + "\" registered successfully.");
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            error("IATA or ICAO code already in use — this company is already registered.");
        } catch (final IllegalArgumentException e) {
            error("Validation failed: " + e.getMessage());
        }
        return false;
    }

    private void header() {
        System.out.println();
        System.out.println("  ┌" + "─".repeat(50) + "┐");
        System.out.printf("  │  %-48s│%n", "REGISTER AIR TRANSPORT COMPANY");
        System.out.println("  └" + "─".repeat(50) + "┘");
    }

    private void section(final String title) {
        System.out.println();
        final String label = "── " + title + " ";
        System.out.println("  " + label + "─".repeat(Math.max(0, 50 - label.length())));
    }

    private void ok(final String msg) {
        System.out.println();
        System.out.println("  [ OK ]  " + msg);
        System.out.println();
    }

    private void error(final String msg) {
        System.out.println();
        System.out.println("  [FAIL]  " + msg);
        System.out.println();
    }

    @Override
    public String headline() {
        return "Register Air Transport Company";
    }
}
