package eapli.aisafeweb.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.framework.infrastructure.authz.domain.model.Role;

public enum AisafeWebModule {
    BACKOFFICE("backoffice", "Backoffice", "Operational reference data", "/dashboard",
            List.of("Dashboard", "Air Control Areas", "Airports", "Aircraft Models", "Engine Models",
                    "Companies", "Collaborators"),
            Set.of(AisafeRoles.POWER_USER, AisafeRoles.ADMIN, AisafeRoles.BACKOFFICE_OPERATOR)),

    ADMINISTRATION("administration", "Administration", "Users, roles, and platform governance",
            "/modules/administration",
            List.of("Dashboard", "Users", "Roles"),
            Set.of(AisafeRoles.POWER_USER, AisafeRoles.ADMIN)),

    FLIGHT_CONTROL("flight-control", "Flight Control", "Flight control, simulation, and reporting",
            "/modules/flight-control",
            List.of("Dashboard", "Consult Weather Data", "Create Area Simulation", "View Area Simulation",
                    "Simulation Report", "Monthly Report"),
            Set.of(AisafeRoles.POWER_USER, AisafeRoles.FLIGHT_CONTROL_OPERATOR)),

    WEATHER_SERVICE("weather-service", "Weather Service", "Weather reports and operational forecasts",
            "/modules/weather-service/dashboard",
            List.of("Dashboard", "Register Weather Data", "Import Bulk Data", "Consult Weather Data"),
            Set.of(AisafeRoles.POWER_USER, AisafeRoles.WEATHER_PERSON, AisafeRoles.FLIGHT_CONTROL_OPERATOR)),

    AIR_TRANSPORT_COMPANY("air-transport-company", "Air Transport Company", "Fleet and company operations",
            "/modules/air-transport-company",
            List.of("Dashboard", "Aircraft", "Create Flight Route", "Deactivate Flight Route",
                    "Pilots"),
            Set.of(AisafeRoles.POWER_USER, AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR)),

    PILOT_OPERATIONS("pilot-operations", "Pilot Operations", "Pilot flight plan workspace",
            "/modules/pilot-operations",
            List.of("Dashboard", "Create Flight", "Import Flight Plan",
                    "Consult Weather Data", "Add Weather Data", "Test Flight Plan", "Simulate Flight Plan"),
            Set.of(AisafeRoles.POWER_USER, AisafeRoles.PILOT));

    /** Route for creating a Flight and its initial draft flight plan (Pilot Operations). */
    public static final String CREATE_FLIGHT_PATH = "/modules/pilot-operations/create-flight";

    /** Route for importing a validated DSL file into an existing draft (Pilot Operations). */
    public static final String IMPORT_FLIGHT_PLAN_PATH = "/modules/pilot-operations/import-flight-plan";

    /** Route for consulting weather data from the pilot workspace (Pilot Operations). */
    public static final String PILOT_CONSULT_WEATHER_DATA_PATH = "/modules/pilot-operations/consult-weather-data";

    /** Route for attaching weather data to a flight plan (Pilot Operations). */
    public static final String ADD_WEATHER_DATA_PATH = "/modules/pilot-operations/add-weather-data";

    /** Route for testing the current flight plan (Pilot Operations). */
    public static final String TEST_FLIGHT_PLAN_PATH = "/modules/pilot-operations/test-flight-plan";

    /** Route for the in-browser flight-plan simulator (Pilot Operations). */
    public static final String SIMULATE_FLIGHT_PLAN_PATH = "/modules/pilot-operations/simulate-flight-plan";

    /** Route for the in-browser airspace (multi-flight) simulator (Flight Control). */
    public static final String SIMULATE_AREA_FLIGHTS_PATH = "/modules/flight-control/simulate-area-flights";

    /** Route for running the native SCOMP area simulation (Flight Control). */
    public static final String RUN_AREA_SIMULATION_PATH = "/modules/flight-control/run-area-simulation";

    /** Route for consulting weather data from the Flight Control workspace. */
    public static final String FCO_CONSULT_WEATHER_DATA_PATH = "/modules/flight-control/consult-weather-data";

    /** Route for the monthly statistics report (Flight Control / FCO). */
    public static final String MONTHLY_REPORT_PATH = "/modules/flight-control/monthly-report";

    /** Route for simulation report generation/consultation (Flight Control / FCO). */
    public static final String SIMULATION_REPORT_PATH = "/modules/flight-control/simulation-report";

    /** Dashboard for the Air Transport Company module (tactical map). */
    public static final String ATCC_DASHBOARD_PATH = "/modules/air-transport-company/dashboard";

    /** Route list page for the Air Transport Company module (table view). */
    public static final String ATCC_ROUTES_PATH = "/modules/air-transport-company/routes";

    /** Route creation page for the Air Transport Company module. */
    public static final String ATCC_CREATE_FLIGHT_ROUTE_PATH = "/modules/air-transport-company/create-flight-route";

    /** Aircraft fleet page for the Air Transport Company module. */
    public static final String ATCC_AIRCRAFT_PATH = "/modules/air-transport-company/aircraft";

    /** Route deactivation page for the Air Transport Company module. */
    public static final String ATCC_DEACTIVATE_FLIGHT_ROUTE_PATH =
            "/modules/air-transport-company/deactivate-flight-route";

    /** Pilot roster page for the Air Transport Company module. */
    public static final String ATCC_PILOTS_PATH = "/modules/air-transport-company/pilots";

    public static final String WEATHER_DASHBOARD_PATH = "/modules/weather-service/dashboard";
    public static final String REGISTER_WEATHER_DATA_PATH = "/modules/weather-service/register-weather-data";
    public static final String IMPORT_BULK_WEATHER_DATA_PATH = "/modules/weather-service/import-bulk-weather-data";
    public static final String WEATHER_CONSULT_DATA_PATH = "/modules/weather-service/consult-weather-data";

    private final String id;
    private final String label;
    private final String description;
    private final String homePath;
    private final List<String> navigationLabels;
    private final Set<Role> roles;

    AisafeWebModule(final String id, final String label, final String description, final String homePath,
            final List<String> navigationLabels, final Set<Role> roles) {
        this.id = id;
        this.label = label;
        this.description = description;
        this.homePath = homePath;
        this.navigationLabels = navigationLabels;
        this.roles = roles;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public String homePath() {
        return homePath;
    }

    public boolean isBackoffice() {
        return this == BACKOFFICE;
    }

    public boolean isAccessibleBy(final Set<String> roleNames) {
        return roles.stream().anyMatch(role -> roleNames.contains(role.toString()));
    }

    public List<NavItem> navigation(final String activePage) {
        final List<NavItem> items = new ArrayList<>();
        for (int i = 0; i < navigationLabels.size(); i++) {
            final String label = navigationLabels.get(i);
            final boolean dashboard = i == 0;
            final boolean implemented = isImplementedFeature(label);
            final boolean enabled = isBackoffice() || dashboard || implemented;
            final String href;
            if (isBackoffice()) {
                href = backofficeHref(label);
            } else if (implemented) {
                href = featureHref(label);
            } else {
                href = homePath;
            }
            final String pageKey = pageKey(label);
            items.add(new NavItem(label, href, iconClass(label), pageKey.equals(activePage), enabled));
        }
        return items;
    }

    private static String backofficeHref(final String label) {
        if ("Dashboard".equals(label)) {
            return "/dashboard";
        }
        if ("Air Control Areas".equals(label)) {
            return "/areas";
        }
        if ("Airports".equals(label)) {
            return "/airports";
        }
        if ("Engine Models".equals(label)) {
            return "/engine-models";
        }
        if ("Aircraft Models".equals(label)) {
            return "/aircraft-models";
        }
        if ("Companies".equals(label)) {
            return "/companies";
        }
        if ("Collaborators".equals(label)) {
            return "/collaborators";
        }
        return "#";
    }

    /** Non-backoffice feature pages that have a real route wired (everything else lands on the module dashboard). */
    private boolean isImplementedFeature(final String label) {
        return (this == PILOT_OPERATIONS && ("Create Flight".equals(label)
                        || "Import Flight Plan".equals(label)
                        || "Consult Weather Data".equals(label)
                        || "Add Weather Data".equals(label)
                        || "Test Flight Plan".equals(label)
                        || "Simulate Flight Plan".equals(label)))
                || (this == FLIGHT_CONTROL && ("Consult Weather Data".equals(label)
                        || "Monthly Report".equals(label)
                        || "Simulation Report".equals(label)
                        || "Create Area Simulation".equals(label)
                        || "View Area Simulation".equals(label)))
                || (this == WEATHER_SERVICE && ("Register Weather Data".equals(label)
                        || "Import Bulk Data".equals(label)
                        || "Consult Weather Data".equals(label)))
                || (this == AIR_TRANSPORT_COMPANY && ("Aircraft".equals(label)
                        || "Create Flight Route".equals(label)
                        || "Deactivate Flight Route".equals(label)
                        || "Pilots".equals(label)));
    }

    private String featureHref(final String label) {
        if (this == PILOT_OPERATIONS && "Create Flight".equals(label)) {
            return CREATE_FLIGHT_PATH;
        }
        if (this == PILOT_OPERATIONS && "Import Flight Plan".equals(label)) {
            return IMPORT_FLIGHT_PLAN_PATH;
        }
        if (this == PILOT_OPERATIONS && "Consult Weather Data".equals(label)) {
            return PILOT_CONSULT_WEATHER_DATA_PATH;
        }
        if (this == PILOT_OPERATIONS && "Add Weather Data".equals(label)) {
            return ADD_WEATHER_DATA_PATH;
        }
        if (this == PILOT_OPERATIONS && "Test Flight Plan".equals(label)) {
            return TEST_FLIGHT_PLAN_PATH;
        }
        if (this == PILOT_OPERATIONS && "Simulate Flight Plan".equals(label)) {
            return SIMULATE_FLIGHT_PLAN_PATH;
        }
        if (this == FLIGHT_CONTROL && "Consult Weather Data".equals(label)) {
            return FCO_CONSULT_WEATHER_DATA_PATH;
        }
        if (this == FLIGHT_CONTROL && "View Area Simulation".equals(label)) {
            return SIMULATE_AREA_FLIGHTS_PATH;
        }
        if (this == FLIGHT_CONTROL && "Create Area Simulation".equals(label)) {
            return RUN_AREA_SIMULATION_PATH;
        }
        if (this == FLIGHT_CONTROL && "Monthly Report".equals(label)) {
            return MONTHLY_REPORT_PATH;
        }
        if (this == FLIGHT_CONTROL && "Simulation Report".equals(label)) {
            return SIMULATION_REPORT_PATH;
        }
        if (this == WEATHER_SERVICE && "Register Weather Data".equals(label)) {
            return REGISTER_WEATHER_DATA_PATH;
        }
        if (this == WEATHER_SERVICE && "Import Bulk Data".equals(label)) {
            return IMPORT_BULK_WEATHER_DATA_PATH;
        }
        if (this == WEATHER_SERVICE && "Consult Weather Data".equals(label)) {
            return WEATHER_CONSULT_DATA_PATH;
        }
        if (this == AIR_TRANSPORT_COMPANY && "Create Flight Route".equals(label)) {
            return ATCC_CREATE_FLIGHT_ROUTE_PATH;
        }
        if (this == AIR_TRANSPORT_COMPANY && "Aircraft".equals(label)) {
            return ATCC_AIRCRAFT_PATH;
        }
        if (this == AIR_TRANSPORT_COMPANY && "Deactivate Flight Route".equals(label)) {
            return ATCC_DEACTIVATE_FLIGHT_ROUTE_PATH;
        }
        if (this == AIR_TRANSPORT_COMPANY && "Pilots".equals(label)) {
            return ATCC_PILOTS_PATH;
        }
        if (this == AIR_TRANSPORT_COMPANY && "Routes".equals(label)) {
            return ATCC_ROUTES_PATH;
        }
        return homePath;
    }

    private static String pageKey(final String label) {
        // keep the existing active-page keys (and controller/template references)
        // stable even though the display labels were renamed.
        return switch (label) {
            case "Create Area Simulation" -> "run-area-simulation";
            case "View Area Simulation" -> "simulate-area-flights";
            default -> label.toLowerCase().replace(" ", "-");
        };
    }

    private static String iconClass(final String label) {
        return switch (label) {
            case "Dashboard" -> "dashboard";
            case "Air Control Areas" -> "air-control-areas";
            case "Register Weather Data" -> "register-weather-data";
            case "Import Bulk Data" -> "import-weather-data";
            case "Consult Weather Data" -> "consult-weather-data";
            case "Create Flight" -> "create-flight";
            case "Import Flight Plan" -> "import-flight-plan";
            case "Add Weather Data" -> "add-flight-weather";
            case "Test Flight Plan" -> "test-flight-plan";
            case "Create Flight Route" -> "create-flight-route";
            case "Deactivate Flight Route" -> "deactivate-flight-route";
            case "Airports" -> "airports";
            case "Aircraft Models" -> "aircraft-models";
            case "Simulate Flight Plan" -> "simulate-flight-plan";
            case "View Area Simulation" -> "simulate-area-flights";
            case "Create Area Simulation" -> "run-area-simulation";
            case "Aircraft" -> "aircraft-models";
            case "Active Flights" -> "aircraft";
            case "Engine Models" -> "engine-models";
            case "Forecast Inputs" -> "engine";
            case "Companies" -> "air-transport-companies";
            case "Monthly Report" -> "monthly-report";
            case "Simulation Report" -> "simulation-report";
            case "Routes" -> "grid";
            case "Pilots" -> "pilots";
            case "Collaborators" -> "collaborators";
            case "Users" -> "users";
            case "Roles" -> "people";
            default -> "user";
        };
    }

    public record ModuleOption(String id, String label, String description, String href, boolean active) {
    }

    public record NavItem(String label, String href, String iconClass, boolean active, boolean enabled) {
    }
}
