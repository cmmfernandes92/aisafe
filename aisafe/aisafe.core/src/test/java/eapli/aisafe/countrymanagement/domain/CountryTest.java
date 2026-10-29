package eapli.aisafe.countrymanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CountryTest {

    @Test(expected = IllegalArgumentException.class)
    public void ensureCountryRejectsNullCode() {
        new Country(null, new CountryName("Portugal"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCountryRejectsNullName() {
        new Country(new CountryCode("351"), null);
    }

    @Test
    public void ensureCountryAcceptsValidInput() {
        final CountryCode code = new CountryCode("351");
        final CountryName name = new CountryName("Portugal");

        final Country country = new Country(code, name);

        assertEquals(code, country.identity());
        assertEquals(code, country.code());
        assertEquals(name, country.name());
    }

    @Test
    public void ensureCountryAllowsNameChange() {
        final Country country = Country.valueOf("351", "Portugal");
        final CountryName newName = new CountryName("Portuguese Republic");

        country.changeName(newName);

        assertEquals(newName, country.name());
    }

    @Test
    public void ensureCountrySameAsPassesForSameCode() {
        assertTrue(Country.valueOf("351", "Portugal").sameAs(Country.valueOf("351", "Other")));
    }

    @Test
    public void ensureCountrySameAsFailsForDifferentCode() {
        assertFalse(Country.valueOf("351", "Portugal").sameAs(Country.valueOf("34", "Spain")));
    }

    @Test
    public void ensureCountryEqualsPassesForSameCode() {
        assertTrue(Country.valueOf("351", "Portugal").equals(Country.valueOf("351", "Other")));
    }

    @Test
    public void ensureCountryHashCodeConsistentForSameCode() {
        assertEquals(Country.valueOf("351", "Portugal").hashCode(),
                Country.valueOf("351", "Other").hashCode());
    }

    @Test
    public void ensureCountryToStringContainsNameAndCode() {
        final String text = Country.valueOf("351", "Portugal").toString();

        assertTrue(text.contains("Portugal"));
        assertTrue(text.contains("351"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCountryRejectsNullNameChange() {
        Country.valueOf("351", "Portugal").changeName(null);
    }

    @Test
    public void ensureCountryProtectedConstructorExistsForORM() {
        assertTrue(new Country() instanceof Country);
    }
}
