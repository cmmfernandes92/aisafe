package eapli.aisafe.aircraftmodelmanagement.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.enginemodelmanagement.domain.EngineModel;
import eapli.aisafe.enginemodelmanagement.domain.EngineModelName;
import eapli.aisafe.enginemodelmanagement.domain.FuelType;
import eapli.aisafe.enginemodelmanagement.domain.MotorizationType;
import eapli.aisafe.enginemodelmanagement.repositories.EngineModelRepository;
import eapli.aisafe.fleetmanagement.domain.Aircraft;
import eapli.aisafe.fleetmanagement.domain.OperationalStatus;
import eapli.aisafe.fleetmanagement.domain.Registration;
import eapli.aisafe.fleetmanagement.repositories.AircraftRepository;
import eapli.aisafe.makermanagement.domain.Maker;
import eapli.aisafe.test.AuthzTestHelper;
import eapli.framework.infrastructure.authz.application.exceptions.UnauthorizedException;

public class RemoveEngineModelFromAircraftModelControllerTest {

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

    private final FakeAircraftModelRepository modelRepo   = new FakeAircraftModelRepository();
    private final FakeEngineModelRepository   engineRepo  = new FakeEngineModelRepository();
    private final FakeAircraftRepository      aircraftRepo = new FakeAircraftRepository();
    private final RemoveEngineModelFromAircraftModelController controller =
            new RemoveEngineModelFromAircraftModelController(modelRepo, engineRepo, aircraftRepo);

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Maker aircraftMaker() { return Maker.valueOf("Airbus"); }
    private Maker engineMaker()   { return Maker.valueOf("CFM International"); }

    private EngineModel engine1() {
        return new EngineModel(EngineModelName.valueOf("CFM56-5B"), engineMaker(),
                MotorizationType.TURBOFAN, FuelType.JET_A, 120000, 0.54);
    }

    private EngineModel engine2() {
        return new EngineModel(EngineModelName.valueOf("LEAP-1A"), engineMaker(),
                MotorizationType.TURBOFAN, FuelType.JET_A, 130000, 0.51);
    }

    private AircraftModel validModelWithOneEngine(final EngineModel engine) {
        return new AircraftModel(AircraftModelName.valueOf("A320neo"), aircraftMaker(),
                AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                42600, 79000, 64300, 26730, 12131, 833, 122.6, 0.024, 1.5, 6300, Set.of(engine));
    }

    /**
     * Sets the {@code id} field via reflection so entities have distinct hashCodes
     * in the framework's identity-based equals/hashCode implementation.
     */
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

    // -------------------------------------------------------------------------
    // allAircraftModels tests
    // -------------------------------------------------------------------------

    @Test
    public void ensureAllAircraftModelsReturnsRegisteredModel() {
        modelRepo.putWithId(1L, withId(validModelWithOneEngine(engine1()), 1L));

        final List<AircraftModel> result = StreamSupport.stream(
                controller.allAircraftModels().spliterator(), false).toList();

        assertEquals(1, result.size());
    }

    @Test
    public void ensureAllAircraftModelsReturnsEmptyWhenNoneRegistered() {
        assertEquals(0, StreamSupport.stream(
                controller.allAircraftModels().spliterator(), false).count());
    }

    // -------------------------------------------------------------------------
    // certifiedEnginesOf tests
    // -------------------------------------------------------------------------

    @Test
    public void ensureCertifiedEnginesOfReturnsBothEngines() {
        final EngineModel e1 = withId(engine1(), 1L);
        final EngineModel e2 = withId(engine2(), 2L);
        final AircraftModel model = new AircraftModel(
                AircraftModelName.valueOf("A320neo"), aircraftMaker(),
                AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                42600, 79000, 64300, 26730, 12131, 833, 122.6, 0.024, 1.5, 6300,
                Set.of(e1, e2));

        final long count = StreamSupport.stream(
                controller.certifiedEnginesOf(model).spliterator(), false).count();

        assertEquals(2, count);
    }

    @Test
    public void ensureCertifiedEnginesOfReturnsSingleEngine() {
        final AircraftModel model = validModelWithOneEngine(engine1());

        final long count = StreamSupport.stream(
                controller.certifiedEnginesOf(model).spliterator(), false).count();

        assertEquals(1, count);
    }

    // -------------------------------------------------------------------------
    // removeEngine tests (US058)
    // -------------------------------------------------------------------------

    @Test
    public void ensureRemoveEngineSucceedsWhenModelHasMultipleEnginesAndNoActiveAircraft() {
        final EngineModel e1 = withId(engine1(), 1L);
        final EngineModel e2 = withId(engine2(), 2L);
        engineRepo.putWithId(1L, e1);
        engineRepo.putWithId(2L, e2);

        final AircraftModel model = withId(new AircraftModel(
                AircraftModelName.valueOf("A320neo"), aircraftMaker(),
                AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                42600, 79000, 64300, 26730, 12131, 833, 122.6, 0.024, 1.5, 6300,
                Set.of(e1, e2)), 1L);
        assertEquals(2, model.certifiedEngines().size()); // precondition
        modelRepo.putWithId(1L, model);
        // No aircraft in repo → AC2 check passes

        final AircraftModel result = controller.removeEngine(1L, 1L);

        assertEquals(1, result.certifiedEngines().size());
        assertFalse(result.certifiedEngines().stream()
                .anyMatch(e -> e.engineModelName().equals(e1.engineModelName())));
        assertTrue(result.certifiedEngines().stream()
                .anyMatch(e -> e.engineModelName().equals(e2.engineModelName())));
    }

    @Test(expected = IllegalStateException.class)
    public void ensureRemoveEngineFailsWhenActiveAircraftExistForModel() {
        // AC2: cannot remove engine if active aircraft use this model
        final EngineModel e1 = withId(engine1(), 1L);
        final EngineModel e2 = withId(engine2(), 2L);
        engineRepo.putWithId(1L, e1);
        engineRepo.putWithId(2L, e2);

        final AircraftModel model = withId(new AircraftModel(
                AircraftModelName.valueOf("A320neo"), aircraftMaker(),
                AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                42600, 79000, 64300, 26730, 12131, 833, 122.6, 0.024, 1.5, 6300,
                Set.of(e1, e2)), 1L);
        modelRepo.putWithId(1L, model);

        // Add an ACTIVE aircraft for this model → AC2 check must block the removal
        aircraftRepo.addActiveAircraftForModel(1L);

        controller.removeEngine(1L, 1L); // must throw IllegalStateException
    }

    @Test(expected = IllegalStateException.class)
    public void ensureRemoveEngineFailsWhenOnlyOneEngineRemains() {
        // A2: must retain at least one certified engine
        final EngineModel e1 = withId(engine1(), 1L);
        engineRepo.putWithId(1L, e1);
        modelRepo.putWithId(1L, withId(validModelWithOneEngine(e1), 1L));

        controller.removeEngine(1L, 1L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRemoveEngineFailsWhenAircraftModelNotFound() {
        engineRepo.putWithId(1L, withId(engine1(), 1L));
        controller.removeEngine(99L, 1L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRemoveEngineFailsWhenEngineModelNotFound() {
        modelRepo.putWithId(1L, withId(validModelWithOneEngine(engine1()), 1L));
        controller.removeEngine(1L, 99L);
    }

    // -------------------------------------------------------------------------
    // Authorization tests
    // -------------------------------------------------------------------------

    @Test(expected = UnauthorizedException.class)
    public void ensureAllAircraftModelsRequiresBackofficeOperator() {
        AuthzTestHelper.loginAsAdmin();
        controller.allAircraftModels();
    }

    @Test(expected = UnauthorizedException.class)
    public void ensureRemoveEngineRequiresBackofficeOperator() {
        AuthzTestHelper.loginAsAdmin();
        controller.removeEngine(1L, 1L);
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

        @Override
        public Optional<AircraftModel> ofIdentity(final Long id) {
            return Optional.ofNullable(data.get(id));
        }

        @Override public Iterable<AircraftModel> findAll()       { return data.values(); }
        @Override public long count()                            { return data.size(); }
        @Override public void delete(final AircraftModel e)     { data.values().remove(e); }
        @Override public void deleteOfIdentity(final Long id)   { data.remove(id); }

        @Override
        public Optional<AircraftModel> findByNameAndMaker(
                final AircraftModelName name, final Maker maker) {
            return data.values().stream()
                    .filter(m -> m.modelName().equals(name) && m.maker().equals(maker))
                    .findFirst();
        }

        @Override
        public boolean existsByNameAndMaker(final AircraftModelName name, final Maker maker) {
            return findByNameAndMaker(name, maker).isPresent();
        }
    }

    private static class FakeEngineModelRepository implements EngineModelRepository {
        private final Map<Long, EngineModel> data = new HashMap<>();

        void putWithId(final Long id, final EngineModel engine) { data.put(id, engine); }

        @Override
        public <S extends EngineModel> S save(final S entity) {
            for (final Map.Entry<Long, EngineModel> e : data.entrySet()) {
                if (e.getValue() == entity) { data.put(e.getKey(), entity); return entity; }
            }
            return entity;
        }

        @Override
        public Optional<EngineModel> ofIdentity(final Long id) {
            return Optional.ofNullable(data.get(id));
        }

        @Override public Iterable<EngineModel> findAll()        { return data.values(); }
        @Override public long count()                            { return data.size(); }
        @Override public void delete(final EngineModel e)       { data.values().remove(e); }
        @Override public void deleteOfIdentity(final Long id)   { data.remove(id); }

        @Override
        public Optional<EngineModel> findByNameAndMaker(
                final EngineModelName name, final Maker maker) {
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
    }

    /**
     * Fake AircraftRepository.
     *
     * <p>{@link #addActiveAircraftForModel(Long)} seeds a minimal stub Aircraft
     * with a given modelId and ACTIVE status, allowing tests to exercise the AC2 check.
     * The stub is created via a simple inner class that overrides {@code modelId()} and
     * {@code status()} only — avoiding the need to supply a full {@link Aircraft} constructor.
     */
    private static class FakeAircraftRepository implements AircraftRepository {
        private final List<Aircraft> data = new ArrayList<>();

        /** Adds a minimal active Aircraft stub for the given modelId (for AC2 tests). */
        void addActiveAircraftForModel(final Long modelId) {
            data.add(new Aircraft() {                          // ← protected ORM constructor
                @Override public Long modelId()             { return modelId; }
                @Override public OperationalStatus status() { return OperationalStatus.ACTIVE; }
            });
        }

        @Override public Iterable<Aircraft> findAll()                             { return data; }
        @Override public Iterable<Aircraft> findAllByCompany(CompanyIATACode c)   { return List.of(); }
        @Override public <S extends Aircraft> S save(final S e)                   { data.add(e); return e; }
        @Override public Optional<Aircraft> ofIdentity(final Registration id)     { return Optional.empty(); }
        @Override public long count()                                              { return data.size(); }
        @Override public void delete(final Aircraft e)                            { data.remove(e); }
        @Override public void deleteOfIdentity(final Registration id)             {}
    }
}