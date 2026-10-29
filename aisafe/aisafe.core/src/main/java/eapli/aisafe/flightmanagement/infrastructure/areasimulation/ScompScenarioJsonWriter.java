package eapli.aisafe.flightmanagement.infrastructure.areasimulation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

import eapli.aisafe.flightmanagement.application.areasimulation.AreaSimulationScenario;
import eapli.aisafe.flightmanagement.application.areasimulation.FlightProfile;
import eapli.aisafe.flightmanagement.application.areasimulation.ProfilePoint;
import eapli.aisafe.flightmanagement.application.areasimulation.ScenarioFlight;
import eapli.aisafe.flightmanagement.application.areasimulation.ScenarioSegment;
import eapli.aisafe.flightmanagement.application.areasimulation.SimParams;
import eapli.framework.validations.Preconditions;

public class ScompScenarioJsonWriter {

    public Path write(final AreaSimulationScenario scenario, final Path runDir) throws IOException {
        Preconditions.noneNull(scenario, runDir);
        Files.createDirectories(runDir);
        final Path path = runDir.resolve("scenario.json");
        Files.writeString(path, toJson(scenario), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return path;
    }

    public String toJson(final AreaSimulationScenario scenario) {
        Preconditions.nonNull(scenario);
        final StringBuilder json = new StringBuilder(8192);
        appendScenario(json, scenario);
        return json.toString();
    }

    private static void appendScenario(final StringBuilder json, final AreaSimulationScenario scenario) {
        json.append("{\n");
        appendParams(json, scenario.params());
        json.append(",\n");
        json.append("    \"flights\": [\n");
        for (int i = 0; i < scenario.flights().size(); i++) {
            appendFlight(json, scenario.flights().get(i));
            if (i < scenario.flights().size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("    ]\n");
        json.append("}\n");
    }

    private static void appendParams(final StringBuilder json, final SimParams params) {
        json.append("    \"sim_params\": {\n");
        appendStringField(json, "name", params.name(), true);
        appendNumberField(json, "total_steps", params.totalSteps(), true);
        appendNumberField(json, "seconds_per_step", params.secondsPerStep(), true);
        appendNumberField(json, "history_depth", params.historyDepth(), true);
        appendNumberField(json, "max_violations", params.maxViolations(), true);
        appendNumberField(json, "h_min", params.hMin(), true);
        appendNumberField(json, "v_min", params.vMin(), true);
        appendNumberField(json, "area_x_min", params.areaXMin(), true);
        appendNumberField(json, "area_x_max", params.areaXMax(), true);
        appendNumberField(json, "area_y_min", params.areaYMin(), true);
        appendNumberField(json, "area_y_max", params.areaYMax(), true);
        appendNumberField(json, "area_alt_min", params.areaAltMin(), true);
        appendNumberField(json, "area_alt_max", params.areaAltMax(), true);
        appendNumberField(json, "ref_lat", params.refLat(), true);
        appendNumberField(json, "ref_lon", params.refLon(), false);
        json.append("    }");
    }

    private static void appendFlight(final StringBuilder json, final ScenarioFlight flight) {
        json.append("        {\n");
        json.append("            \"ID\": ").append(flight.id()).append(",\n");
        if (flight.entryStep() > 0) {
            json.append("            \"EntryStep\": ").append(flight.entryStep()).append(",\n");
        }
        appendProfile(json, flight.profile());
        json.append(",\n");
        json.append("            \"Segments\": [\n");
        for (int i = 0; i < flight.segments().size(); i++) {
            appendSegment(json, flight.segments().get(i));
            if (i < flight.segments().size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("            ]\n");
        json.append("        }");
    }

    private static void appendProfile(final StringBuilder json, final FlightProfile profile) {
        json.append("            \"Flight Profile\": {\n");
        json.append("                \"Climb\": [\n");
        for (int i = 0; i < profile.climb().size(); i++) {
            appendProfilePoint(json, profile.climb().get(i), "RateClimb");
            if (i < profile.climb().size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("                ],\n");
        json.append("                \"Descend\": [\n");
        for (int i = 0; i < profile.descend().size(); i++) {
            appendProfilePoint(json, profile.descend().get(i), "RateDescent");
            if (i < profile.descend().size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("                ],\n");
        json.append("                \"Cruise\": { \"Speed\": { \"Value\": ")
                .append(number(profile.cruiseSpeedKnots()))
                .append(", \"Unit\": \"Knots\" } }\n");
        json.append("            }");
    }

    private static void appendProfilePoint(final StringBuilder json, final ProfilePoint point,
            final String rateKey) {
        json.append("                    { \"Altitude\": { \"Value\": ")
                .append(number(point.altitudeMeters()))
                .append(", \"Unit\": \"m\" }, \"Speed\": { \"Value\": ")
                .append(number(point.speedKnots()))
                .append(", \"Unit\": \"Knots\" }, \"")
                .append(rateKey)
                .append("\": { \"Value\": ")
                .append(number(point.verticalRateMps()))
                .append(", \"Unit\": \"m/s\" } }");
    }

    private static void appendSegment(final StringBuilder json, final ScenarioSegment segment) {
        json.append("                {\n");
        json.append("                    \"Mode\": \"").append(escape(segment.mode())).append("\",\n");
        appendPosition(json, "Start", segment.startLatitude(), segment.startLongitude(),
                segment.startAltitudeMeters(), true);
        appendPosition(json, "End", segment.endLatitude(), segment.endLongitude(),
                segment.endAltitudeMeters(), false);
        json.append("                }");
    }

    private static void appendPosition(final StringBuilder json, final String key,
            final double latitude, final double longitude, final double altitude, final boolean comma) {
        json.append("                    \"").append(key).append("\": { \"Latitude\": ")
                .append(number(latitude))
                .append(", \"Longitude\": ")
                .append(number(longitude))
                .append(", \"Altitude\": { \"Quantity\": ")
                .append(number(altitude))
                .append(", \"Unit\": \"m\" } }");
        json.append(comma ? ",\n" : "\n");
    }

    private static void appendStringField(final StringBuilder json, final String key,
            final String value, final boolean comma) {
        json.append("        \"").append(key).append("\": \"")
                .append(escape(value)).append("\"");
        json.append(comma ? ",\n" : "\n");
    }

    private static void appendNumberField(final StringBuilder json, final String key,
            final double value, final boolean comma) {
        json.append("        \"").append(key).append("\": ").append(number(value));
        json.append(comma ? ",\n" : "\n");
    }

    private static void appendNumberField(final StringBuilder json, final String key,
            final int value, final boolean comma) {
        json.append("        \"").append(key).append("\": ").append(value);
        json.append(comma ? ",\n" : "\n");
    }

    private static String number(final double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }

    private static String escape(final String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
