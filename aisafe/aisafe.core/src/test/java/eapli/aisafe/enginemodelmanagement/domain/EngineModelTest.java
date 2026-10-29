package eapli.aisafe.enginemodelmanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import eapli.aisafe.makermanagement.domain.Maker;

public class EngineModelTest {

    private Maker maker() {
        return Maker.valueOf("CFM International");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureEngineModelRejectsNullName() {
        new EngineModel(null, maker(), MotorizationType.TURBOFAN, FuelType.JET_A, 120000, 0.54);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureEngineModelRejectsNullMaker() {
        new EngineModel(new EngineModelName("CFM56-5B"), null, MotorizationType.TURBOFAN, FuelType.JET_A, 120000, 0.54);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureEngineModelRejectsNullMotorizationType() {
        new EngineModel(new EngineModelName("CFM56-5B"), maker(), null, FuelType.JET_A, 120000, 0.54);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureEngineModelRejectsNullFuelType() {
        new EngineModel(new EngineModelName("CFM56-5B"), maker(), MotorizationType.TURBOFAN, null, 120000, 0.54);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureEngineModelRejectsNonPositivePower() {
        new EngineModel(new EngineModelName("CFM56-5B"), maker(), MotorizationType.TURBOFAN, FuelType.JET_A, 0, 0.54);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureEngineModelRejectsNonPositiveEfficiency() {
        new EngineModel(new EngineModelName("CFM56-5B"), maker(), MotorizationType.TURBOFAN, FuelType.JET_A, 120000, 0);
    }

    @Test
    public void ensureEngineModelAcceptsValidInput() {
        final EngineModelName name = new EngineModelName("CFM56-5B");
        final Maker m = maker();
        final EngineModel model = new EngineModel(name, m, MotorizationType.TURBOFAN, FuelType.JET_A, 120000, 0.54);

        assertEquals(name, model.engineModelName());
        assertEquals(m, model.maker());
        assertEquals(MotorizationType.TURBOFAN, model.motorizationType());
        assertEquals(FuelType.JET_A, model.fuelType());
        assertEquals(120000, model.power(), 0.001);
        assertEquals(0.54, model.efficiency(), 0.001);
    }

    @Test
    public void ensureEngineModelSameAsForSameNameAndMaker() {
        final EngineModel one = new EngineModel(new EngineModelName("CFM56-5B"), maker(), MotorizationType.TURBOFAN, FuelType.JET_A, 120000, 0.54);
        final EngineModel another = new EngineModel(new EngineModelName("CFM56-5B"), maker(), MotorizationType.TURBOFAN, FuelType.JET_A, 90000, 0.60);
        assertTrue(one.sameAs(another));
    }

    @Test
    public void ensureEngineModelNotSameAsForDifferentName() {
        final EngineModel one = new EngineModel(new EngineModelName("CFM56-5B"), maker(), MotorizationType.TURBOFAN, FuelType.JET_A, 120000, 0.54);
        final EngineModel another = new EngineModel(new EngineModelName("LEAP-1A"), maker(), MotorizationType.TURBOFAN, FuelType.JET_A, 120000, 0.54);
        assertFalse(one.sameAs(another));
    }

    @Test
    public void ensureEngineModelToStringIsNotEmpty() {
        assertFalse(new EngineModel(new EngineModelName("CFM56-5B"), maker(), MotorizationType.TURBOFAN, FuelType.JET_A, 120000, 0.54).toString().isEmpty());
    }

    @Test
    public void ensureEngineModelSameAsReturnsFalseForNonEngineModel() {
        final EngineModel engine = new EngineModel(new EngineModelName("CFM56-5B"), maker(),
                MotorizationType.TURBOFAN, FuelType.JET_A, 120000, 0.54);
        assertFalse(engine.sameAs("not an engine model"));
    }

    @Test
    public void ensureEqualsReturnsTrueForSameInstance() {
        final EngineModel engine = new EngineModel(new EngineModelName("CFM56-5B"), maker(),
                MotorizationType.TURBOFAN, FuelType.JET_A, 120000, 0.54);
        assertTrue(engine.equals(engine));
    }

    @Test
    public void ensureEqualsReturnsFalseForNull() {
        assertFalse(new EngineModel(new EngineModelName("CFM56-5B"), maker(),
                MotorizationType.TURBOFAN, FuelType.JET_A, 120000, 0.54).equals(null));
    }

    @Test
    public void ensureHashCodeIsConsistent() {
        final EngineModel engine = new EngineModel(new EngineModelName("CFM56-5B"), maker(),
                MotorizationType.TURBOFAN, FuelType.JET_A, 120000, 0.54);
        assertEquals(engine.hashCode(), engine.hashCode());
    }

    // --- US085: cruise thrust (2nd §3.3 thrust point) ---

    @Test
    public void ensureFullConstructorKeepsCruiseThrust() {
        final EngineModel engine = new EngineModel(new EngineModelName("CFM LEAP-1A"), maker(),
                MotorizationType.TURBOFAN, FuelType.JET_A, 130000, 24000, 0.51);
        assertEquals(130000, engine.power(), 1e-9);
        assertEquals(24000, engine.cruiseThrust(), 1e-9);
    }

    @Test
    public void ensureLegacyConstructorDerivesCruiseThrustFromStaticThrust() {
        final EngineModel engine = new EngineModel(new EngineModelName("CFM56-5B"), maker(),
                MotorizationType.TURBOFAN, FuelType.JET_A, 120000, 0.54);
        // default cruise thrust = 20% of static thrust
        assertEquals(0.2 * 120000, engine.cruiseThrust(), 1e-9);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureFullConstructorRejectsCruiseThrustAboveStatic() {
        new EngineModel(new EngineModelName("CFM LEAP-1A"), maker(),
                MotorizationType.TURBOFAN, FuelType.JET_A, 130000, 140000, 0.51);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureFullConstructorRejectsNonPositiveCruiseThrust() {
        new EngineModel(new EngineModelName("CFM LEAP-1A"), maker(),
                MotorizationType.TURBOFAN, FuelType.JET_A, 130000, 0, 0.51);
    }
}
