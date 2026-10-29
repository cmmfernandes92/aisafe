package eapli.aisafe.flightplanmanagement.dsl;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FlightPlanImporterFactoryTest {

    @Test
    void returnsCompilerForFlightExtension() {
        final var importer = FlightPlanImporterFactory.forPath(Path.of("plan.flight"));

        assertThat(importer).isPresent();
        assertThat(importer.get()).isInstanceOf(FlightDslCompiler.class);
    }

    @Test
    void returnsEmptyForUnknownExtension() {
        assertThat(FlightPlanImporterFactory.forPath(Path.of("plan.xml"))).isEmpty();
        assertThat(FlightPlanImporterFactory.forPath(Path.of("plan.json"))).isEmpty();
        assertThat(FlightPlanImporterFactory.forPath(Path.of("plan"))).isEmpty();
    }

    @Test
    void extensionMatchingIsCaseInsensitive() {
        assertThat(FlightPlanImporterFactory.forPath(Path.of("plan.FLIGHT"))).isPresent();
        assertThat(FlightPlanImporterFactory.forPath(Path.of("plan.Flight"))).isPresent();
    }

    @Test
    void supportedExtensionsContainsFlight() {
        assertThat(FlightPlanImporterFactory.supportedExtensions()).contains("flight");
    }
}