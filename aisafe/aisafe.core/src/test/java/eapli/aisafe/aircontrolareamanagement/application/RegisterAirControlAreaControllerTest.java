package eapli.aisafe.aircontrolareamanagement.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import eapli.aisafe.aircontrolareamanagement.domain.AirControlArea;
import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircontrolareamanagement.domain.AreaName;
import eapli.aisafe.aircontrolareamanagement.domain.GeographicBoundary;
import eapli.aisafe.aircontrolareamanagement.repositories.AirControlAreaRepository;
import eapli.aisafe.test.AuthzTestHelper;
import eapli.framework.infrastructure.authz.application.exceptions.UnauthorizedException;

public class RegisterAirControlAreaControllerTest {

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

    private static class FakeAirControlAreaRepository
            implements AirControlAreaRepository {

        private final Map<AreaCode, AirControlArea> data = new HashMap<>();

        @Override
        public AirControlArea save(final AirControlArea entity) {
            data.put(entity.identity(), entity);
            return entity;
        }

        @Override
        public Optional<AirControlArea> findByCode(final AreaCode code) {
            return Optional.ofNullable(data.get(code));
        }

        @Override
        public Optional<AirControlArea> findByName(final AreaName name) {
            return data.values().stream()
                    .filter(a -> a.name().equals(name))
                    .findFirst();
        }

        @Override
        public Iterable<AirControlArea> findOverlapping(final GeographicBoundary boundary) {
            return data.values().stream()
                    .filter(a -> a.boundary().overlaps(boundary))
                    .toList();
        }

        @Override
        public Optional<AirControlArea> ofIdentity(final AreaCode id) {
            return findByCode(id);
        }

        @Override
        public Iterable<AirControlArea> findAll() {
            return data.values();
        }

        @Override
        public long count() {
            return data.size();
        }

        @Override
        public void delete(final AirControlArea entity) {
            throw new UnsupportedOperationException("not used in tests");
        }

        @Override
        public void deleteOfIdentity(final AreaCode id) {
            throw new UnsupportedOperationException("not used in tests");
        }
    }

    private RegisterAirControlAreaController newController() {
        return new RegisterAirControlAreaController(new FakeAirControlAreaRepository());
    }

    @Test
    public void ensureRegisterSucceedsWithValidInput() {
        final RegisterAirControlAreaController controller = newController();
        final AirControlArea area = controller.registerArea(
                "LPPC", "Lisbon TMA", 38.0, -10.0, 40.0, -8.0);
        assertNotNull(area);
        assertEquals("LPPC", area.identity().value());
    }

    @Test
    public void ensurePowerUserCanRegisterArea() {
        AuthzTestHelper.loginAsPowerUser();
        final RegisterAirControlAreaController controller = newController();

        final AirControlArea area = controller.registerArea(
                "LPPC", "Lisbon TMA", 38.0, -10.0, 40.0, -8.0);

        assertNotNull(area);
    }

    @Test(expected = UnauthorizedException.class)
    public void ensureRegisterRequiresBackofficeOperatorOrPowerUser() {
        AuthzTestHelper.loginAsPilot();
        final RegisterAirControlAreaController controller = newController();

        controller.registerArea("LPPC", "Lisbon TMA", 38.0, -10.0, 40.0, -8.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRegisterRejectsDuplicateAreaCode() {
        final RegisterAirControlAreaController controller = newController();
        controller.registerArea("LPPC", "Lisbon TMA", 38.0, -10.0, 40.0, -8.0);
        controller.registerArea("LPPC", "Different Name", 50.0, 50.0, 51.0, 51.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRegisterRejectsDuplicateAreaName() {
        final RegisterAirControlAreaController controller = newController();
        controller.registerArea("LPPC", "Lisbon TMA", 38.0, -10.0, 40.0, -8.0);
        controller.registerArea("LPPO", "Lisbon TMA", 50.0, 50.0, 51.0, 51.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRegisterRejectsOverlappingArea() {
        final RegisterAirControlAreaController controller = newController();
        controller.registerArea("LPPC", "Lisbon TMA", 0.0, 0.0, 10.0, 10.0);
        controller.registerArea("LPPO", "Porto TMA", 5.0, 5.0, 15.0, 15.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRegisterRejectsAreaWithSharedEdge() {
        final RegisterAirControlAreaController controller = newController();
        controller.registerArea("LPPC", "Lisbon TMA", 0.0, 0.0, 10.0, 10.0);
        controller.registerArea("LPPO", "Porto TMA", 10.0, 0.0, 20.0, 10.0);
    }

    @Test
    public void ensureRegisterAllowsAdjacentNonTouchingArea() {
        final RegisterAirControlAreaController controller = newController();
        final AirControlArea first = controller.registerArea(
                "LPPC", "Lisbon TMA", 0.0, 0.0, 10.0, 10.0);
        final AirControlArea second = controller.registerArea(
                "LPPO", "Porto TMA", 10.1, 0.0, 20.0, 10.0);
        assertNotNull(first);
        assertNotNull(second);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRegisterRejectsInvalidCoordinates() {
        final RegisterAirControlAreaController controller = newController();
        controller.registerArea("LPPC", "Lisbon TMA", 91.0, 0.0, 92.0, 10.0);
    }
}
