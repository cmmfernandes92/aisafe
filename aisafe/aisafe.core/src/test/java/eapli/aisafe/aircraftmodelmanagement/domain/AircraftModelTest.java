package eapli.aisafe.aircraftmodelmanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Set;

import org.junit.Test;

import eapli.aisafe.enginemodelmanagement.domain.EngineModel;
import eapli.aisafe.enginemodelmanagement.domain.EngineModelName;
import eapli.aisafe.enginemodelmanagement.domain.FuelType;
import eapli.aisafe.enginemodelmanagement.domain.MotorizationType;
import eapli.aisafe.makermanagement.domain.Maker;

public class AircraftModelTest {

    private Maker makerAirbus() { return Maker.valueOf("Airbus"); }
    private Maker makerCfm() { return Maker.valueOf("CFM International"); }

    private EngineModel turbofanEngine() {
        return new EngineModel(EngineModelName.valueOf("CFM56-5B"), makerCfm(),
                MotorizationType.TURBOFAN, FuelType.JET_A, 120000, 0.54);
    }

    private EngineModel turbopropEngine() {
        return new EngineModel(EngineModelName.valueOf("PW150A"), makerCfm(),
                MotorizationType.TURBOPROP, FuelType.JET_A, 3800, 0.35);
    }

    private EngineModel secondTurbofanEngine() {
        return new EngineModel(EngineModelName.valueOf("LEAP-1A"), makerCfm(),
                MotorizationType.TURBOFAN, FuelType.JET_A, 130000, 0.51);
    }

    private AircraftModel validModel(final Set<EngineModel> engines) {
        return new AircraftModel(AircraftModelName.valueOf("A320neo"), makerAirbus(),
                AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                42600, 79000, 64300, 26730, 12131, 833, 122.6, 0.024, 1.5, 6300, engines);
    }

    private static EngineModel withId(final EngineModel engine, final Long id) {
        try {
            Class<?> clazz = engine.getClass();
            Field field = null;
            while (clazz != null) {
                try { field = clazz.getDeclaredField("id"); break; }
                catch (final NoSuchFieldException e) { clazz = clazz.getSuperclass(); }
            }
            if (field == null) throw new IllegalStateException("'id' field not found on EngineModel hierarchy");
            field.setAccessible(true);
            field.set(engine, id);
            return engine;
        } catch (final IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    // -------------------------------------------------------------------------
    // Construction tests
    // -------------------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void ensureAircraftModelRejectsNullName() {
        new AircraftModel(null, makerAirbus(), AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                42600, 79000, 64300, 26730, 12131, 833, 122.6, 0.024, 1.5, 6300, Set.of(turbofanEngine()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAircraftModelRejectsNullMaker() {
        new AircraftModel(AircraftModelName.valueOf("A320neo"), null, AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                42600, 79000, 64300, 26730, 12131, 833, 122.6, 0.024, 1.5, 6300, Set.of(turbofanEngine()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAircraftModelRejectsNullType() {
        new AircraftModel(AircraftModelName.valueOf("A320neo"), makerAirbus(), null, MotorizationType.TURBOFAN,
                42600, 79000, 64300, 26730, 12131, 833, 122.6, 0.024, 1.5, 6300, Set.of(turbofanEngine()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAircraftModelRejectsNullMotorizationType() {
        new AircraftModel(AircraftModelName.valueOf("A320neo"), makerAirbus(), AircraftType.PASSENGER, null,
                42600, 79000, 64300, 26730, 12131, 833, 122.6, 0.024, 1.5, 6300, Set.of(turbofanEngine()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAircraftModelRejectsEmptyEngineList() {
        new AircraftModel(AircraftModelName.valueOf("A320neo"), makerAirbus(),
                AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                42600, 79000, 64300, 26730, 12131, 833, 122.6, 0.024, 1.5, 6300, Set.of());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAircraftModelRejectsIncompatibleEngine() {
        new AircraftModel(AircraftModelName.valueOf("A320neo"), makerAirbus(),
                AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                42600, 79000, 64300, 26730, 12131, 833, 122.6, 0.024, 1.5, 6300,
                Set.of(turbopropEngine()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAircraftModelRejectsNonPositiveEmptyWeight() {
        new AircraftModel(AircraftModelName.valueOf("A320neo"), makerAirbus(),
                AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                0, 79000, 64300, 26730, 12131, 833, 122.6, 0.024, 1.5, 6300, Set.of(turbofanEngine()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAircraftModelRejectsNonPositiveMtow() {
        new AircraftModel(AircraftModelName.valueOf("A320neo"), makerAirbus(),
                AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                42600, 0, 64300, 26730, 12131, 833, 122.6, 0.024, 1.5, 6300, Set.of(turbofanEngine()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAircraftModelRejectsNonPositiveMzfw() {
        new AircraftModel(AircraftModelName.valueOf("A320neo"), makerAirbus(),
                AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                42600, 79000, 0, 26730, 12131, 833, 122.6, 0.024, 1.5, 6300, Set.of(turbofanEngine()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAircraftModelRejectsNonPositiveMaxFuelCapacity() {
        new AircraftModel(AircraftModelName.valueOf("A320neo"), makerAirbus(),
                AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                42600, 79000, 64300, 0, 12131, 833, 122.6, 0.024, 1.5, 6300, Set.of(turbofanEngine()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAircraftModelRejectsNonPositiveServiceCeiling() {
        new AircraftModel(AircraftModelName.valueOf("A320neo"), makerAirbus(),
                AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                42600, 79000, 64300, 26730, 0, 833, 122.6, 0.024, 1.5, 6300, Set.of(turbofanEngine()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAircraftModelRejectsNonPositiveCruiseSpeed() {
        new AircraftModel(AircraftModelName.valueOf("A320neo"), makerAirbus(),
                AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                42600, 79000, 64300, 26730, 12131, 0, 122.6, 0.024, 1.5, 6300, Set.of(turbofanEngine()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAircraftModelRejectsNonPositiveWingArea() {
        new AircraftModel(AircraftModelName.valueOf("A320neo"), makerAirbus(),
                AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                42600, 79000, 64300, 26730, 12131, 833, 0, 0.024, 1.5, 6300, Set.of(turbofanEngine()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAircraftModelRejectsNonPositiveDragCoefficient() {
        new AircraftModel(AircraftModelName.valueOf("A320neo"), makerAirbus(),
                AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                42600, 79000, 64300, 26730, 12131, 833, 122.6, 0, 1.5, 6300, Set.of(turbofanEngine()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAircraftModelRejectsNonPositiveLiftCoefficient() {
        new AircraftModel(AircraftModelName.valueOf("A320neo"), makerAirbus(),
                AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                42600, 79000, 64300, 26730, 12131, 833, 122.6, 0.024, 0, 6300, Set.of(turbofanEngine()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAircraftModelRejectsNonPositiveMaximumRange() {
        new AircraftModel(AircraftModelName.valueOf("A320neo"), makerAirbus(),
                AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                42600, 79000, 64300, 26730, 12131, 833, 122.6, 0.024, 1.5, 0, Set.of(turbofanEngine()));
    }

    @Test
    public void ensureAircraftModelAcceptsValidInput() {
        final EngineModel engine = turbofanEngine();
        final AircraftModel model = validModel(Set.of(engine));

        assertEquals(AircraftModelName.valueOf("A320neo"), model.modelName());
        assertEquals(AircraftType.PASSENGER, model.type());
        assertEquals(MotorizationType.TURBOFAN, model.motorizationType());
        assertTrue(model.certifiedEngines().contains(engine));
    }

    // -------------------------------------------------------------------------
    // addCertifiedEngine tests
    // -------------------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void ensureAddCertifiedEngineRejectsIncompatibleType() {
        final AircraftModel model = validModel(Set.of(turbofanEngine()));
        model.addCertifiedEngine(turbopropEngine());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAddCertifiedEngineRejectsDuplicate() {
        final EngineModel engine = turbofanEngine();
        final AircraftModel model = validModel(Set.of(engine));
        model.addCertifiedEngine(engine);
    }

    @Test
    public void ensureAddCertifiedEngineAddsCompatibleEngine() {
        final EngineModel engine1 = turbofanEngine();
        final EngineModel engine2 = secondTurbofanEngine();
        final AircraftModel model = validModel(Set.of(engine1));
        model.addCertifiedEngine(engine2);
        assertTrue(model.certifiedEngines().contains(engine2));
    }

    // -------------------------------------------------------------------------
    // removeCertifiedEngine tests (US058)
    // -------------------------------------------------------------------------

    @Test
    public void ensureRemoveCertifiedEngineSucceedsWhenMultipleEnginesExist() {
        // Give each engine a distinct id so the internal HashSet treats them as
        // different elements (framework hashCode/equals are identity-based).
        final EngineModel engine1 = withId(turbofanEngine(), 1L);
        final EngineModel engine2 = withId(secondTurbofanEngine(), 2L);
        final AircraftModel model = validModel(Set.of(engine1, engine2));
        assertEquals(2, model.certifiedEngines().size()); // precondition

        model.removeCertifiedEngine(engine1);

        assertEquals(1, model.certifiedEngines().size());
        assertFalse(model.certifiedEngines().stream()
                .anyMatch(e -> e.engineModelName().equals(engine1.engineModelName())));
        assertTrue(model.certifiedEngines().stream()
                .anyMatch(e -> e.engineModelName().equals(engine2.engineModelName())));
    }

    @Test(expected = IllegalStateException.class)
    public void ensureRemoveCertifiedEngineFailsWhenOnlyOneEngineRemains() {
        // Invariant A2: AircraftModel must retain at least one certified engine
        final EngineModel engine = turbofanEngine();
        final AircraftModel model = validModel(Set.of(engine));

        model.removeCertifiedEngine(engine);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRemoveCertifiedEngineFailsWhenEngineNotCertifiedForModel() {
        // Engine not in the certified list must be rejected
        final EngineModel certifiedEngine = turbofanEngine();
        final EngineModel notCertifiedEngine = secondTurbofanEngine();
        final AircraftModel model = validModel(Set.of(certifiedEngine));

        model.removeCertifiedEngine(notCertifiedEngine);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRemoveCertifiedEngineRejectsNullEngine() {
        final EngineModel engine = turbofanEngine();
        final AircraftModel model = validModel(Set.of(engine));

        model.removeCertifiedEngine(null);
    }

    // -------------------------------------------------------------------------
    // sameAs / equals / toString tests
    // -------------------------------------------------------------------------

    @Test
    public void ensureAircraftModelSameAsForSameNameAndMaker() {
        assertTrue(validModel(Set.of(turbofanEngine())).sameAs(validModel(Set.of(turbofanEngine()))));
    }

    @Test
    public void ensureAircraftModelNotSameAsForDifferentName() {
        final AircraftModel other = new AircraftModel(AircraftModelName.valueOf("B737"), makerAirbus(),
                AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                42600, 79000, 64300, 26730, 12131, 833, 122.6, 0.024, 1.5, 6300, Set.of(turbofanEngine()));
        assertFalse(validModel(Set.of(turbofanEngine())).sameAs(other));
    }

    @Test
    public void ensureAircraftModelToStringIsNotEmpty() {
        assertFalse(validModel(Set.of(turbofanEngine())).toString().isEmpty());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void ensureCertifiedEnginesSetIsUnmodifiable() {
        validModel(Set.of(turbofanEngine())).certifiedEngines().add(turbofanEngine());
    }

    // -------------------------------------------------------------------------
    // Getter tests
    // -------------------------------------------------------------------------

    @Test
    public void ensureGettersReturnCorrectValues() {
        final AircraftModel model = validModel(Set.of(turbofanEngine()));
        assertEquals(makerAirbus(), model.maker());
        assertEquals(AircraftType.PASSENGER, model.type());
        assertEquals(MotorizationType.TURBOFAN, model.motorizationType());
        assertEquals(42600, model.emptyWeight(), 0.001);
        assertEquals(79000, model.mtow(), 0.001);
        assertEquals(64300, model.mzfw(), 0.001);
        assertEquals(26730, model.maxFuelCapacity(), 0.001);
        assertEquals(12131, model.serviceCeiling(), 0.001);
        assertEquals(833, model.cruiseSpeed(), 0.001);
        assertEquals(122.6, model.wingArea(), 0.001);
        assertEquals(0.024, model.dragCoefficient(), 0.0001);
        assertEquals(1.5, model.liftCoefficient(), 0.001);
        assertEquals(6300, model.maximumRange(), 0.001);
    }

    @Test
    public void ensureIdentityIsNullBeforePersist() {
        assertNull(validModel(Set.of(turbofanEngine())).identity());
    }

    // -------------------------------------------------------------------------
    // equals / hashCode
    // -------------------------------------------------------------------------

    @Test
    public void ensureEqualsReturnsTrueForSameInstance() {
        final AircraftModel m = validModel(Set.of(turbofanEngine()));
        assertTrue(m.equals(m));
    }

    @Test
    public void ensureEqualsReturnsFalseForNull() {
        assertFalse(validModel(Set.of(turbofanEngine())).equals(null));
    }

    @Test
    public void ensureEqualsReturnsFalseForDifferentType() {
        assertFalse(validModel(Set.of(turbofanEngine())).equals("not an aircraft model"));
    }

    @Test
    public void ensureHashCodeIsConsistent() {
        final AircraftModel m = validModel(Set.of(turbofanEngine()));
        assertEquals(m.hashCode(), m.hashCode());
    }

    @Test
    public void ensureAircraftModelSameAsReturnsFalseForNonAircraftModel() {
        assertFalse(validModel(Set.of(turbofanEngine())).sameAs("not an aircraft model"));
    }

    // --- US085: wing span + aspect ratio for the §3.3 induced-drag term ---

    @Test
    public void ensureFullConstructorComputesAspectRatioFromSpanAndArea() {
        // A320neo: span 35.8 m, area 122.6 m² → AR = 35.8²/122.6 ≈ 10.45
        final AircraftModel m = new AircraftModel(AircraftModelName.valueOf("A320neo"), makerAirbus(),
                AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                42600, 79000, 64300, 26730, 12131, 833, 122.6, 35.8, 0.024, 1.5, 6300,
                Set.of(turbofanEngine()));
        assertEquals(35.8, m.wingSpan(), 1e-9);
        assertEquals(35.8 * 35.8 / 122.6, m.aspectRatio(), 1e-9);
        assertEquals(10.45, m.aspectRatio(), 0.01);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureFullConstructorRejectsNonPositiveWingSpan() {
        new AircraftModel(AircraftModelName.valueOf("A320neo"), makerAirbus(),
                AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                42600, 79000, 64300, 26730, 12131, 833, 122.6, 0.0, 0.024, 1.5, 6300,
                Set.of(turbofanEngine()));
    }

    @Test
    public void ensureLegacyConstructorDerivesSpanFromNominalAspectRatio() {
        // No span supplied → AR defaults to the nominal 9.0
        final AircraftModel m = validModel(Set.of(turbofanEngine()));
        assertEquals(9.0, m.aspectRatio(), 1e-9);
    }
}