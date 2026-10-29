package eapli.aisafe.app.fco.console.presentation.reporting;

import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.reporting.application.GenerateSimulationReportController;
import eapli.aisafe.reporting.infrastructure.SimulationReportWriter;
import eapli.aisafe.reporting.infrastructure.TextSimulationReportParser;
import eapli.framework.actions.Action;

public class GenerateSimulationReportAction implements Action {

    @Override
    public boolean execute() {
        final GenerateSimulationReportController controller = new GenerateSimulationReportController(
                new TextSimulationReportParser(),
                PersistenceContext.repositories().simulationRuns(),
                PersistenceContext.repositories().flights(),
                PersistenceContext.repositories().customerCollaborators(),
                new SimulationReportWriter());

        return new GenerateSimulationReportUI(controller).show();
    }
}