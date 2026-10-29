package eapli.aisafe.app.backoffice.console.presentation.customercollaborator;

import eapli.aisafe.pilotmanagement.application.ListPilotController;
import eapli.aisafe.pilotmanagement.domain.Pilot;
import eapli.framework.presentation.console.AbstractUI;

public class ListPilotUI extends AbstractUI {

    private final ListPilotController controller = new ListPilotController();

    @Override
    protected boolean doShow() {

        System.out.println("\n=== Pilot Roster ===");

        Iterable<Pilot> pilots = controller.listPilots();

        if (!pilots.iterator().hasNext()) {
            System.out.println("No pilots found.");
            return false;
        }

        for (Pilot p : pilots) {
            System.out.println(formatPilot(p));
        }

        return false;
    }

    private String formatPilot(Pilot p) {
        return p.identity()
                + " | Company: " + p.companyCode()
                + " | Status: " + p.status();
    }

    @Override
    public String headline() {
        return "List Pilot Roster";
    }
}