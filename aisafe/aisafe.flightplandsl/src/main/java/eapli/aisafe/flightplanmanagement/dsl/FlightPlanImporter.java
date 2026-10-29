package eapli.aisafe.flightplanmanagement.dsl;

import java.io.InputStream;

public interface FlightPlanImporter {

    CompilationResult importFrom(InputStream content, String sourceName);
}
