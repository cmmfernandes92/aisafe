package eapli.aisafe.app.fco.console.presentation.reporting;

import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.reporting.application.GenerateMonthlyReportController;
import eapli.aisafe.reporting.infrastructure.HtmlMonthlyReportGenerator;
import eapli.framework.actions.Action;

public class GenerateMonthlyReportAction implements Action {

    @Override
    public boolean execute() {
        final var repo      = PersistenceContext.repositories().monthlyStatistics();
        final var generator = new HtmlMonthlyReportGenerator();
        final var ctrl      = new GenerateMonthlyReportController(repo, generator);
        return new GenerateMonthlyReportUI(ctrl).show();
    }
}
