package eapli.aisafe.app.atcc.console.presentation.fleetmanagement;

import eapli.aisafe.fleetmanagement.application.DecommissionAircraftController;
import eapli.aisafe.fleetmanagement.domain.Aircraft;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class DecommissionAircraftUI extends AbstractUI {

    private final DecommissionAircraftController controller = new DecommissionAircraftController(
            PersistenceContext.repositories().aircraft(),
            PersistenceContext.repositories().customerCollaborators(),
            PersistenceContext.repositories().aisafeUsers());

    @Override
    protected boolean doShow() {
        final Iterable<Aircraft> fleet = controller.listCompanyAircraft();

        System.out.println("\nActive Aircraft in Your Fleet:");
        boolean any = false;
        for (final Aircraft a : fleet) {
            System.out.printf("  %-10s  model id: %-4d  country: %s%n",
                    a.registration().value(), a.modelId(), a.registrationCountry());
            any = true;
        }
        if (!any) {
            System.out.println("  No active aircraft found.");
            return false;
        }

        final String registration = Console.readNonEmptyLine("Registration to decommission",
                "Registration must not be blank.");

        try {
            final Aircraft result = controller.decommissionAircraft(registration);
            System.out.println("Aircraft decommissioned: " + result);
            return true;
        } catch (final IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Decommission failed: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public String headline() {
        return "Decommission Aircraft";
    }
}
