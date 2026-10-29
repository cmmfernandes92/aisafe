package eapli.aisafe.aircraftmodelmanagement.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
import eapli.aisafe.enginemodelmanagement.domain.EngineModel;
import eapli.aisafe.enginemodelmanagement.domain.EngineModelName;
import eapli.aisafe.enginemodelmanagement.domain.FuelType;
import eapli.aisafe.enginemodelmanagement.domain.MotorizationType;
import eapli.aisafe.enginemodelmanagement.repositories.EngineModelRepository;
import eapli.aisafe.makermanagement.domain.Maker;
import eapli.aisafe.makermanagement.domain.MakerName;
import eapli.aisafe.makermanagement.repositories.MakerRepository;
import eapli.aisafe.test.AuthzTestHelper;
import eapli.framework.domain.repositories.IntegrityViolationException;

public class RegisterAircraftModelControllerTest {

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

    private final FakeMakerRepository makerRepo = new FakeMakerRepository();
    private final FakeEngineModelRepository engineRepo = new FakeEngineModelRepository();
    private final FakeAircraftModelRepository aircraftRepo = new FakeAircraftModelRepository();
    private final RegisterAircraftModelController controller =
            new RegisterAircraftModelController(aircraftRepo, engineRepo, makerRepo);

    private Maker airbus() { return Maker.valueOf("Airbus"); }
    private Maker cfm()    { return Maker.valueOf("CFM International"); }

    private EngineModel turbofanEngine() {
        return new EngineModel(EngineModelName.valueOf("CFM56-5B"), cfm(),
                MotorizationType.TURBOFAN, FuelType.JET_A, 120000, 0.54);
    }

    @Before
    public void seedMakers() {
        makerRepo.add(airbus());
        makerRepo.add(cfm());
        engineRepo.add(turbofanEngine());
    }

    @Test
    public void ensureRegisterSucceedsWithValidInput() {
        final AircraftModel model = controller.register(
                "A320neo", "Airbus", AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                42600, 79000, 64300, 26730, 12131, 833, 122.6, 0.024, 1.5, 6300,
                Set.of(turbofanEngine()));
        assertNotNull(model);
        assertEquals(AircraftModelName.valueOf("A320neo"), model.modelName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRegisterRejectsMakerNotFound() {
        controller.register(
                "A320neo", "Unknown Maker", AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                42600, 79000, 64300, 26730, 12131, 833, 122.6, 0.024, 1.5, 6300,
                Set.of(turbofanEngine()));
    }

    @Test(expected = IntegrityViolationException.class)
    public void ensureRegisterRejectsDuplicateNameAndMaker() {
        controller.register(
                "A320neo", "Airbus", AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                42600, 79000, 64300, 26730, 12131, 833, 122.6, 0.024, 1.5, 6300,
                Set.of(turbofanEngine()));
        controller.register(
                "A320neo", "Airbus", AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                42600, 79000, 64300, 26730, 12131, 833, 122.6, 0.024, 1.5, 6300,
                Set.of(turbofanEngine()));
    }

    @Test
    public void ensureListMakersReturnsSeedMakers() {
        final long count = StreamSupport.stream(controller.listMakers().spliterator(), false).count();
        assertEquals(2, count);
    }

    @Test
    public void ensureListEnginesByMotorizationTypeReturnsTurbofanEngines() {
        final long count = StreamSupport.stream(
                controller.listEnginesByMotorizationType(MotorizationType.TURBOFAN).spliterator(), false).count();
        assertEquals(1, count);
    }

    @Test
    public void ensureListEnginesByMotorizationTypeReturnsEmptyForOtherType() {
        final long count = StreamSupport.stream(
                controller.listEnginesByMotorizationType(MotorizationType.TURBOPROP).spliterator(), false).count();
        assertEquals(0, count);
    }

    // -------------------------------------------------------------------------
    // Fake repositories
    // -------------------------------------------------------------------------

    private static class FakeMakerRepository implements MakerRepository {
        private final List<Maker> data = new ArrayList<>();

        void add(final Maker maker) { data.add(maker); }

        @Override
        public Optional<Maker> findByName(final MakerName name) {
            return data.stream().filter(m -> m.identity().equals(name)).findFirst();
        }

        @Override public <S extends Maker> S save(final S e) { data.add(e); return e; }
        @Override public Optional<Maker> ofIdentity(final MakerName id) { return findByName(id); }
        @Override public Iterable<Maker> findAll()      { return data; }
        @Override public long count()                   { return data.size(); }
        @Override public void delete(final Maker e)     { data.remove(e); }
        @Override public void deleteOfIdentity(final MakerName id) { data.removeIf(m -> m.identity().equals(id)); }
    }

    private static class FakeEngineModelRepository implements EngineModelRepository {
        private final Map<Long, EngineModel> data = new HashMap<>();
        private long nextId = 1L;

        void add(final EngineModel e) { data.put(nextId++, e); }

        @Override
        public <S extends EngineModel> S save(final S entity) {
            data.put(nextId++, entity);
            return entity;
        }

        @Override
        public Optional<EngineModel> ofIdentity(final Long id) { return Optional.ofNullable(data.get(id)); }

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

    private static class FakeAircraftModelRepository implements AircraftModelRepository {
        private final Map<Long, AircraftModel> data = new HashMap<>();
        private long nextId = 1L;

        @Override
        public <S extends AircraftModel> S save(final S entity) {
            data.put(nextId++, entity);
            return entity;
        }

        @Override
        public Optional<AircraftModel> ofIdentity(final Long id) { return Optional.ofNullable(data.get(id)); }

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
}
