package eapli.aisafe.airtransportcompanymanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AirTransportCompanyTest {

    private AirTransportCompany validCompany() {
        return new AirTransportCompany(
                CompanyIATACode.valueOf("TP"),
                CompanyICAOCode.valueOf("TAP"),
                "TAP Air Portugal");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCompanyRejectsNullIataCode() {
        new AirTransportCompany(null, CompanyICAOCode.valueOf("TAP"), "TAP Air Portugal");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCompanyRejectsNullIcaoCode() {
        new AirTransportCompany(CompanyIATACode.valueOf("TP"), null, "TAP Air Portugal");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCompanyRejectsBlankName() {
        new AirTransportCompany(CompanyIATACode.valueOf("TP"), CompanyICAOCode.valueOf("TAP"), "   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCompanyRejectsNullName() {
        new AirTransportCompany(CompanyIATACode.valueOf("TP"), CompanyICAOCode.valueOf("TAP"), null);
    }

    @Test
    public void ensureCompanyAcceptsValidInput() {
        final AirTransportCompany company = validCompany();
        assertEquals(CompanyIATACode.valueOf("TP"), company.identity());
        assertEquals(CompanyICAOCode.valueOf("TAP"), company.icaoCode());
        assertEquals("TAP Air Portugal", company.companyName());
    }

    @Test
    public void ensureCompanyEqualsForSameIataCode() {
        final AirTransportCompany one = validCompany();
        final AirTransportCompany another = new AirTransportCompany(
                CompanyIATACode.valueOf("TP"), CompanyICAOCode.valueOf("TPX"), "Other Name");
        assertTrue(one.equals(another));
    }

    @Test
    public void ensureCompanyNotEqualsForDifferentIataCode() {
        final AirTransportCompany one = validCompany();
        final AirTransportCompany another = new AirTransportCompany(
                CompanyIATACode.valueOf("FR"), CompanyICAOCode.valueOf("RYR"), "Ryanair");
        assertFalse(one.equals(another));
    }

    @Test
    public void ensureCompanySameAsForSameIataCode() {
        final AirTransportCompany another = new AirTransportCompany(
                CompanyIATACode.valueOf("TP"), CompanyICAOCode.valueOf("TPX"), "Other Name");
        assertTrue(validCompany().sameAs(another));
    }

    @Test
    public void ensureCompanyToStringIsNotEmpty() {
        assertFalse(validCompany().toString().isEmpty());
    }

    @Test
    public void ensureIataCodeGetterReturnsCorrectValue() {
        assertEquals(CompanyIATACode.valueOf("TP"), validCompany().iataCode());
    }

    @Test
    public void ensureHashCodeIsConsistent() {
        final AirTransportCompany c = validCompany();
        assertEquals(c.hashCode(), c.hashCode());
    }
}
