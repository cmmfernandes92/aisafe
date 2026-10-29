package eapli.aisafe.airportmanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircontrolareamanagement.domain.Coordinates;
import eapli.aisafe.countrymanagement.domain.CountryCode;

public class AirportTest {

    private Airport validAirport() {
        return new Airport(new AirportICAOCode("LPPT"), new AirportIATACode("LIS"),
                new AirportName("Humberto Delgado"), new Town("Lisbon"),
                new Elevation(114.0), new Coordinates(38.7742, -9.1342),
                new CountryCode("351"), new AreaCode("LPPC"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAirportRejectsNullICAOCode() {
        new Airport(null, new AirportIATACode("LIS"),
                new AirportName("Humberto Delgado"), new Town("Lisbon"),
                new Elevation(114.0), new Coordinates(38.7742, -9.1342),
                new CountryCode("351"), new AreaCode("LPPC"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAirportRejectsNullIATACode() {
        new Airport(new AirportICAOCode("LPPT"), null,
                new AirportName("Humberto Delgado"), new Town("Lisbon"),
                new Elevation(114.0), new Coordinates(38.7742, -9.1342),
                new CountryCode("351"), new AreaCode("LPPC"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAirportRejectsNullCoordinates() {
        new Airport(new AirportICAOCode("LPPT"), new AirportIATACode("LIS"),
                new AirportName("Humberto Delgado"), new Town("Lisbon"),
                new Elevation(114.0), null, new CountryCode("351"), new AreaCode("LPPC"));
    }

    @Test
    public void ensureAirportAcceptsValidInput() {
        final Airport airport = validAirport();

        assertEquals(new AirportICAOCode("LPPT"), airport.identity());
        assertEquals(new AirportIATACode("LIS"), airport.iataCode());
        assertEquals(new AirportName("Humberto Delgado"), airport.airportName());
        assertEquals(new Town("Lisbon"), airport.town());
        assertEquals(new Elevation(114.0), airport.elevation());
        assertEquals(new Coordinates(38.7742, -9.1342), airport.coordinates());
        assertEquals(new CountryCode("351"), airport.countryCode());
        assertEquals(new AreaCode("LPPC"), airport.areaCode());
    }

    @Test
    public void ensureAirportICAOCodeAliasesIdentity() {
        final Airport airport = validAirport();

        assertEquals(airport.identity(), airport.icaoCode());
    }

    @Test
    public void ensureAirportSameAsPassesForSameICAOCode() {
        final Airport one = validAirport();
        final Airport another = new Airport(new AirportICAOCode("LPPT"), new AirportIATACode("OPO"),
                new AirportName("Other"), new Town("Porto"), new Elevation(69.0),
                new Coordinates(41.2356, -8.6781), new CountryCode("351"), new AreaCode("LPPC"));

        assertTrue(one.sameAs(another));
    }

    @Test
    public void ensureAirportSameAsFailsForDifferentICAOCode() {
        final Airport one = validAirport();
        final Airport another = new Airport(new AirportICAOCode("LPPR"), new AirportIATACode("OPO"),
                new AirportName("Other"), new Town("Porto"), new Elevation(69.0),
                new Coordinates(39.0, -9.0), new CountryCode("351"), new AreaCode("LPPC"));

        assertFalse(one.sameAs(another));
    }

    @Test
    public void ensureAirportEqualsPassesForSameICAOCode() {
        assertTrue(validAirport().equals(validAirport()));
    }

    @Test
    public void ensureAirportHashCodeConsistentForSameICAOCode() {
        assertEquals(validAirport().hashCode(), validAirport().hashCode());
    }

    @Test
    public void ensureAirportToStringContainsCodesAndName() {
        final String text = validAirport().toString();

        assertTrue(text.contains("LPPT"));
        assertTrue(text.contains("LIS"));
        assertTrue(text.contains("Humberto Delgado"));
    }

    @Test
    public void ensureAirportProtectedConstructorExistsForORM() {
        assertTrue(new Airport() instanceof Airport);
    }
}
