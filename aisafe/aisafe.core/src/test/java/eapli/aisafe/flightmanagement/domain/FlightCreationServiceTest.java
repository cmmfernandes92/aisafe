package eapli.aisafe.flightmanagement.domain;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircontrolareamanagement.domain.Coordinates;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModel;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModelName;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftType;
import eapli.aisafe.airportmanagement.domain.Airport;
import eapli.aisafe.airportmanagement.domain.AirportIATACode;
import eapli.aisafe.airportmanagement.domain.AirportICAOCode;
import eapli.aisafe.airportmanagement.domain.AirportName;
import eapli.aisafe.airportmanagement.domain.Elevation;
import eapli.aisafe.airportmanagement.domain.Town;
import eapli.aisafe.airtransportcompanymanagement.domain.AirTransportCompany;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyICAOCode;
import eapli.aisafe.countrymanagement.domain.CountryCode;
import eapli.aisafe.enginemodelmanagement.domain.EngineModel;
import eapli.aisafe.enginemodelmanagement.domain.EngineModelName;
import eapli.aisafe.enginemodelmanagement.domain.FuelType;
import eapli.aisafe.enginemodelmanagement.domain.MotorizationType;
import eapli.aisafe.fleetmanagement.domain.Aircraft;
import eapli.aisafe.fleetmanagement.domain.CabinConfiguration;
import eapli.aisafe.fleetmanagement.domain.Country;
import eapli.aisafe.fleetmanagement.domain.Registration;
import eapli.aisafe.fleetmanagement.domain.SeatClass;
import eapli.aisafe.flightroutemanagement.domain.FlightRoute;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.makermanagement.domain.Maker;
import eapli.aisafe.pilotmanagement.domain.Pilot;
import eapli.aisafe.usermanagement.domain.Email;

/**
 * Unit tests for the US080 {@link FlightCreationService}: the cross-aggregate
 * preconditions (I2, I3, I5, I6) and the happy-path assembly of a {@code DRAFT}
 * flight. I6 (route deactivated as of the departure date) uses US074's date-based
 * route deactivation: a departure on or after the cutoff is rejected, one strictly
 * before it is allowed.
 */
public class FlightCreationServiceTest {

    private static final CompanyIATACode TP = CompanyIATACode.valueOf("TP");
    private static final CompanyIATACode FR = CompanyIATACode.valueOf("FR");
    private static final RouteName ROUTE = RouteName.valueOf("TP123");
    private static final Registration REG = Registration.valueOf("CS-TUA");
    private static final Email PILOT_EMAIL = Email.valueOf("pilot@tap.pt");
    private static final LocalDateTime DEPARTURE = LocalDateTime.parse("2026-07-01T09:30");

    private final FlightCreationService service = new FlightCreationService();
    private final AircraftModel a320 = model("A320neo", "Airbus");

    @Test
    public void ensureCreateDraftHappyPath() {
        final Flight flight = service.createDraft(routeFor(TP), pilot(TP, a320),
                aircraft(TP, a320), a320, DEPARTURE);

        assertEquals(FlightPlanStatus.DRAFT, flight.currentPlan().status());
        assertEquals(ROUTE, flight.route());
        assertEquals(PILOT_EMAIL, flight.pilot());
        assertEquals(REG, flight.aircraft());
        assertEquals(DEPARTURE, flight.departure());
        assertEquals(new FlightId(ROUTE, DEPARTURE.toLocalDate()), flight.identity());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCreateDraftRejectsPilotFromAnotherCompany() {
        service.createDraft(routeFor(TP), pilot(FR, a320), aircraft(TP, a320), a320, DEPARTURE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCreateDraftRejectsAircraftFromAnotherCompany() {
        service.createDraft(routeFor(TP), pilot(TP, a320), aircraft(FR, a320), a320, DEPARTURE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCreateDraftRejectsRetiredAircraft() {
        final Aircraft retired = aircraft(TP, a320);
        retired.decommission();
        service.createDraft(routeFor(TP), pilot(TP, a320), retired, a320, DEPARTURE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCreateDraftRejectsPilotNotCertifiedForAircraftModel() {
        final AircraftModel b787 = model("787-9", "Boeing");
        service.createDraft(routeFor(TP), pilot(TP, b787), aircraft(TP, a320), a320, DEPARTURE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCreateDraftRejectsRouteDeactivatedOnDepartureDate() {
        final FlightRoute route = routeFor(TP);
        route.deactivate(DEPARTURE.toLocalDate()); // cutoff == departure date → rejected (US074.3)
        service.createDraft(route, pilot(TP, a320), aircraft(TP, a320), a320, DEPARTURE);
    }

    @Test
    public void ensureCreateDraftSucceedsWhenRouteDeactivatedAfterDeparture() {
        final FlightRoute route = routeFor(TP);
        route.deactivate(DEPARTURE.toLocalDate().plusDays(1)); // cutoff after departure → allowed (US074.4)

        final Flight flight = service.createDraft(route, pilot(TP, a320), aircraft(TP, a320), a320, DEPARTURE);

        assertEquals(FlightPlanStatus.DRAFT, flight.currentPlan().status());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCreateDraftRejectsNullArguments() {
        service.createDraft(routeFor(TP), pilot(TP, a320), aircraft(TP, a320), a320, null);
    }

    // ---- fixtures ----

    private FlightRoute routeFor(final CompanyIATACode companyCode) {
        final AirTransportCompany company = new AirTransportCompany(companyCode,
                CompanyICAOCode.valueOf("TAP"), "Carrier " + companyCode);
        return new FlightRoute(ROUTE, company, lisbon(), porto());
    }

    private Pilot pilot(final CompanyIATACode companyCode, final AircraftModel... certifiedModels) {
        return new Pilot(PILOT_EMAIL, companyCode, Set.of(certifiedModels));
    }

    private Aircraft aircraft(final CompanyIATACode companyCode, final AircraftModel ignoredModel) {
        return new Aircraft(REG, 1L, companyCode,
                new CabinConfiguration(Map.of(SeatClass.ECONOMY, 150)),
                new Country("Portugal"), 0);
    }

    private static AircraftModel model(final String name, final String makerName) {
        final Maker maker = Maker.valueOf(makerName);
        final EngineModel engine = new EngineModel(EngineModelName.valueOf(name + "-engine"), maker,
                MotorizationType.TURBOFAN, FuelType.JET_A, 120000, 0.54);
        return new AircraftModel(AircraftModelName.valueOf(name), maker, AircraftType.PASSENGER,
                MotorizationType.TURBOFAN, 42600, 79000, 64300, 26730, 12131, 833, 122.6,
                0.024, 1.5, 6300, Set.of(engine));
    }

    private static Airport lisbon() {
        return new Airport(new AirportICAOCode("LPPT"), new AirportIATACode("LIS"),
                new AirportName("Humberto Delgado"), new Town("Lisbon"),
                new Elevation(114.0), new Coordinates(38.7742, -9.1342),
                new CountryCode("351"), new AreaCode("LPPC"));
    }

    private static Airport porto() {
        return new Airport(new AirportICAOCode("LPPR"), new AirportIATACode("OPO"),
                new AirportName("Francisco Sa Carneiro"), new Town("Porto"),
                new Elevation(69.0), new Coordinates(41.2356, -8.6781),
                new CountryCode("351"), new AreaCode("LPPC"));
    }
}