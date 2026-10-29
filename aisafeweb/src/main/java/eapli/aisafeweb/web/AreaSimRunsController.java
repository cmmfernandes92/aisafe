package eapli.aisafeweb.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import eapli.aisafeweb.security.AisafeWebModule;
import eapli.aisafeweb.security.AisafeWebSession;
import eapli.aisafeweb.service.AreaSimRunsService;
import eapli.aisafeweb.service.AreaSimRunsService.RunBundle;
import eapli.aisafeweb.service.AreaSimRunsService.RunSummary;
import jakarta.servlet.http.HttpSession;

/**
 * JSON feed for the area-flights 3D visualiser: lists finished SCOMP runs and
 * serves one run's bundle (scenario.json passthrough + verdict + violations) so
 * the browser kernel can replay the real run. Read-only, Flight-Control guarded.
 */
@Controller
public class AreaSimRunsController {

    private static final String RUNS_PATH = AisafeWebModule.SIMULATE_AREA_FLIGHTS_PATH + "/runs";

    private final AisafeWebSession webSession;
    private final AreaSimRunsService runs;

    public AreaSimRunsController(final AisafeWebSession webSession, final AreaSimRunsService runs) {
        this.webSession = webSession;
        this.runs = runs;
    }

    @GetMapping(RUNS_PATH)
    @ResponseBody
    public ResponseEntity<List<RunSummary>> list(final HttpSession session) {
        if (!allowed(session)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(runs.list());
    }

    @GetMapping(RUNS_PATH + "/{id}")
    @ResponseBody
    public ResponseEntity<RunBundle> bundle(@PathVariable final String id, final HttpSession session) {
        if (!allowed(session)) {
            return ResponseEntity.status(403).build();
        }
        return runs.bundle(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private boolean allowed(final HttpSession session) {
        return webSession.hasLogin(session)
                && webSession.canAccessModule(session, AisafeWebModule.FLIGHT_CONTROL);
    }
}