// AISafe — Area-simulation kernel (faithful JS port of the SCOMP C simulator)
//
// This replays a REAL FCO run: it is fed the same scenario.json the C binary
// consumed and reproduces its trajectory by porting movement.c line-for-line,
// the flight_proc.c step loop, and the detection_thread.c violation check.
//
// Units are the C's units: positions in METRES east/north relative to
// (ref_lat, ref_lon), altitude in METRES, h_min/v_min in METRES. The C is still
// authoritative for the verdict (report.txt); this kernel only drives the
// animation + the live conflict highlight.
//
// Sources ported: scomp/sprint-3/flight_sim/{movement.c, flight_proc.c,
// detection_thread.c, config.c}.

export const EARTH_RADIUS_M = 6371000.0;
const D2R = Math.PI / 180.0;
const KNOTS_TO_MS = 0.514444;
const TRAIL_LEN = 600;            // == HISTORY_SIZE

export const PALETTE = ['#00E5FF', '#FFB547', '#7B61FF', '#3EE6A8', '#FF4D9D',
  '#60A5FA', '#FACC15', '#A78BFA', '#34D399', '#F472B6'];

// ── projection (movement.c project_to_local + exact inverse) ─────────────────
export function projectToLocal(lat, lon, refLat, refLon) {
  const x = EARTH_RADIUS_M * (lon - refLon) * D2R * Math.cos(((lat + refLat) / 2.0) * D2R);
  const y = EARTH_RADIUS_M * (lat - refLat) * D2R;
  return { x, y };
}

// metres (east, north) relative to ref → lat/lon. lat is x-independent, so solve
// it first and use the same mean-latitude term project_to_local used.
export function localToLatLon(x, y, refLat, refLon) {
  const lat = refLat + (y / (EARTH_RADIUS_M * D2R));
  const lon = refLon + x / (EARTH_RADIUS_M * D2R * Math.cos(((lat + refLat) / 2.0) * D2R));
  return { lat, lon };
}

// ── profile lookup (movement.c profile_lookup) ───────────────────────────────
// NOTE: cruise speed never comes from Flight Profile.Cruise — movement.c always
// reads the climb/descend bands. Replicated so trajectories match the C.
function profileLookup(f, alt, isClimb) {
  const table = isClimb ? f.climb : f.descend;
  const n = table.length;
  if (n <= 0) return { rate_ms: 0.0, speed_ms: 0.0 };
  let best = 0;
  let bestDiff = Math.abs(table[0].altitude - alt);
  for (let i = 1; i < n; i++) {
    const diff = Math.abs(table[i].altitude - alt);
    if (diff < bestDiff) { bestDiff = diff; best = i; }
  }
  return { rate_ms: table[best].rate, speed_ms: table[best].speed_knots * KNOTS_TO_MS };
}

// ── one step of flight kinematics (movement.c advance_flight, wind = 0) ───────
function advanceFlight(f, dt) {
  if (f.segments.length <= 0) return;

  // Visualiser-only: once a flight reaches its final waypoint, hold it there.
  // The C re-targets the last segment's end every step and moves a full
  // speed*dt past it, so it oscillates back and forth (~speed*dt) at the
  // destination for the rest of the run. We park instead. Positions BEFORE
  // arrival are unchanged, so the flown path stays identical to the C.
  if (f.arrived) { f.vx = 0; f.vy = 0; f.vz = 0; return; }

  let idx = f.seg_index;
  if (idx < 0) idx = 0;
  if (idx >= f.segments.length) idx = f.segments.length - 1;
  const seg = f.segments[idx];

  let dx = seg.end_x - f.x;
  let dy = seg.end_y - f.y;
  const horiz = Math.sqrt(dx * dx + dy * dy);

  const isClimb = seg.end_alt >= f.alt;        // level/cruise ⇒ climb table (then clamp)
  const { rate_ms, speed_ms } = profileLookup(f, f.alt, isClimb);

  let avx = 0.0;
  let avy = 0.0;
  if (horiz > 1e-6) { avx = speed_ms * (dx / horiz); avy = speed_ms * (dy / horiz); }

  // wind is 0,0 → ground velocity == air velocity
  f.x += avx * dt;
  f.y += avy * dt;
  f.alt += rate_ms * dt;

  if (isClimb && f.alt > seg.end_alt) f.alt = seg.end_alt;   // clamp keeps cruise level
  if (!isClimb && f.alt < seg.end_alt) f.alt = seg.end_alt;

  f.vx = avx * dt;       // per-step deltas (metres)
  f.vy = avy * dt;
  f.vz = rate_ms * dt;

  dx = seg.end_x - f.x;
  dy = seg.end_y - f.y;
  if (Math.sqrt(dx * dx + dy * dy) < speed_ms * dt) {
    if (f.seg_index < f.segments.length - 1) {
      f.seg_index++;
    } else {
      f.x = seg.end_x;          // snap to the final waypoint and park (see top of fn)
      f.y = seg.end_y;
      f.alt = seg.end_alt;
      f.arrived = true;
    }
  }
}

// ── closest separation over a step (detection_thread.c closest_separation) ───
function closestSeparation(a, b, hasPrev) {
  if (!hasPrev) {
    const ddx = a.x - b.x;
    const ddy = a.y - b.y;
    return { h: Math.sqrt(ddx * ddx + ddy * ddy), v: Math.abs(a.alt - b.alt) };
  }
  const dpx = a.prev.x - b.prev.x;
  const dpy = a.prev.y - b.prev.y;
  const dpz = a.prev.alt - b.prev.alt;

  const dvx = (a.x - a.prev.x) - (b.x - b.prev.x);
  const dvy = (a.y - a.prev.y) - (b.y - b.prev.y);
  const dvz = (a.alt - a.prev.alt) - (b.alt - b.prev.alt);

  const dv2 = dvx * dvx + dvy * dvy + dvz * dvz;
  let t = dv2 < 1e-9 ? 0.0 : -(dpx * dvx + dpy * dvy + dpz * dvz) / dv2;
  if (t < 0.0) t = 0.0;
  if (t > 1.0) t = 1.0;

  const x1 = a.prev.x + t * (a.x - a.prev.x);
  const y1 = a.prev.y + t * (a.y - a.prev.y);
  const z1 = a.prev.alt + t * (a.alt - a.prev.alt);
  const x2 = b.prev.x + t * (b.x - b.prev.x);
  const y2 = b.prev.y + t * (b.y - b.prev.y);
  const z2 = b.prev.alt + t * (b.alt - b.prev.alt);

  const ddx = x1 - x2;
  const ddy = y1 - y2;
  return { h: Math.sqrt(ddx * ddx + ddy * ddy), v: Math.abs(z1 - z2) };
}

// ── scenario loading (config.c project_flight + json.c field mapping) ────────
function buildFlight(raw, index) {
  const profile = raw['Flight Profile'] || {};
  const climb = (profile.Climb || []).map(b => ({
    altitude: num(b.Altitude && b.Altitude.Value),
    speed_knots: num(b.Speed && b.Speed.Value),
    rate: num(b.RateClimb && b.RateClimb.Value),
  }));
  const descend = (profile.Descend || []).map(b => ({
    altitude: num(b.Altitude && b.Altitude.Value),
    speed_knots: num(b.Speed && b.Speed.Value),
    rate: num(b.RateDescent && b.RateDescent.Value),
  }));
  const segments = (raw.Segments || []).map(s => ({
    start_lat: num(s.Start.Latitude), start_lon: num(s.Start.Longitude),
    start_alt: num(s.Start.Altitude && s.Start.Altitude.Quantity),
    end_lat: num(s.End.Latitude), end_lon: num(s.End.Longitude),
    end_alt: num(s.End.Altitude && s.End.Altitude.Quantity),
    start_x: 0, start_y: 0, end_x: 0, end_y: 0,
  }));
  return {
    index,
    id: num(raw.ID),
    entry_step: Math.max(0, num(raw.EntryStep)),
    climb, descend, segments,
    seg_index: 0,
    x: 0, y: 0, alt: 0, vx: 0, vy: 0, vz: 0,
    prev: { x: 0, y: 0, alt: 0 },
    initial: { x: 0, y: 0, alt: 0 },
    active: false,
    arrived: false,
    outcome: 'IN_FLIGHT',
    history: [],
    color: PALETTE[index % PALETTE.length],
    label: String(num(raw.ID)),
  };
}

function projectFlight(f, refLat, refLon) {
  for (const seg of f.segments) {
    const a = projectToLocal(seg.start_lat, seg.start_lon, refLat, refLon);
    const b = projectToLocal(seg.end_lat, seg.end_lon, refLat, refLon);
    seg.start_x = a.x; seg.start_y = a.y;
    seg.end_x = b.x; seg.end_y = b.y;
  }
  if (f.segments.length > 0) {
    f.x = f.segments[0].start_x;
    f.y = f.segments[0].start_y;
    f.alt = f.segments[0].start_alt;
  }
  f.seg_index = 0;
  f.initial = { x: f.x, y: f.y, alt: f.alt };
}

function num(v) {
  const n = Number(v);
  return Number.isFinite(n) ? n : 0;
}

// ── sim factory ──────────────────────────────────────────────────────────────
export function createAreaSim() {
  const sim = {
    params: null,
    flights: [],
    hMin: 8.0,
    vMin: 600.0,
    maxViolations: 3,
    totalSteps: 0,
    step: 0,
    notified: {},
    altNotified: {},
    violationCount: 0,
    active: {},
    altActive: [],
    endReason: null,
    log: [],
  };

  // bundle: { scenario, params:{...}, flights:[raw], idMap:[{surrogateId,route,...}] }
  function loadScenario(bundle) {
    const p = bundle.params || {};
    sim.params = {
      name: bundle.scenario || p.name || 'scenario',
      ref_lat: num(p.ref_lat), ref_lon: num(p.ref_lon),
      total_steps: num(p.total_steps), seconds_per_step: num(p.seconds_per_step) || 60,
      max_violations: num(p.max_violations), h_min: num(p.h_min) || 8.0, v_min: num(p.v_min) || 600.0,
      area_x_min: num(p.area_x_min), area_x_max: num(p.area_x_max),
      area_y_min: num(p.area_y_min), area_y_max: num(p.area_y_max),
      // config.c defaults: floor 0, ceiling 20000 — apply when the scenario omits them
      area_alt_min: num(p.area_alt_min), area_alt_max: num(p.area_alt_max) || 20000.0,
    };
    sim.hMin = sim.params.h_min;
    sim.vMin = sim.params.v_min;
    sim.maxViolations = sim.params.max_violations;
    sim.totalSteps = sim.params.total_steps;

    const idBySurrogate = {};
    for (const e of (bundle.idMap || [])) idBySurrogate[e.surrogateId] = e.route;

    sim.flights = (bundle.flights || []).map((raw, i) => {
      const f = buildFlight(raw, i);
      projectFlight(f, sim.params.ref_lat, sim.params.ref_lon);
      if (idBySurrogate[f.id]) f.label = idBySurrogate[f.id];   // show route name when known
      return f;
    });
    resetState();
    return sim;
  }

  function resetState() {
    sim.step = 0;
    sim.notified = {};
    sim.altNotified = {};
    sim.violationCount = 0;
    sim.active = {};
    sim.altActive = [];
    sim.endReason = null;
    sim.log = [];
    for (const f of sim.flights) {
      f.x = f.initial.x; f.y = f.initial.y; f.alt = f.initial.alt;
      f.vx = 0; f.vy = 0; f.vz = 0;
      f.prev = { x: f.x, y: f.y, alt: f.alt };
      f.seg_index = 0;
      f.arrived = false;
      f.active = f.entry_step <= 0;
      f.outcome = 'IN_FLIGHT';
      f.history = [{ x: f.x, y: f.y, alt: f.alt }];
    }
  }

  function finish(reason, outcome) {
    sim.endReason = reason;
    for (const f of sim.flights) if (f.outcome === 'IN_FLIGHT') f.outcome = outcome;
  }

  // one step: advance (flight_proc.c) then detect (detection_thread.c)
  function step() {
    if (sim.endReason) return;
    if (!sim.flights.length) return;
    if (sim.step >= sim.totalSteps) { finish('NORMAL', 'COMPLETED'); return; }

    const s = sim.step;
    for (const f of sim.flights) {
      f.prev = { x: f.x, y: f.y, alt: f.alt };
      const active = s >= f.entry_step;
      if (active) advanceFlight(f, sim.params.seconds_per_step);
      else { f.vx = 0; f.vy = 0; f.vz = 0; }
      f.active = active;
      f.history.push({ x: f.x, y: f.y, alt: f.alt });
      if (f.history.length > TRAIL_LEN) f.history.shift();
    }

    detect(s);
    sim.step += 1;
    if (sim.step >= sim.totalSteps && !sim.endReason) finish('NORMAL', 'COMPLETED');
  }

  function detect(s) {
    const hasPrev = s > 0;

    // ── single-flight altitude-bound check (detection_thread.c: runs BEFORE the
    // pairwise scan, so its violations are counted first within the step) ──────
    const altMax = sim.params.area_alt_max;
    const altMin = sim.params.area_alt_min;
    const newAltActive = [];
    for (let i = 0; i < sim.flights.length; i++) {
      const f = sim.flights[i];
      // C's `active` == entered && !arrived; the JS flags it the same way
      if (!f.active || f.arrived) continue;
      let kind = null;
      let distPast = 0;
      if (f.alt > altMax) { kind = 'ALT_CEILING'; distPast = f.alt - altMax; }
      else if (f.alt < altMin) { kind = 'ALT_FLOOR'; distPast = altMin - f.alt; }
      if (!kind) { sim.altNotified[i] = false; continue; }   // not breaching: re-arm
      newAltActive.push(i);
      if (!sim.altNotified[i]) {                              // report once per sustained breach
        sim.altNotified[i] = true;
        sim.violationCount++;
        sim.log.push({ step: s, id1: f.label, id2: f.label, h: 0, v: distPast, kind });
        if (sim.violationCount > sim.maxViolations) finish('THRESHOLD_EXCEEDED', 'TERMINATED');
      }
    }
    sim.altActive = newAltActive;

    // ── pairwise separation check ──────────────────────────────────────────────
    const newActive = {};
    for (let i = 0; i < sim.flights.length; i++) {
      for (let j = i + 1; j < sim.flights.length; j++) {
        const a = sim.flights[i];
        const b = sim.flights[j];
        const key = `${i}-${j}`;
        // skip not-yet-entered flights (C's `active` gating) AND landed/parked
        // flights — the C keeps the latter oscillating ~5 km apart so they never
        // violate; our cosmetic parking would otherwise co-locate them at h=0.
        if (!a.active || !b.active || a.arrived || b.arrived) { sim.notified[key] = false; continue; }

        const { h, v } = closestSeparation(a, b, hasPrev);
        const inViolation = h < sim.hMin && v < sim.vMin;
        if (inViolation) {
          newActive[key] = { i, j, h, v };
          if (!sim.notified[key]) {
            sim.notified[key] = true;
            sim.violationCount++;
            sim.log.push({ step: s, id1: a.label, id2: b.label, h, v, kind: 'SEPARATION' });
            if (sim.violationCount > sim.maxViolations) {
              finish('THRESHOLD_EXCEEDED', 'TERMINATED');
            }
          }
        } else {
          sim.notified[key] = false;
        }
      }
    }
    sim.active = newActive;
  }

  return { sim, loadScenario, resetState, step };
}