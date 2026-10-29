package eapli.aisafe.fleetmanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.EnumMap;
import java.util.Map;

import org.junit.Test;

import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;

public class AircraftTest {

    private static final Long MODEL_ID = 1L;
    private static final CompanyIATACode COMPANY_ID = CompanyIATACode.valueOf("TP");

    private CabinConfiguration validCabinConfiguration() {
        final Map<SeatClass, Integer> seats = new EnumMap<>(SeatClass.class);
        seats.put(SeatClass.ECONOMY, 150);
        seats.put(SeatClass.BUSINESS, 20);
        return new CabinConfiguration(seats);
    }

    private Aircraft validAircraft() {
        return new Aircraft(
                new Registration("CS-TNA"),
                MODEL_ID,
                COMPANY_ID,
                validCabinConfiguration(),
                new Country("Portugal"),
                300);
    }

    @Test
    public void ensureAircraftAcceptsValidInput() {
        final Aircraft aircraft = validAircraft();
        assertNotNull(aircraft);
        assertEquals("CS-TNA", aircraft.registration().value());
    }

    @Test
    public void ensureAircraftInitialStatusIsActive() {
        final Aircraft aircraft = validAircraft();
        assertEquals(OperationalStatus.ACTIVE, aircraft.status());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAircraftRejectsNullRegistration() {
        new Aircraft(null, MODEL_ID, COMPANY_ID, validCabinConfiguration(), new Country("Portugal"), 300);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAircraftRejectsNullModelId() {
        new Aircraft(new Registration("CS-TNA"), null, COMPANY_ID, validCabinConfiguration(),
                new Country("Portugal"), 300);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAircraftRejectsNullCompanyCode() {
        new Aircraft(new Registration("CS-TNA"), MODEL_ID, null, validCabinConfiguration(),
                new Country("Portugal"), 300);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAircraftRejectsTotalSeatsExceedingModelCapacity() {
        final Map<SeatClass, Integer> seats = new EnumMap<>(SeatClass.class);
        seats.put(SeatClass.ECONOMY, 200);
        seats.put(SeatClass.BUSINESS, 50);
        new Aircraft(
                new Registration("CS-TUK"),
                MODEL_ID,
                COMPANY_ID,
                new CabinConfiguration(seats),
                new Country("Portugal"),
                180);
    }

    @Test
    public void ensureAircraftAllowsTotalSeatsEqualToCapacity() {
        final Map<SeatClass, Integer> seats = new EnumMap<>(SeatClass.class);
        seats.put(SeatClass.ECONOMY, 150);
        seats.put(SeatClass.BUSINESS, 30);
        final Aircraft aircraft = new Aircraft(
                new Registration("CS-TNB"),
                MODEL_ID,
                COMPANY_ID,
                new CabinConfiguration(seats),
                new Country("Portugal"),
                180);
        assertNotNull(aircraft);
    }

    @Test
    public void ensureAircraftSkipsCapacityCheckWhenMaxIsZero() {
        final Map<SeatClass, Integer> seats = new EnumMap<>(SeatClass.class);
        seats.put(SeatClass.ECONOMY, 500);
        final Aircraft aircraft = new Aircraft(
                new Registration("CS-TNC"),
                MODEL_ID,
                COMPANY_ID,
                new CabinConfiguration(seats),
                new Country("Portugal"),
                0);
        assertNotNull(aircraft);
    }

    @Test
    public void ensureDecommissionChangesStatusToRetired() {
        final Aircraft aircraft = validAircraft();
        aircraft.decommission();
        assertEquals(OperationalStatus.RETIRED, aircraft.status());
    }

    @Test(expected = IllegalStateException.class)
    public void ensureDecommissionRejectsAlreadyRetiredAircraft() {
        final Aircraft aircraft = validAircraft();
        aircraft.decommission();
        aircraft.decommission();
    }
}
