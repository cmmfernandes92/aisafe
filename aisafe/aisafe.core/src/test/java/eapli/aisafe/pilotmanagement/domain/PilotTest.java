package eapli.aisafe.pilotmanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.util.Set;

import org.junit.Test;

import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModel;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModelName;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftType;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.enginemodelmanagement.domain.EngineModel;
import eapli.aisafe.enginemodelmanagement.domain.EngineModelName;
import eapli.aisafe.enginemodelmanagement.domain.FuelType;
import eapli.aisafe.enginemodelmanagement.domain.MotorizationType;
import eapli.aisafe.makermanagement.domain.Maker;
import eapli.aisafe.usermanagement.domain.Email;

public class PilotTest {

    @Test
    public void ensureCreatePilotWithValidData() {
        final Email email = Email.valueOf("pilot@tap.pt");
        final CompanyIATACode companyCode = CompanyIATACode.valueOf("TP");
        final Pilot pilot = new Pilot(email, companyCode, Set.of(sampleModel("A320neo")));

        assertEquals(email, pilot.identity());
        assertEquals(companyCode, pilot.companyCode());
        assertEquals(PilotStatus.ACTIVE, pilot.status());
        assertTrue(pilot.isActive());
        assertEquals(1, pilot.certifications().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensurePilotRequiresAtLeastOneCertification() {
        new Pilot(Email.valueOf("pilot@tap.pt"), CompanyIATACode.valueOf("TP"), Set.of());
    }

    @Test
    public void ensurePilotCanBeDeactivated() {
        // Arrange
        final Pilot pilot = new Pilot(Email.valueOf("pilot@tap.pt"), CompanyIATACode.valueOf("TP"),
                Set.of(sampleModel("A320neo")));
        assertTrue(pilot.isActive());
        // Act
        pilot.deactivate();
        // Assert
        assertFalse(pilot.isActive());
        assertEquals(PilotStatus.INACTIVE, pilot.status());
    }

    @Test(expected = IllegalStateException.class)
    public void ensurePilotCannotBeDeactivatedWhenAlreadyInactive() {
        // Arrange
        final Pilot pilot = new Pilot(Email.valueOf("pilot@tap.pt"), CompanyIATACode.valueOf("TP"),
                Set.of(sampleModel("A320neo")));
        pilot.deactivate();
        // Act — second call must throw
        pilot.deactivate();
    }

    @Test
    public void ensureIsActiveReturnsFalseAfterDeactivation() {
        // Arrange
        final Pilot pilot = new Pilot(Email.valueOf("pilot@tap.pt"), CompanyIATACode.valueOf("TP"),
                Set.of(sampleModel("A320neo")));
        // Act
        pilot.deactivate();
        // Assert
        assertFalse(pilot.isActive());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void ensureCertificationsAreImmutableFromOutside() {
        final Pilot pilot = new Pilot(Email.valueOf("pilot2@tap.pt"), CompanyIATACode.valueOf("TP"),
                Set.of(sampleModel("A321neo")));
        pilot.certifications().clear();
    }

    @Test
    public void ensureTwoPilotsWithSameEmailAreEqualAndShareHashCode() {
        final Email email = Email.valueOf("pilot@tap.pt");
        final CompanyIATACode iata = CompanyIATACode.valueOf("TP");
        final Pilot a = new Pilot(email, iata, Set.of(sampleModel("A320neo")));
        final Pilot b = new Pilot(email, iata, Set.of(sampleModel("A321neo")));

        assertEquals(a, b);
        assertTrue(a.sameAs(b));
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void ensureTwoPilotsWithDifferentEmailsAreNotEqual() {
        final CompanyIATACode iata = CompanyIATACode.valueOf("TP");
        final Pilot a = new Pilot(Email.valueOf("a@tap.pt"), iata, Set.of(sampleModel("A320neo")));
        final Pilot b = new Pilot(Email.valueOf("b@tap.pt"), iata, Set.of(sampleModel("A320neo")));

        assertFalse(a.equals(b));
        assertFalse(a.sameAs(b));
    }

    @Test
    public void ensureProtectedDefaultConstructorIsInvocable() throws Exception {
        final Constructor<Pilot> ctor = Pilot.class.getDeclaredConstructor();
        ctor.setAccessible(true);

        final Pilot pilot = ctor.newInstance();

        assertNotNull(pilot);
    }

    private AircraftModel sampleModel(final String name) {
        final Maker maker = Maker.valueOf("Airbus");
        final EngineModel engine = new EngineModel(EngineModelName.valueOf(name + "-engine"), maker,
                MotorizationType.TURBOFAN, FuelType.JET_A, 120000, 0.54);
        return new AircraftModel(AircraftModelName.valueOf(name), maker, AircraftType.PASSENGER,
                MotorizationType.TURBOFAN, 42600, 79000, 64300, 26730, 12131, 833, 122.6,
                0.024, 1.5, 6300, Set.of(engine));
    }
}
