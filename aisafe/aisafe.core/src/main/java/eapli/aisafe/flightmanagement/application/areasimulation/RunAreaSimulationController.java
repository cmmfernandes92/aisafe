package eapli.aisafe.flightmanagement.application.areasimulation;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import eapli.aisafe.aircontrolareamanagement.domain.AirControlArea;
import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircontrolareamanagement.repositories.AirControlAreaRepository;
import eapli.aisafe.customercollaboratormanagement.domain.AreaCollaborator;
import eapli.aisafe.customercollaboratormanagement.repositories.CollaboratorRepository;
import eapli.aisafe.flightmanagement.application.AreaCandidateFlightsService;
import eapli.aisafe.flightmanagement.application.AreaCandidateFlightsService.CandidateFlight;
import eapli.aisafe.flightmanagement.application.parsing.FlightPlanCompiler;
import eapli.aisafe.flightmanagement.repositories.FlightRepository;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.validations.Preconditions;

public class RunAreaSimulationController {

    private static final DateTimeFormatter RUN_STAMP =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final AirControlAreaRepository areas;
    private final CollaboratorRepository collaborators;
    private final AreaCandidateFlightsService candidateFlights;
    private final AreaSimulationScenarioBuilder scenarioBuilder;
    private final AreaSimulationRunner runner;
    private final Path runsRoot;

    public RunAreaSimulationController(final FlightRepository flights,
            final AirControlAreaRepository areas,
            final CollaboratorRepository collaborators,
            final FlightPlanCompiler compiler,
            final AreaSimulationRunner runner,
            final Path runsRoot) {
        Preconditions.noneNull(flights, areas, collaborators, compiler, runner, runsRoot);
        this.areas = areas;
        this.collaborators = collaborators;
        this.candidateFlights = new AreaCandidateFlightsService(flights, areas, compiler);
        this.scenarioBuilder = new AreaSimulationScenarioBuilder();
        this.runner = runner;
        this.runsRoot = runsRoot;
    }

    public AreaCode myAreaCode() {
        return assignedArea().code();
    }

    public List<CandidateFlight> candidateFlights(final SimulationWindow window) {
        Preconditions.nonNull(window);
        final AirControlArea area = assignedArea();
        return candidateFlights.candidates(area.code(), window.from(), window.to());
    }

    public AreaSimulationRun runForMyArea(final SimulationWindow window)
            throws IOException, InterruptedException {
        Preconditions.nonNull(window);
        final AirControlArea area = assignedArea();
        final var candidates = candidateFlights.candidatePlans(area.code(), window.from(), window.to());
        if (candidates.isEmpty()) {
            throw new IllegalStateException("No TESTED candidate flights cross area " + area.code());
        }

        final SurrogateIdMap idMap = new SurrogateIdMap();
        final AreaSimulationScenario scenario = scenarioBuilder.build(area, candidates, window, idMap);
        return runner.run(scenario, nextRunDir(area.code()));
    }

    private AirControlArea assignedArea() {
        final var authz = AuthzRegistry.authorizationService();
        authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.FLIGHT_CONTROL_OPERATOR, AisafeRoles.POWER_USER);

        final var user = authz.session()
                .orElseThrow(() -> new IllegalStateException("No authenticated user"))
                .authenticatedUser();
        final Email email = Email.valueOf(user.email().toString());
        final AreaCollaborator collaborator = collaborators.findAreaCollaboratorByEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user is not assigned to an air control area: " + email));

        return areas.findByCode(collaborator.areaCode())
                .orElseThrow(() -> new IllegalStateException(
                        "Assigned air control area does not exist: " + collaborator.areaCode()));
    }

    private Path nextRunDir(final AreaCode areaCode) {
        return runsRoot.resolve(areaCode + "-" + RUN_STAMP.format(LocalDateTime.now()));
    }
}
