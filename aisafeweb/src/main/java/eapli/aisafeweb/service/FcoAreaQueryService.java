package eapli.aisafeweb.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.customercollaboratormanagement.domain.AreaCollaborator;
import eapli.aisafe.flightmanagement.application.AreaCandidateFlightsService;
import eapli.aisafe.flightmanagement.application.AreaCandidateFlightsService.CandidateFlight;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafeweb.dsl.DslFlightPlanCompiler;

/**
 * Read model for the Flight Control (FCO) workspace. Two foundations the FC
 * dashboard is built on:
 *
 * <ol>
 *   <li><b>Area resolution</b> — the FCO controls exactly one air control area;
 *   {@link #areaOf(String)} resolves it from their {@link AreaCollaborator}
 *   record (the collaborator&nbsp;&rarr;&nbsp;ACA link), so the dashboard can be
 *   scoped to "my area".</li>
 *   <li><b>Simulation candidates</b> — {@link #candidates} lists the TESTED
 *   flights crossing that area in a time window, the flights the FCO would feed to
 *   an area simulation. It delegates to the core {@link AreaCandidateFlightsService}
 *   with the web {@link DslFlightPlanCompiler}, so the crossing geometry is shared
 *   with the (future) egress US.</li>
 * </ol>
 */
@Service
public class FcoAreaQueryService {

    /** Default planning horizon for the candidate window when the caller gives none. */
    private static final int DEFAULT_WINDOW_HOURS = 24;

    private final DslFlightPlanCompiler compiler;

    public FcoAreaQueryService(final DslFlightPlanCompiler compiler) {
        this.compiler = compiler;
    }

    /** An air control area as the dashboard shows it: code + name. */
    public record AreaView(String code, String name) {
    }

    /** The FCO's air control area code, from their area-collaborator record. */
    public Optional<AreaCode> areaOf(final String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return PersistenceContext.repositories().customerCollaborators()
                .findAreaCollaboratorByEmail(Email.valueOf(email))
                .map(AreaCollaborator::areaCode);
    }

    /** The FCO's area as a display view (code + name), empty if unresolved/unknown. */
    public Optional<AreaView> resolveArea(final String email) {
        return areaOf(email).flatMap(code ->
                PersistenceContext.repositories().airControlAreas().findByCode(code)
                        .map(area -> new AreaView(area.code().toString(), area.name().toString())));
    }

    /**
     * TESTED flights crossing the FCO's own area within {@code [from, to]}
     * (defaults to the next {@value #DEFAULT_WINDOW_HOURS}h when both are null).
     * Empty when the FCO has no resolvable area.
     */
    public List<CandidateFlight> candidates(final String email,
            final LocalDateTime from, final LocalDateTime to) {
        return areaOf(email)
                .map(code -> candidatesForArea(code, from, to))
                .orElseGet(List::of);
    }

    /** TESTED flights crossing the given area within the window — backs an area picker too. */
    public List<CandidateFlight> candidatesForArea(final AreaCode areaCode,
            final LocalDateTime from, final LocalDateTime to) {
        final LocalDateTime start = from != null ? from : LocalDateTime.now();
        final LocalDateTime end = to != null ? to
                : (from != null ? null : start.plusHours(DEFAULT_WINDOW_HOURS));
        return service().candidates(areaCode, start, end);
    }

    private AreaCandidateFlightsService service() {
        return new AreaCandidateFlightsService(
                PersistenceContext.repositories().flights(),
                PersistenceContext.repositories().airControlAreas(),
                compiler);
    }
}