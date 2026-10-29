package eapli.aisafe.app.pilot.console.presentation.flight;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import eapli.aisafe.flightmanagement.application.CreateFlightController;
import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.fleetmanagement.domain.Registration;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

/**
 * US080 - pilot console UI to create a flight (route + aircraft + departure) and
 * its initial {@code DRAFT} flight plan. No DSL body is captured here; the draft
 * is filled later by the file import (US121).
 */
public class CreateFlightUI extends AbstractUI {

    private final CreateFlightController controller = new CreateFlightController(
            PersistenceContext.repositories().flights(),
            PersistenceContext.repositories().flightRoutes(),
            PersistenceContext.repositories().aircraft(),
            PersistenceContext.repositories().aircraftModels(),
            PersistenceContext.repositories().pilots(),
            PersistenceContext.repositories().aisafeUsers());

    @Override
    protected boolean doShow() {
        header();

        final RouteName routeName;
        try {
            routeName = RouteName.valueOf(Console.readLine("  Route name (e.g. TP123):").trim());
        } catch (final RuntimeException e) {
            error("Invalid route name: " + e.getMessage());
            return false;
        }

        final Registration registration;
        try {
            registration = Registration.valueOf(Console.readLine("  Aircraft registration (e.g. CS-TUA):").trim());
        } catch (final RuntimeException e) {
            error("Invalid aircraft registration: " + e.getMessage());
            return false;
        }

        final LocalDateTime departure = readDeparture();
        if (departure == null) {
            return false;
        }

        try {
            final Flight flight = controller.createDraftFlight(routeName, departure, registration);
            System.out.println();
            System.out.printf("  [ OK ]  Flight created for %s on %s — initial plan is %s.%n%n",
                    flight.route(), flight.identity().departureDate(), flight.currentPlan().status());
        } catch (final ConcurrencyException | IllegalArgumentException | IllegalStateException e) {
            error(e.getMessage());
        }
        return false;
    }

    private LocalDateTime readDeparture() {
        final LocalDate date;
        try {
            date = LocalDate.parse(Console.readLine("  Departure date (yyyy-MM-dd):").trim());
        } catch (final DateTimeParseException e) {
            error("Invalid date — expected format yyyy-MM-dd.");
            return null;
        }
        final LocalTime time;
        try {
            time = LocalTime.parse(Console.readLine("  Departure time (HH:mm):").trim());
        } catch (final DateTimeParseException e) {
            error("Invalid time — expected format HH:mm.");
            return null;
        }
        return LocalDateTime.of(date, time);
    }

    private void header() {
        System.out.println();
        System.out.println("  ┌" + "─".repeat(50) + "┐");
        System.out.printf("  │  %-48s│%n", "CREATE FLIGHT");
        System.out.println("  └" + "─".repeat(50) + "┘");
        System.out.println();
    }

    private void error(final String msg) {
        System.out.println();
        System.out.println("  [FAIL]  " + msg);
        System.out.println();
    }

    @Override
    public String headline() {
        return "Create Flight";
    }
}