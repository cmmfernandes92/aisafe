// AISafe — Simulator kernel
// Pure, renderer-agnostic logic shared by simulator.html (3D Three.js)
// and simulator-2d.html (Leaflet). Ports flight_proc.c + violation.c.

// ============================================================
// AREAS — kept in sync with the back-office area registry
// ============================================================
export const AREAS = [
  { code:'LFEE', name:'Paris ACC',     swLat:46.800, swLon:-1.000, neLat:50.500, neLon: 5.500, airports:14 },
  { code:'EDUU', name:'Munich ACC',    swLat:47.200, swLon: 8.000, neLat:51.000, neLon:14.000, airports:12 },
  { code:'LIRR', name:'Rome ACC',      swLat:40.000, swLon: 8.000, neLat:46.000, neLon:14.000, airports:10 },
  { code:'LECM', name:'Madrid ACC',    swLat:36.000, swLon:-9.000, neLat:43.500, neLon: 3.000, airports: 9 },
  { code:'EBBU', name:'Brussels ACC',  swLat:49.500, swLon: 2.500, neLat:51.500, neLon: 6.500, airports: 7 },
  { code:'LTAA', name:'Istanbul ACC',  swLat:39.800, swLon:26.000, neLat:42.500, neLon:32.500, airports: 6 },
  { code:'LSAS', name:'Zurich ACC',    swLat:45.800, swLon: 5.800, neLat:47.800, neLon:10.500, airports: 5 },
  { code:'LMMM', name:'Malta ACC',     swLat:35.000, swLon:12.000, neLat:37.000, neLon:16.000, airports: 4 },
  { code:'LFMM', name:'Marseille ACC', swLat:42.500, swLon:-1.000, neLat:46.500, neLon: 8.000, airports: 4 },
  { code:'EDWW', name:'Bremen ACC',    swLat:51.000, swLon: 4.000, neLat:55.000, neLon:10.000, airports: 3 },
  { code:'LPPC', name:'Lisbon ACC',    swLat:36.500, swLon:-9.500, neLat:42.200, neLon:-6.200, airports: 3 },
  { code:'EHAA', name:'Amsterdam ACC', swLat:51.000, swLon: 3.000, neLat:53.700, neLon: 7.500, airports: 2 },
];

// Presets defined as fractions of half-extent so they adapt to any sector.
// Velocities: vx/vy in nm-per-step, vz in ft-per-step.
export const PRESETS = {
  patrol: {
    name: 'Standard Patrol',
    desc: 'Parallel eastbound cruise · clean',
    totalSteps: 20, maxViolations: 3,
    flights: [
      { id:101, fx:-0.7, fy:-0.2, alt:35000, vx: 8, vy: 0, vz:0 },
      { id:102, fx:-0.7, fy: 0.0, alt:36000, vx: 8, vy: 0, vz:0 },
      { id:103, fx:-0.7, fy: 0.2, alt:37000, vx: 8, vy: 0, vz:0 },
    ],
  },
  crossing: {
    name: 'Crossing Conflict',
    desc: '2 flights crossing at same FL',
    totalSteps: 14, maxViolations: 10,
    flights: [
      { id:201, fx:-0.6, fy: 0.0, alt:34000, vx: 8, vy: 0, vz:0 },
      { id:202, fx: 0.0, fy:-0.6, alt:34000, vx: 0, vy: 8, vz:0 },
      { id:203, fx: 0.7, fy: 0.5, alt:38000, vx:-6, vy:-2, vz:0 },
    ],
  },
  descent: {
    name: 'Descent Conflict',
    desc: 'High flight descending into low flight',
    totalSteps: 16, maxViolations: 4,
    flights: [
      { id:301, fx:-0.3, fy: 0.1, alt:38000, vx: 4, vy: 0, vz:-700 },
      { id:302, fx: 0.4, fy: 0.0, alt:32000, vx:-4, vy: 0, vz:   0 },
      { id:303, fx: 0.0, fy:-0.6, alt:36000, vx: 0, vy: 6, vz: 200 },
    ],
  },
  hold: {
    name: 'Stack & Hold',
    desc: 'Staggered FLs over a fix · clean',
    totalSteps: 24, maxViolations: 3,
    flights: [
      { id:401, fx:-0.2, fy: 0.2, alt:24000, vx: 2, vy:-1, vz:-300 },
      { id:402, fx: 0.2, fy:-0.2, alt:28000, vx:-2, vy: 1, vz:-300 },
      { id:403, fx:-0.2, fy:-0.2, alt:32000, vx: 1, vy: 1, vz:-400 },
      { id:404, fx: 0.2, fy: 0.2, alt:36000, vx:-1, vy:-1, vz:-200 },
    ],
  },
  heavy: {
    name: 'Heavy Traffic',
    desc: '6 flights · mixed vectors, likely conflicts',
    totalSteps: 18, maxViolations: 6,
    flights: [
      { id:501, fx:-0.7, fy: 0.0, alt:33000, vx: 7, vy: 0, vz:0 },
      { id:502, fx: 0.7, fy: 0.1, alt:33000, vx:-7, vy: 0, vz:0 },
      { id:503, fx: 0.0, fy:-0.7, alt:35000, vx: 0, vy: 7, vz:0 },
      { id:504, fx: 0.0, fy: 0.7, alt:37000, vx: 0, vy:-7, vz:-200 },
      { id:505, fx:-0.5, fy:-0.5, alt:39000, vx: 5, vy: 5, vz:0 },
      { id:506, fx: 0.5, fy:-0.5, alt:41000, vx:-5, vy: 5, vz:-300 },
    ],
  },
  threshold: {
    name: 'Threshold Test',
    desc: 'Forces termination on first violation',
    totalSteps: 12, maxViolations: 0,
    flights: [
      { id:601, fx:-0.5, fy: 0.0, alt:34000, vx: 6, vy: 0, vz:0 },
      { id:602, fx: 0.5, fy: 0.0, alt:34000, vx:-6, vy: 0, vz:0 },
      { id:603, fx: 0.0, fy:-0.6, alt:38000, vx: 0, vy: 5, vz:0 },
    ],
  },
};

// Flight palette (shared)
export const PALETTE = ['#00E5FF','#FFB547','#7B61FF','#3EE6A8','#FF4D9D','#60A5FA','#FACC15','#A78BFA'];

export const TRAIL_LEN = 160;

// ============================================================
// Coordinate mapping
// ============================================================
export function areaBBox(area) {
  const centerLat = (area.swLat + area.neLat) / 2;
  const centerLon = (area.swLon + area.neLon) / 2;
  const cosLat = Math.cos(centerLat * Math.PI / 180);
  const wNm = (area.neLon - area.swLon) * 60 * cosLat;
  const dNm = (area.neLat - area.swLat) * 60;
  return { centerLat, centerLon, cosLat, wNm, dNm };
}

// fx/fy in -1..1 of half-extent → sim x/y in nm (relative to area center)
export function fracToXY(area, fx, fy) {
  const { wNm, dNm } = areaBBox(area);
  return { x: fx * (wNm / 2) * 0.9, y: fy * (dNm / 2) * 0.9 };
}

// sim x/y (nm, relative to area center) → lat/lon
export function xyToLatLon(area, x, y) {
  const { centerLat, centerLon, cosLat } = areaBBox(area);
  return {
    lat: centerLat + (y / 60),
    lon: centerLon + (x / (60 * cosLat)),
  };
}

// ============================================================
// Kernel factory — creates a sim state + step/reset functions
// ============================================================
export function createSim() {
  const sim = {
    area: null,
    bbox: null,
    preset: null,
    hMin: 8.0,      // nm
    vMin: 600.0,    // ft
    maxViolations: 3,
    totalSteps: 20,
    step: 0,
    flights: [],
    notified: {},
    violationCount: 0,
    active: {},
    endReason: null,
    log: [],
  };

  function setArea(code) {
    const area = AREAS.find(a => a.code === code);
    if (!area) return null;
    sim.area = area;
    const m = areaBBox(area);
    sim.bbox = { xMin:-m.wNm/2, xMax:m.wNm/2, yMin:-m.dNm/2, yMax:m.dNm/2 };
    return sim;
  }

  function loadPreset(key) {
    const p = PRESETS[key];
    if (!p || !sim.area) return null;
    sim.preset = key;
    sim.totalSteps = p.totalSteps;
    sim.maxViolations = p.maxViolations;
    const fls = p.flights.map((f, i) => {
      const { x, y } = fracToXY(sim.area, f.fx, f.fy);
      return makeFlight({
        id: f.id, x, y, alt: f.alt,
        vx: f.vx, vy: f.vy, vz: f.vz,
        color: PALETTE[i % PALETTE.length],
      });
    });
    sim.flights = fls;
    sim.flights.forEach((f, i) => f.index = i);
    resetState();
    return p;
  }

  function makeFlight(spec) {
    return {
      index: 0,
      id: spec.id,
      x: spec.x, y: spec.y, alt: spec.alt,
      vx: spec.vx, vy: spec.vy, vz: spec.vz,
      prev: { x: spec.x, y: spec.y, alt: spec.alt },
      color: spec.color,
      outcome: 'IN_FLIGHT',
      alive: true,
      history: [{ x:spec.x, y:spec.y, alt:spec.alt }],
      initial: { ...spec },
    };
  }

  function addFlight(spec) {
    const color = PALETTE[sim.flights.length % PALETTE.length];
    const f = makeFlight({ ...spec, color });
    f.index = sim.flights.length;
    sim.flights.push(f);
    return f;
  }

  function removeFlight(flight) {
    sim.flights = sim.flights.filter(f => f !== flight);
    sim.flights.forEach((f, i) => f.index = i);
  }

  function resetState() {
    sim.step = 0;
    sim.notified = {};
    sim.violationCount = 0;
    sim.active = {};
    sim.endReason = null;
    sim.log = [];
    for (const f of sim.flights) {
      const s = f.initial;
      f.x = s.x; f.y = s.y; f.alt = s.alt;
      f.vx = s.vx; f.vy = s.vy; f.vz = s.vz;
      f.prev.x = s.x; f.prev.y = s.y; f.prev.alt = s.alt;
      f.history = [{ x:s.x, y:s.y, alt:s.alt }];
      f.alive = true; f.outcome = 'IN_FLIGHT';
    }
  }

  function step() {
    if (sim.endReason) return;
    if (!sim.flights.length) return;
    if (sim.step >= sim.totalSteps) {
      sim.endReason = 'NORMAL';
      for (const f of sim.flights) if (f.alive) f.outcome = 'COMPLETED';
      return;
    }
    for (const f of sim.flights) {
      if (!f.alive) continue;
      f.prev.x = f.x; f.prev.y = f.y; f.prev.alt = f.alt;
      f.x   += f.vx;
      f.y   += f.vy;
      f.alt += f.vz;
      f.history.push({ x:f.x, y:f.y, alt:f.alt });
      if (f.history.length > TRAIL_LEN) f.history.shift();
    }
    const newActive = {};
    for (let i=0;i<sim.flights.length;i++) {
      for (let j=i+1;j<sim.flights.length;j++) {
        const a = sim.flights[i], b = sim.flights[j];
        if (!a.alive || !b.alive) continue;
        const h = Math.hypot(a.x-b.x, a.y-b.y);
        const v = Math.abs(a.alt-b.alt);
        const inViol = h < sim.hMin && v < sim.vMin;
        const key = `${i}-${j}`;
        if (inViol) {
          newActive[key] = { i, j, h, v };
          if (!sim.notified[key]) {
            sim.notified[key] = true;
            sim.violationCount++;
            sim.log.push({ step: sim.step+1, id1: a.id, id2: b.id, h, v });
            if (sim.violationCount > sim.maxViolations) {
              sim.endReason = 'THRESHOLD_EXCEEDED';
              for (const f of sim.flights) if (f.alive) f.outcome = 'TERMINATED';
            }
          }
        } else if (sim.notified[key]) {
          sim.notified[key] = false;
        }
      }
    }
    sim.active = newActive;
    sim.step += 1;
    if (sim.step >= sim.totalSteps && !sim.endReason) {
      sim.endReason = 'NORMAL';
      for (const f of sim.flights) if (f.alive) f.outcome = 'COMPLETED';
    }
  }

  return { sim, setArea, loadPreset, addFlight, removeFlight, resetState, step };
}
