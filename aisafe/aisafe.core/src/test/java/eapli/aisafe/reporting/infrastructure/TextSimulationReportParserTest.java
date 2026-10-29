package eapli.aisafe.reporting.infrastructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.Test;

import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.reporting.application.parsing.ParsedSimulationReport;
import eapli.aisafe.reporting.application.parsing.ParsedViolation;

public class TextSimulationReportParserTest {

    private final TextSimulationReportParser parser = new TextSimulationReportParser();

    private Path resourceDir(final String name) throws Exception {
        return Path.of(getClass().getResource("/us111/" + name).toURI());
    }

    @Test
    public void ensureCleanReportParsesHeaderStatusesAndResolvesIdentities() throws Exception {
        final ParsedSimulationReport report = parser.read(resourceDir("clean"));

        assertEquals(1, report.reportVersion());
        assertEquals(LocalDateTime.parse("2026-06-12T12:05:04"), report.runTimestamp());
        assertEquals("clean", report.scenario());
        assertEquals(3, report.totalFlights());
        assertEquals(60.0, report.secondsPerStep(), 0.0001);
        assertEquals("PASS", report.validation());
        assertEquals(3, report.statuses().size());
        assertTrue(report.violations().isEmpty());

        // surrogate 201 resolved through flights.csv to its route+date identity
        assertEquals(new FlightId(RouteName.valueOf("TP123"), LocalDate.of(2026, 6, 12)),
                report.statuses().get(0).identity());
    }

    @Test
    public void ensureViolationRowCarriesStepDistancesAndBothIdentities() throws Exception {
        final ParsedSimulationReport report = parser.read(resourceDir("violation"));

        assertEquals("FAIL", report.validation());
        assertEquals(1, report.violations().size());

        final ParsedViolation v = report.violations().get(0);
        assertEquals(5, v.step());
        assertEquals(3.50, v.horizontalDistance(), 0.0001);
        assertEquals(9000.0, v.alt1(), 0.0001);
        assertEquals(new FlightId(RouteName.valueOf("TP123"), LocalDate.of(2026, 6, 12)), v.identity1());
        assertEquals(new FlightId(RouteName.valueOf("FR456"), LocalDate.of(2026, 6, 12)), v.identity2());
        assertEquals("separation", v.kind());
    }

    @Test
    public void ensureAltitudeViolationRowCarriesItsKind() throws Exception {
        final ParsedSimulationReport report = parser.read(resourceDir("altitude"));

        assertEquals(1, report.violations().size());

        final ParsedViolation v = report.violations().get(0);
        assertEquals("altitude_ceiling", v.kind());
        assertEquals(8, v.step());
        assertEquals(250.0, v.verticalDistance(), 0.0001);
        assertEquals(0.0, v.horizontalDistance(), 0.0001);
    }

    @Test
    public void ensureLegacyViolationRowWithoutKindColumnDefaultsToSeparation() {
        final String reportText = "run_timestamp=2026-06-12T14:00:00\n"
                + "validation=FAIL\n"
                + "\n"
                + "[violations]\n"
                + "step,flight_1,flight_2,h_dist,v_dist,x1,y1,alt1,vx1,vy1,vz1,x2,y2,alt2,vx2,vy2,vz2\n"
                + "5,201,202,3.50,120.00,1000.0,2000.0,9000.0,200.0,0.0,0.0,1002.0,2001.0,9100.0,-200.0,0.0,0.0\n";

        final ParsedSimulationReport report = parser.parse(reportText, Map.of());

        assertEquals(1, report.violations().size());
        assertEquals("separation", report.violations().get(0).kind());
    }
}