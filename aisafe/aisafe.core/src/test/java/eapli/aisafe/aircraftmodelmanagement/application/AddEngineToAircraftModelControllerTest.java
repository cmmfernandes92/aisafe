package eapli.aisafe.aircraftmodelmanagement.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModel;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModelName;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftType;
import eapli.aisafe.aircraftmodelmanagement.repositories.AircraftModelRepository;
import eapli.aisafe.enginemodelmanagement.domain.EngineModel;
import eapli.aisafe.enginemodelmanagement.domain.EngineModelName;
import eapli.aisafe.enginemodelmanagement.domain.FuelType;
import eapli.aisafe.enginemodelmanagement.domain.MotorizationType;
import eapli.aisafe.enginemodelmanagement.repositories.EngineModelRepository;
import eapli.aisafe.makermanagement.domain.Maker;
import eapli.aisafe.makermanagement.domain.MakerName;
import eapli.aisafe.test.AuthzTestHelper;

public class AddEngineToAircraftModelControllerTest {

    @BeforeClass
    public static void setUpAuthz() {
        AuthzTestHelper.configureAuthz();
    }

    @Before
    public void login() {
        AuthzTestHelper.loginAsBackofficeOperator();
    }

    @After
    public void logout() {
        AuthzTestHelper.clearSession();
    }

    private final FakeAircraftModelRepository modelRepo  = new FakeAircraftModelRepository();
    private final FakeEngineModelRepository   engineRepo = new FakeEngineModelRepository();
    private final AddEngineToAircraftModelController controller =
            new AddEngineToAircraftModelController(modelRepo, engineRepo);

    private Maker airbus() { return Maker.valueOf("Airbus"); }
    private Maker cfm()    { return Maker.valueOf("CFM International"); }

    private EngineModel engine1() {
        return new EngineModel(EngineModelName.valueOf("CFM56-5B"), cfm(),
                MotorizationType.TURBOFAN, FuelType.JET_A, 120000, 0.54);
    }

    private EngineModel engine2() {
        return new EngineModel(EngineModelName.valueOf("LEAP-1A"), cfm(),
                MotorizationType.TURBOFAN, FuelType.JET_A, 130000, 0.51);
    }

    private EngineModel turbopropEngine() {
        return new EngineModel(EngineModelName.valueOf("PW150A"), cfm(),
                MotorizationType.TURBOPROP, FuelType.JET_A, 3800, 0.35);
    }

    private AircraftModel validModel(final Set<EngineModel> engines) {
        return new AircraftModel(AircraftModelName.valueOf("A320neo"), airbus(),
                AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                42600, 79000, 64300, 26730, 12131, 833, 122.6, 0.024, 1.5, 6300, engines);
    }

    @SuppressWarnings("unchecked")
    private static <T> T withId(final T entity, final Long id) {
        try {
            Class<?> clazz = entity.getClass();
            Field field = null;
            while (clazz != null) {
                try { field = clazz.getDeclaredField("id"); break; }
                catch (final NoSuchFieldException e) { clazz = clazz.getSuperclass(); }
            }
            if (field == null) throw new IllegalStateException("'id' field not found");
            field.setAccessible(true);
            field.set(entity, id);
            return entity;
        } catch (final IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void ensureAddEngineSucceedsWithCompatibleEngine() {
        final EngineModel e1 = withId(engine1(), 1L);
        final EngineModel e2 = withId(engine2(), 2L);
        engineRepo.putWithId(1L, e1);
        engineRepo.putWithId(2L, e2);
        modelRepo.putWithId(1L, withId(validModel(Set.of(e1)), 1L));

        final AircraftModel result = controller.addEngine(1L, 2L);

        assertTrue(result.certifiedEngines().stream()
                .anyMatch(e -> e.engineModelName().equals(e2.engineModelName())));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAddEngineFailsWhenAircraftModelNotFound() {
        engineRepo.putWithId(1L, withId(engine1(), 1L));
        controller.addEngine(99L, 1L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAddEngineFailsWhenEngineModelNotFound() {
        modelRepo.putWithId(1L, withId(validModel(Set.of(engine1())), 1L));
        controller.addEngine(1L, 99L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAddEngineRejectsIncompatibleMotorizationType() {
        final EngineModel e1 = withId(engine1(), 1L);
        final EngineModel prop = withId(turbopropEngine(), 2L);
        engineRepo.putWithId(1L, e1);
        engineRepo.putWithId(2L, prop);
        modelRepo.putWithId(1L, withId(validModel(Set.of(e1)), 1L));

        controller.addEngine(1L, 2L);
    }

    @Test
    public void ensureListAircraftModelsReturnsRegisteredModel() {
        modelRepo.putWithId(1L, withId(validModel(Set.of(engine1())), 1L));
        final long count = StreamSupport.stream(
                controller.listAircraftModels().spliterator(), false).count();
        assertEquals(1, count);
    }

    @Test
    public void ensureListCompatibleEnginesReturnsEnginesWithMatchingType() {
        final EngineModel e1 = withId(engine1(), 1L);
        engineRepo.putWithId(1L, e1);
        engineRepo.putWithId(2L, withId(turbopropEngine(), 2L));
        modelRepo.putWithId(1L, withId(validModel(Set.of(e1)), 1L));

        final long count = StreamSupport.stream(
                controller.listCompatibleEngines(1L).spliterator(), false).count();
        assertEquals(1, count);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureListCompatibleEnginesFailsWhenAircraftNotFound() {
        controller.listCompatibleEngines(99L);
    }

    // -------------------------------------------------------------------------
    // Fake repositories
    // -------------------------------------------------------------------------

    private static class FakeAircraftModelRepository implements AircraftModelRepository {
        private final Map<Long, AircraftModel> data = new HashMap<>();

        void putWithId(final Long id, final AircraftModel model) { data.put(id, model); }

        @Override
        public <S extends AircraftModel> S save(final S entity) {
            for (final Map.Entry<Long, AircraftModel> e : data.entrySet()) {
                if (e.getValue() == entity) { data.put(e.getKey(), entity); return entity; }
            }
            return entity;
        }

        @Override public Optional<AircraftModel> ofIdentity(final Long id) { return Optional.ofNullable(data.get(id)); }

        @Override
        public Optional<AircraftModel> findByNameAndMaker(final AircraftModelName name, final Maker maker) {
            return data.values().stream()
                    .filter(m -> m.modelName().equals(name) && m.maker().equals(maker))
                    .findFirst();
        }

        @Override
        public boolean existsByNameAndMaker(final AircraftModelName name, final Maker maker) {
            return findByNameAndMaker(name, maker).isPresent();
        }

        @Override public Iterable<AircraftModel> findAll()        { return data.values(); }
        @Override public long count()                             { return data.size(); }
        @Override public void delete(final AircraftModel e)      { data.values().remove(e); }
        @Override public void deleteOfIdentity(final Long id)    { data.remove(id); }
    }

    private static class FakeEngineModelRepository implements EngineModelRepository {
        private final Map<Long, EngineModel> data = new HashMap<>();

        void putWithId(final Long id, final EngineModel engine) { data.put(id, engine); }

        @Override
        public <S extends EngineModel> S save(final S entity) { return entity; }

        @Override public Optional<EngineModel> ofIdentity(final Long id) { return Optional.ofNullable(data.get(id)); }

        @Override
        public Optional<EngineModel> findByNameAndMaker(final EngineModelName name, final Maker maker) {
            return data.values().stream()
                    .filter(e -> e.engineModelName().equals(name) && e.maker().equals(maker))
                    .findFirst();
        }

        @Override
        public boolean existsByNameAndMaker(final EngineModelName name, final Maker maker) {
            return findByNameAndMaker(name, maker).isPresent();
        }

        @Override
        public Iterable<EngineModel> findByMotorizationType(final MotorizationType type) {
            return data.values().stream().filter(e -> e.motorizationType() == type).toList();
        }

        @Override public Iterable<EngineModel> findAll()       { return data.values(); }
        @Override public long count()                          { return data.size(); }
        @Override public void delete(final EngineModel e)     { data.values().remove(e); }
        @Override public void deleteOfIdentity(final Long id) { data.remove(id); }
    }
}
