package eapli.aisafeweb.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import eapli.aisafe.aircontrolareamanagement.domain.AirControlArea;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModel;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftType;
import eapli.aisafe.airportmanagement.domain.Airport;
import eapli.aisafe.airtransportcompanymanagement.domain.AirTransportCompany;
import eapli.aisafe.countrymanagement.domain.Country;
import eapli.aisafe.customercollaboratormanagement.domain.AreaCollaborator;
import eapli.aisafe.customercollaboratormanagement.domain.Collaborator;
import eapli.aisafe.customercollaboratormanagement.domain.CompanyCollaborator;
import eapli.aisafe.enginemodelmanagement.domain.EngineModel;
import eapli.aisafe.enginemodelmanagement.domain.FuelType;
import eapli.aisafe.enginemodelmanagement.domain.MotorizationType;
import eapli.aisafe.flightroutemanagement.domain.FlightRoute;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.makermanagement.domain.Maker;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.aisafe.usermanagement.domain.User;
import eapli.framework.infrastructure.authz.domain.model.Role;

@Service
public class BackofficeQueryService {

    public DashboardSnapshot dashboard() {
        final List<User> users = toList(PersistenceContext.repositories().aisafeUsers().findAll());
        final List<AircraftModel> models = toList(PersistenceContext.repositories().aircraftModels().findAll());
        final List<Airport> airports = toList(PersistenceContext.repositories().airports().findAll());
        final List<AirControlArea> areas = areas();

        final LocalDate today = LocalDate.now();
        final long expiringClearance = users.stream()
                .filter(u -> !u.securityClearance().expiryDate().isAfter(today.plusDays(30)))
                .count();
        final long overdueAssessment = users.stream()
                .filter(u -> u.skillsAssessment().lastAssessmentDate().isBefore(today.minusYears(1)))
                .count();

        final List<Kpi> kpis = List.of(
                new Kpi("Active Users", users.size(), "AISafe user profiles"),
                new Kpi("Expiring Clearance", expiringClearance, "Within 30 days"),
                new Kpi("Overdue Assessment", overdueAssessment, "Older than 1 year"),
                new Kpi("Total Aircraft", models.size(), "Model-backed in Sprint 2"),
                new Kpi("Aircraft Models", models.size(), "Registered models"),
                new Kpi("Airports", airports.size(), "Registered airports"));

        return new DashboardSnapshot(
                kpis,
                areas,
                airports,
                areaMapViews(areas, airports),
                airportMapViews(airports),
                safeRouteViews(),
                airportsPerArea(areas, airports),
                aircraftModelsByType(models));
    }

    public List<AirControlArea> areas() {
        return toList(PersistenceContext.repositories().airControlAreas().findAll()).stream()
                .sorted(Comparator.comparing(a -> a.code().toString()))
                .toList();
    }

    public List<Airport> airports() {
        return toList(PersistenceContext.repositories().airports().findAll()).stream()
                .sorted(Comparator.comparing(a -> a.icaoCode().toString()))
                .toList();
    }

    public List<Country> countries() {
        return toList(PersistenceContext.repositories().countries().findAll()).stream()
                .sorted(Comparator.comparing(c -> c.code().toString()))
                .toList();
    }

    public List<AirTransportCompany> companies() {
        return toList(PersistenceContext.repositories().airTransportCompanies().findAll()).stream()
                .sorted(Comparator.comparing(c -> c.iataCode().toString()))
                .toList();
    }

    public AreaPageSnapshot areaPage() {
        final List<AirControlArea> areas = areas();
        final List<Airport> airports = airports();
        return new AreaPageSnapshot(areas, airports, airportsPerArea(areas, airports));
    }

    public AirportPageSnapshot airportPage() {
        return new AirportPageSnapshot(airportRows(), areas(), countries());
    }

    public CompanyPageSnapshot companyPage() {
        return new CompanyPageSnapshot(companyRows());
    }

    public CollaboratorPageSnapshot collaboratorPage() {
        return new CollaboratorPageSnapshot(
                collaboratorRows(),
                companies(),
                areas(),
                collaboratorUserOptions(AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR),
                collaboratorUserOptions(AisafeRoles.FLIGHT_CONTROL_OPERATOR));
    }

    public List<Maker> makers() {
        return toList(PersistenceContext.repositories().makers().findAll()).stream()
                .sorted(Comparator.comparing(m -> m.identity().toString()))
                .toList();
    }

    public List<EngineModel> engineModels() {
        return toList(PersistenceContext.repositories().engineModels().findAll()).stream()
                .sorted(Comparator.comparing(e -> e.engineModelName().toString()))
                .toList();
    }

    public List<EngineModel> engineModelsByMotorizationType(final MotorizationType type) {
        return toList(PersistenceContext.repositories().engineModels().findByMotorizationType(type)).stream()
                .sorted(Comparator.comparing(e -> e.engineModelName().toString()))
                .toList();
    }

    public EngineModelPageSnapshot engineModelPage() {
        return new EngineModelPageSnapshot(engineModelRows(), makers());
    }

    public List<EngineModelRow> engineModelRows() {
        return engineModels().stream()
                .map(e -> new EngineModelRow(
                        e.identity(),
                        e.engineModelName().toString(),
                        e.maker().identity().toString(),
                        e.motorizationType(),
                        e.fuelType(),
                        e.power(),
                        e.efficiency()))
                .toList();
    }

    public AircraftModelPageSnapshot aircraftModelPage() {
        return new AircraftModelPageSnapshot(aircraftModelRows(), makers(), engineModelRows());
    }

    public List<AircraftModelRow> aircraftModelRows() {
        return toList(PersistenceContext.repositories().aircraftModels().findAll()).stream()
                .sorted(Comparator.comparing(a -> a.modelName().toString()))
                .map(a -> new AircraftModelRow(
                        a.identity(),
                        a.modelName().toString(),
                        a.maker().identity().toString(),
                        a.type(),
                        a.motorizationType(),
                        a.emptyWeight(),
                        a.mtow(),
                        a.mzfw(),
                        a.maxFuelCapacity(),
                        a.serviceCeiling(),
                        a.cruiseSpeed(),
                        a.maximumRange(),
                        a.certifiedEngines().stream()
                                .map(e -> e.engineModelName().toString())
                                .sorted()
                                .collect(java.util.stream.Collectors.joining(", "))))
                .toList();
    }

    public List<CompanyRow> companyRows() {
        return companies().stream()
                .map(company -> new CompanyRow(
                        company.iataCode().toString(),
                        company.icaoCode().toString(),
                        company.companyName()))
                .toList();
    }

    public List<CollaboratorRow> collaboratorRows() {
        final Map<String, String> companyNames = new LinkedHashMap<>();
        for (final AirTransportCompany company : companies()) {
            companyNames.put(company.iataCode().toString(), company.companyName());
        }

        final Map<String, String> areaNames = new LinkedHashMap<>();
        for (final AirControlArea area : areas()) {
            areaNames.put(area.code().toString(), area.name().toString());
        }

        return toList(PersistenceContext.repositories().customerCollaborators().findAll()).stream()
                .sorted(Comparator.comparing(c -> c.identity().toString()))
                .map(collaborator -> collaboratorRow(collaborator, companyNames, areaNames))
                .toList();
    }

    public RouteMapSnapshot routeMapPage() {
        return routeMapPage(null);
    }

    public List<FlightRouteView> routeList(final String companyIata) {
        List<FlightRouteView> routes = safeRouteViews();
        if (companyIata != null) {
            routes = routes.stream()
                    .filter(r -> companyIata.equalsIgnoreCase(r.companyIata()))
                    .toList();
        }
        return routes;
    }

    public RouteMapSnapshot routeMapPage(final String companyIata) {
        final List<AirControlArea> areas = areas();
        final List<Airport> airports = airports();
        List<FlightRouteView> routes = safeRouteViews();
        if (companyIata != null) {
            routes = routes.stream()
                    .filter(r -> companyIata.equalsIgnoreCase(r.companyIata()))
                    .toList();
        }
        return new RouteMapSnapshot(areaMapViews(areas, airports), airportMapViews(airports), routes);
    }

    private List<FlightRouteView> safeRouteViews() {
        try {
            return routeViews(toList(PersistenceContext.repositories().flightRoutes().findAll()));
        } catch (final Exception e) {
            return List.of();
        }
    }

    private List<FlightRouteView> routeViews(final List<FlightRoute> routes) {
        return routes.stream()
                .map(r -> new FlightRouteView(
                        r.routeName().toString(),
                        r.company().iataCode().toString(),
                        r.company().companyName(),
                        r.origin().iataCode().toString(),
                        r.origin().icaoCode().toString(),
                        r.origin().coordinates().latitude(),
                        r.origin().coordinates().longitude(),
                        r.destination().iataCode().toString(),
                        r.destination().icaoCode().toString(),
                        r.destination().coordinates().latitude(),
                        r.destination().coordinates().longitude(),
                        r.isActive()))
                .toList();
    }

    public List<AirportRow> airportRows() {
        final Map<String, String> countryNames = new LinkedHashMap<>();
        for (final Country country : countries()) {
            countryNames.put(country.code().toString(), country.name().toString());
        }
        return airports().stream()
                .map(airport -> new AirportRow(
                        airport.icaoCode().toString(),
                        airport.iataCode().toString(),
                        airport.airportName().toString(),
                        airport.town().toString(),
                        countryNames.getOrDefault(airport.countryCode().toString(),
                                airport.countryCode().toString()),
                        airport.areaCode().toString(),
                        airport.elevation().value(),
                        airport.coordinates().latitude(),
                        airport.coordinates().longitude()))
                .toList();
    }

    private List<ChartBar> airportsPerArea(final List<AirControlArea> areas, final List<Airport> airports) {
        final Map<String, Long> counts = new LinkedHashMap<>();
        for (final AirControlArea area : areas) {
            counts.put(area.code().toString(), 0L);
        }
        for (final Airport airport : airports) {
            counts.computeIfPresent(airport.areaCode().toString(), (k, v) -> v + 1);
        }
        return counts.entrySet().stream()
                .map(e -> new ChartBar(e.getKey(), e.getValue()))
                .toList();
    }

    private List<ChartBar> aircraftModelsByType(final List<AircraftModel> models) {
        final Map<String, Long> counts = new LinkedHashMap<>();
        for (final AircraftModel model : models) {
            counts.merge(model.type().name(), 1L, Long::sum);
        }
        return counts.entrySet().stream()
                .map(e -> new ChartBar(e.getKey(), e.getValue()))
                .toList();
    }

    private List<AreaMapView> areaMapViews(final List<AirControlArea> areas, final List<Airport> airports) {
        final Map<String, Long> airportCounts = new LinkedHashMap<>();
        for (final AirControlArea area : areas) {
            airportCounts.put(area.code().toString(), 0L);
        }
        for (final Airport airport : airports) {
            airportCounts.computeIfPresent(airport.areaCode().toString(), (k, v) -> v + 1);
        }

        return areas.stream()
                .map(area -> new AreaMapView(
                        area.code().toString(),
                        area.name().toString(),
                        area.boundary().southwest().latitude(),
                        area.boundary().southwest().longitude(),
                        area.boundary().northeast().latitude(),
                        area.boundary().northeast().longitude(),
                        (area.boundary().southwest().latitude() + area.boundary().northeast().latitude()) / 2,
                        (area.boundary().southwest().longitude() + area.boundary().northeast().longitude()) / 2,
                        airportCounts.getOrDefault(area.code().toString(), 0L)))
                .toList();
    }

    private List<AirportMapView> airportMapViews(final List<Airport> airports) {
        return airports.stream()
                .map(airport -> new AirportMapView(
                        airport.icaoCode().toString(),
                        airport.iataCode().toString(),
                        airport.airportName().toString(),
                        airport.coordinates().latitude(),
                        airport.coordinates().longitude(),
                        airport.areaCode().toString()))
                .toList();
    }

    private CollaboratorRow collaboratorRow(
            final Collaborator collaborator,
            final Map<String, String> companyNames,
            final Map<String, String> areaNames) {
        final String type;
        final String assignmentCode;
        final String assignmentName;
        if (collaborator instanceof CompanyCollaborator companyCollaborator) {
            type = "Company";
            assignmentCode = companyCollaborator.companyCode().toString();
            assignmentName = companyNames.getOrDefault(assignmentCode, assignmentCode);
        } else if (collaborator instanceof AreaCollaborator areaCollaborator) {
            type = "Area";
            assignmentCode = areaCollaborator.areaCode().toString();
            assignmentName = areaNames.getOrDefault(assignmentCode, assignmentCode);
        } else {
            type = "Unknown";
            assignmentCode = "";
            assignmentName = "";
        }

        return new CollaboratorRow(
                collaborator.identity().toString(),
                type,
                collaborator.position().toString(),
                assignmentCode,
                assignmentName,
                collaborator.contactEmail().toString(),
                collaborator.contactPhoneNumber().toString(),
                collaborator.status().toString());
    }

    private List<UserOption> collaboratorUserOptions(final Role role) {
        final Set<String> linkedEmails = new HashSet<>();
        for (final Collaborator collaborator : toList(PersistenceContext.repositories().customerCollaborators().findAll())) {
            linkedEmails.add(collaborator.identity().toString());
        }

        return toList(PersistenceContext.repositories().aisafeUsers().findAll()).stream()
                .filter(user -> user.hasRole(role))
                .filter(user -> !linkedEmails.contains(user.identity().toString()))
                .sorted(Comparator.comparing(user -> user.identity().toString()))
                .map(user -> new UserOption(
                        user.identity().toString(),
                        user.phoneNumber().toString()))
                .toList();
    }

    private static <T> List<T> toList(final Iterable<T> source) {
        final List<T> result = new ArrayList<>();
        source.forEach(result::add);
        return result;
    }

    public record Kpi(String label, long value, String caption) {
    }

    public record ChartBar(String label, long value) {
    }

    public record AreaMapView(
            String code,
            String name,
            double swLat,
            double swLon,
            double neLat,
            double neLon,
            double centerLat,
            double centerLon,
            long airportCount) {
    }

    public record AirportMapView(
            String icaoCode,
            String iataCode,
            String name,
            double latitude,
            double longitude,
            String areaCode) {
    }

    public record DashboardSnapshot(
            List<Kpi> kpis,
            List<AirControlArea> areas,
            List<Airport> airports,
            List<AreaMapView> areaMap,
            List<AirportMapView> airportMap,
            List<FlightRouteView> routes,
            List<ChartBar> airportsPerArea,
            List<ChartBar> aircraftModelsByType) {
    }

    public record AirportRow(
            String icaoCode,
            String iataCode,
            String name,
            String town,
            String country,
            String areaCode,
            double elevationMeters,
            double latitude,
            double longitude) {
    }

    public record AreaPageSnapshot(
            List<AirControlArea> areas,
            List<Airport> airports,
            List<ChartBar> airportsPerArea) {
    }

    public record AirportPageSnapshot(
            List<AirportRow> airports,
            List<AirControlArea> areas,
            List<Country> countries) {
    }

    public record CompanyRow(
            String iataCode,
            String icaoCode,
            String name) {
    }

    public record CompanyPageSnapshot(
            List<CompanyRow> companies) {
    }

    public record UserOption(
            String email,
            String phoneNumber) {
    }

    public record CollaboratorRow(
            String email,
            String type,
            String position,
            String assignmentCode,
            String assignmentName,
            String contactEmail,
            String contactPhoneNumber,
            String status) {
    }

    public record CollaboratorPageSnapshot(
            List<CollaboratorRow> collaborators,
            List<AirTransportCompany> companies,
            List<AirControlArea> areas,
            List<UserOption> companyUsers,
            List<UserOption> areaUsers) {
    }

    public record FlightRouteView(
            String routeName,
            String companyIata,
            String companyName,
            String originIata,
            String originIcao,
            double originLat,
            double originLon,
            String destinationIata,
            String destinationIcao,
            double destinationLat,
            double destinationLon,
            boolean active) {
    }

    public record RouteMapSnapshot(
            List<AreaMapView> areas,
            List<AirportMapView> airports,
            List<FlightRouteView> routes) {
    }

    public record EngineModelRow(
            Long id,
            String name,
            String makerName,
            MotorizationType motorizationType,
            FuelType fuelType,
            double power,
            double efficiency) {
    }

    public record EngineModelPageSnapshot(
            List<EngineModelRow> engineModels,
            List<Maker> makers) {
    }

    public record AircraftModelRow(
            Long id,
            String name,
            String makerName,
            AircraftType type,
            MotorizationType motorizationType,
            double emptyWeight,
            double mtow,
            double mzfw,
            double maxFuelCapacity,
            double serviceCeiling,
            double cruiseSpeed,
            double maximumRange,
            String certifiedEngines) {
    }

    public record AircraftModelPageSnapshot(
            List<AircraftModelRow> aircraftModels,
            List<Maker> makers,
            List<EngineModelRow> engineModels) {
    }
}
