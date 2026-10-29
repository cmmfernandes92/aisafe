package eapli.aisafe.reporting.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.customercollaboratormanagement.domain.AreaCollaborator;
import eapli.aisafe.customercollaboratormanagement.domain.Collaborator;
import eapli.aisafe.customercollaboratormanagement.domain.CompanyCollaborator;
import eapli.aisafe.customercollaboratormanagement.domain.Position;
import eapli.aisafe.customercollaboratormanagement.repositories.CollaboratorRepository;
import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightmanagement.repositories.FlightRepository;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.reporting.domain.FlightOutcome;
import eapli.aisafe.reporting.domain.SimulationRun;
import eapli.aisafe.reporting.domain.ValidationOutcome;
import eapli.aisafe.reporting.domain.ViolationKind;
import eapli.aisafe.reporting.infrastructure.SimulationReportWriter;
import eapli.aisafe.reporting.infrastructure.TextSimulationReportParser;
import eapli.aisafe.reporting.repositories.SimulationRunRepository;
import eapli.aisafe.test.AuthzTestHelper;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafe.usermanagement.domain.PhoneNumber;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

public class GenerateSimulationReportControllerTest {

    private static final String FCO_EMAIL = "flightcontroloperator@aisafe.test";
    private static final AreaCode LPPC = AreaCode.valueOf("LPPC");

    private final FakeSimulationRunRepository runs = new FakeSimulationRunRepository();
    private final FakeFlightRepository flights = new FakeFlightRepository();
    private final FakeCollaboratorRepository collaborators = new FakeCollaboratorRepository();
    private GenerateSimulationReportController controller;

    @Before
    public void setUp() {
        AuthzTestHelper.configureAuthz();
        // eapli's InMemoryDomainRepository keeps a process-wide store per type; drain it
        // so each test sees only the run it persists.
        final var existing = new java.util.ArrayList<SimulationRun>();
        runs.findAll().forEach(existing::add);
        existing.forEach(runs::delete);
        collaborators.save(new AreaCollaborator(Email.valueOf(FCO_EMAIL), Position.valueOf("FCO"),
                LPPC, Email.valueOf(FCO_EMAIL), PhoneNumber.valueOf("+351910000000")));
        controller = new GenerateSimulationReportController(
                new TextSimulationReportParser(), runs, flights, collaborators,
                new SimulationReportWriter());
    }

    private Path resourceDir(final String name) throws Exception {
        return Path.of(getClass().getResource("/us111/" + name).toURI());
    }

    private SimulationRun onlyRun() {
        return runs.findAll().iterator().next();
    }

    @Test
    public void ensureGeneratePersistsRunAndWritesFileForCleanReport() throws Exception {
        AuthzTestHelper.loginAsFlightControlOperator();
        final Path out = Files.createTempFile("us111-clean", ".txt");

        controller.generate(resourceDir("clean"), out);

        assertTrue("output file should exist", Files.exists(out));
        assertTrue("output file should not be empty", Files.size(out) > 0);
        assertEquals(3, onlyRun().totalFlights());
        assertEquals(3, onlyRun().flightStatuses().size());
        assertTrue(onlyRun().flightStatuses().stream()
                .allMatch(s -> s.outcome() == FlightOutcome.COMPLETED));
        assertEquals(ValidationOutcome.PASS, onlyRun().validationOutcome());
        assertEquals(LPPC, onlyRun().areaCode());
    }

    @Test
    public void ensureVerdictIsFailAndViolationDerivesTimestampForViolationReport() throws Exception {
        AuthzTestHelper.loginAsFlightControlOperator();
        final Path out = Files.createTempFile("us111-violation", ".txt");

        controller.generate(resourceDir("violation"), out);

        final SimulationRun run = onlyRun();
        assertEquals(ValidationOutcome.FAIL, run.validationOutcome());
        assertEquals(1, run.violations().size());
        // step 5 × 60s after 14:00:00
        assertEquals(LocalDateTime.parse("2026-06-12T14:05:00"), run.violations().get(0).timestamp());
        assertFalse("a position per involved flight", run.violations().get(0).positions().isEmpty());
    }

    @Test
    public void ensureAltitudeViolationIsPersistedWithSingleFlightAndDerivedTimestamp() throws Exception {
        AuthzTestHelper.loginAsFlightControlOperator();
        final Path out = Files.createTempFile("us111-altitude", ".txt");

        controller.generate(resourceDir("altitude"), out);

        final SimulationRun run = onlyRun();
        assertEquals(1, run.violations().size());
        final var violation = run.violations().get(0);
        assertEquals(ViolationKind.ALTITUDE_CEILING, violation.kind());
        assertEquals(1, violation.involvedFlights().size());
        assertEquals(1, violation.positions().size());
        // step 8 × 60s after 14:00:00
        assertEquals(LocalDateTime.parse("2026-06-12T14:08:00"), violation.timestamp());
    }

    @Test
    public void ensureNonFcoUserIsRejected() throws Exception {
        AuthzTestHelper.loginAsPilot();
        final Path out = Files.createTempFile("us111-denied", ".txt");

        boolean rejected = false;
        try {
            controller.generate(resourceDir("clean"), out);
        } catch (final RuntimeException expected) {
            rejected = true;
        }
        assertTrue("a pilot must not generate a simulation report", rejected);
    }

    @Test
    public void ensureFcoNotAssignedToAnAreaIsRejected() throws Exception {
        AuthzTestHelper.loginAsFlightControlOperator();
        // drop the seeded area assignment so the session FCO resolves to no area
        collaborators.findAreaCollaboratorByEmail(Email.valueOf(FCO_EMAIL))
                .ifPresent(collaborators::delete);
        final Path out = Files.createTempFile("us111-noarea", ".txt");

        boolean rejected = false;
        try {
            controller.generate(resourceDir("clean"), out);
        } catch (final IllegalStateException expected) {
            rejected = true;
        }
        assertTrue("an FCO with no area assignment must not generate a report", rejected);
    }

    // --- fakes ---------------------------------------------------------------

    private static final class FakeSimulationRunRepository
            extends InMemoryDomainRepository<SimulationRun, eapli.aisafe.reporting.domain.SimulationRunId>
            implements SimulationRunRepository {
        @Override
        public Iterable<SimulationRun> findByAreaCode(final AreaCode areaCode) {
            return java.util.stream.StreamSupport.stream(findAll().spliterator(), false)
                    .filter(r -> r.areaCode().equals(areaCode)).toList();
        }
        @Override
        public Iterable<SimulationRun> findInPeriod(final java.time.LocalDateTime startInclusive,
                final java.time.LocalDateTime endExclusive) {
            return java.util.stream.StreamSupport.stream(findAll().spliterator(), false)
                    .filter(r -> !r.runTimestamp().isBefore(startInclusive)
                            && r.runTimestamp().isBefore(endExclusive))
                    .toList();
        }
    }

    private static final class FakeFlightRepository implements FlightRepository {

        @Override
        public boolean existsForRouteOnOrAfter(eapli.aisafe.flightroutemanagement.domain.RouteName route, java.time.LocalDate date) {
            return false;
        }
        private final Map<FlightId, Flight> data = new HashMap<>();

        @Override
        public <S extends Flight> S save(final S entity) {
            data.put(entity.identity(), entity);
            return entity;
        }

        @Override
        public boolean existsForRouteOnDate(final RouteName route, final LocalDate date) {
            return data.containsKey(new FlightId(route, date));
        }

        @Override
        public Iterable<Flight> flightsOf(final Email pilot) {
            return data.values();
        }

        @Override
        public Optional<Flight> ofIdentity(final FlightId id) {
            return Optional.ofNullable(data.get(id));
        }

        @Override
        public Iterable<Flight> findAll() {
            return data.values();
        }

        @Override
        public long count() {
            return data.size();
        }

        @Override
        public void delete(final Flight entity) {
            data.remove(entity.identity());
        }

        @Override
        public void deleteOfIdentity(final FlightId id) {
            data.remove(id);
        }
    }

    private static final class FakeCollaboratorRepository implements CollaboratorRepository {
        private final Map<Email, Collaborator> data = new HashMap<>();

        @Override
        public <S extends Collaborator> S save(final S entity) {
            data.put((Email) entity.identity(), entity);
            return entity;
        }

        @Override
        public Optional<AreaCollaborator> findAreaCollaboratorByEmail(final Email email) {
            final Collaborator c = data.get(email);
            return c instanceof AreaCollaborator ? Optional.of((AreaCollaborator) c) : Optional.empty();
        }

        @Override
        public Optional<CompanyCollaborator> findCompanyCollaboratorByEmail(final Email email) {
            return Optional.empty();
        }

        @Override
        public Iterable<CompanyCollaborator> findAllActiveByCompanyCode(final CompanyIATACode companyCode) {
            return java.util.List.of();
        }

        @Override
        public Iterable<CompanyCollaborator> findAllByCompanyCode(final CompanyIATACode companyCode) {
            return java.util.List.of();
        }

        @Override
        public Iterable<AreaCollaborator> findAllActiveByAreaCode(final AreaCode areaCode) {
            return java.util.List.of();
        }

        @Override
        public Iterable<AreaCollaborator> findAllByAreaCode(final AreaCode areaCode) {
            return java.util.List.of();
        }

        @Override
        public Optional<Collaborator> ofIdentity(final Email id) {
            return Optional.ofNullable(data.get(id));
        }

        @Override
        public Iterable<Collaborator> findAll() {
            return data.values();
        }

        @Override
        public long count() {
            return data.size();
        }

        @Override
        public void delete(final Collaborator entity) {
            data.remove(entity.identity());
        }

        @Override
        public void deleteOfIdentity(final Email id) {
            data.remove(id);
        }
    }
}