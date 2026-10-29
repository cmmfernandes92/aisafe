package eapli.aisafe.reporting.application;

import java.io.IOException;
import java.nio.file.Path;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.customercollaboratormanagement.domain.AreaCollaborator;
import eapli.aisafe.customercollaboratormanagement.repositories.CollaboratorRepository;
import eapli.aisafe.flightmanagement.repositories.FlightRepository;
import eapli.aisafe.reporting.application.parsing.ParsedSimulationReport;
import eapli.aisafe.reporting.domain.SimulationRun;
import eapli.aisafe.reporting.infrastructure.SimulationReportWriter;
import eapli.aisafe.reporting.repositories.SimulationRunRepository;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.validations.Preconditions;

/**
 * US111 use-case entry point (GRASP Controller). Authenticates the FCO, resolves
 * the air control area they are responsible for, reads the run folder through the
 * {@link SimulationResultSource} port, delegates assembly to the
 * {@link SimulationReportAssembler}, persists the {@link SimulationRun}, and drives
 * the operator-file writer. Holds no parsing logic and no domain invariant.
 */
@UseCaseController
public class GenerateSimulationReportController {

    private final SimulationResultSource source;
    private final SimulationRunRepository runs;
    private final SimulationReportAssembler assembler;
    private final SimulationReportWriter writer;
    private final CollaboratorRepository collaborators;

    public GenerateSimulationReportController(final SimulationResultSource source,
            final SimulationRunRepository runs,
            final FlightRepository flights,
            final CollaboratorRepository collaborators,
            final SimulationReportWriter writer) {
        Preconditions.noneNull(source, runs, flights, collaborators, writer);
        this.source = source;
        this.runs = runs;
        this.assembler = new SimulationReportAssembler(flights);
        this.collaborators = collaborators;
        this.writer = writer;
    }

    /**
     * Ingests the run folder, persists it as one {@link SimulationRun} stamped with
     * the FCO's area, and writes the operator summary file.
     *
     * @param runDir     folder holding {@code report.txt} (+ optional {@code flights.csv})
     * @param outputFile destination of the operator-facing summary
     */
    public SimulationRun generate(final Path runDir, final Path outputFile) throws IOException {
        Preconditions.noneNull(runDir, outputFile);
        final AreaCode area = resolveAuthorizedArea();
        final ParsedSimulationReport report = source.read(runDir);
        final SimulationRun run = assembler.assemble(report, area);
        final SimulationRun saved = runs.save(run);
        writer.write(saved, outputFile);
        return saved;
    }

    /**
     * Enforces the PO rule (03/06/2026) that an FCO may only generate reports for
     * the area they are responsible for: authz check, then session → area.
     */
    private AreaCode resolveAuthorizedArea() {
        final var authz = AuthzRegistry.authorizationService();
        authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.FLIGHT_CONTROL_OPERATOR);

        final var user = authz.session()
                .orElseThrow(() -> new IllegalStateException("No authenticated user"))
                .authenticatedUser();
        final Email email = Email.valueOf(user.email().toString());
        final AreaCollaborator collaborator = collaborators.findAreaCollaboratorByEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated FCO is not assigned to an air control area: " + email));
        return collaborator.areaCode();
    }
}