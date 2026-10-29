// AISafe — Flight Simulator v0.2 (MapLibre GL + Three.js custom layer)
//
// Plan-driven: the route geometry, per-segment altitude & wind, departure/arrival
// times and loaded fuel come from a real VALIDATED/TESTED flight plan injected by
// the server as `window.AISAFE_SIM_PLAN`; the fuel burn is computed per the
// requirements §3.3 physics. With no plan selected it falls back to a demo voyage.
//
// ⚠️ v0.2 VISUALISATION ONLY — it never changes plan state. The physics constants
// not stored in our domain (aspect ratio, Oswald e, Cd₀, 2nd thrust point) come
// from the server's published-constants table. See .carlos/design-notes.md §2.
// TODO US085: once the flight-test use case ships, drive the burn/telemetry from
// that controller instead of this client-side copy.

import * as THREE from 'three';

const M_TO_FT = 1 / 0.3048;
const NM_TO_M = 1852;
const MS_TO_KT = 1.94384;
const DEG = Math.PI / 180;
const G = 9.81;

// ---- the plan (server-injected) or a demo fallback (same shape) ----
const DEMO_PLAN = {
  designator: 'TP1923', originCode: 'LPPR', destinationCode: 'LPPT',
  departureIso: '2026-06-12T13:00', arrivalIso: '2026-06-12T13:50', fuelKg: 3300,
  segments: [
    { startLat: 41.2481, startLon: -8.6814, endLat: 40.0100, endLon: -8.9000, altitudeM: 9000, windDirDeg: 350, windSpeedMs: 8.0 },
    { startLat: 40.0100, startLon: -8.9000, endLat: 38.7742, endLon: -9.1342, altitudeM: 9000, windDirDeg: 340, windSpeedMs: 6.5 },
  ],
  aircraft: {
    model: 'A320neo (demo)', wingArea: 122.6, cruiseSpeedMs: 231, emptyWeightKg: 42600,
    mzfwKg: 64300, mtowKg: 79000, ceilingM: 12131, totalSeats: 150, thrustMaxN: 130000,
    numEngines: 2, tsfcNNs: 0.51 / 3600, cd0: 0.02, aspectRatio: 10.45, oswaldE: 0.8,
    dragCoefficient: 0.024, liftCoefficient: 1.5,
  },
};
const PLAN = window.AISAFE_SIM_PLAN || DEMO_PLAN;
const AC = PLAN.aircraft;
const SEGS = PLAN.segments;

const CLIMB_FRAC = 0.12, DESC_FRAC = 0.12;   // share of the route used for climb / descent ramps
const PAX_MASS_KG = 100;

const ui = {
  show: { buildings: true, terrain: false, trail: true, labels: true, route: true },
  basemap: 'dark', camera: 'free', playing: false, rate: 1, exag: 1,
};

// ---- geo helpers ----
function haversineNm(a, b) {
  const R = 6371000;
  const dLat = (b.lat - a.lat) * DEG, dLon = (b.lon - a.lon) * DEG;
  const s = Math.sin(dLat / 2) ** 2 + Math.cos(a.lat * DEG) * Math.cos(b.lat * DEG) * Math.sin(dLon / 2) ** 2;
  return (2 * R * Math.asin(Math.sqrt(s))) / NM_TO_M;
}
function bearingDeg(a, b) {
  const y = Math.sin((b.lon - a.lon) * DEG) * Math.cos(b.lat * DEG);
  const x = Math.cos(a.lat * DEG) * Math.sin(b.lat * DEG) - Math.sin(a.lat * DEG) * Math.cos(b.lat * DEG) * Math.cos((b.lon - a.lon) * DEG);
  return (Math.atan2(y, x) / DEG + 360) % 360;
}
const lerp = (a, b, t) => a + (b - a) * t;
const easeIO = t => t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;

// ============================================================
// Route geometry from the plan's segments
// ============================================================
// polyline waypoints = [seg0.start, seg0.end, seg1.end, ...]
const WPTS = [{ lat: SEGS[0].startLat, lon: SEGS[0].startLon },
  ...SEGS.map(s => ({ lat: s.endLat, lon: s.endLon }))];
const LEG_NM = [];
const CUM_NM = [0];
for (let i = 0; i < WPTS.length - 1; i++) {
  const d = haversineNm(WPTS[i], WPTS[i + 1]);
  LEG_NM.push(d); CUM_NM.push(CUM_NM[i] + d);
}
const TOTAL_NM = CUM_NM[CUM_NM.length - 1] || 1;
const ORIGIN_WP = WPTS[0], DEST_WP = WPTS[WPTS.length - 1];
const CRUISE_M = Math.max(...SEGS.map(s => s.altitudeM));

function segIndexAt(p) {
  const d = Math.max(0, Math.min(TOTAL_NM, p * TOTAL_NM));
  for (let i = 0; i < LEG_NM.length; i++) if (d <= CUM_NM[i + 1] + 1e-9) return i;
  return LEG_NM.length - 1;
}
function posAt(p) {
  const d = Math.max(0, Math.min(TOTAL_NM, p * TOTAL_NM));
  const i = segIndexAt(p);
  const t = LEG_NM[i] > 0 ? (d - CUM_NM[i]) / LEG_NM[i] : 0;
  return { lon: lerp(WPTS[i].lon, WPTS[i + 1].lon, t), lat: lerp(WPTS[i].lat, WPTS[i + 1].lat, t), seg: i };
}
function headingAt(p) {
  const i = segIndexAt(p);
  return bearingDeg(WPTS[i], WPTS[i + 1]);
}
function altMAt(p) {
  if (p <= 0) return 0;
  if (p < CLIMB_FRAC) return SEGS[0].altitudeM * easeIO(p / CLIMB_FRAC);
  if (p > 1 - DESC_FRAC) return SEGS[SEGS.length - 1].altitudeM * easeIO(Math.max(0, (1 - p) / DESC_FRAC));
  return SEGS[segIndexAt(p)].altitudeM;
}
function windAt(p) {
  const s = SEGS[segIndexAt(p)];
  return { dir: s.windDirDeg, speed: s.windSpeedMs };
}

// ---- clock (departure → arrival, linear in progress) ----
const DEP_MS = Date.parse(PLAN.departureIso);
const ARR_MS = Date.parse(PLAN.arrivalIso);
const FLIGHT_SECONDS = Math.max(60, (ARR_MS - DEP_MS) / 1000 || 3000);
function clockAt(p) {
  const d = new Date(DEP_MS + p * (ARR_MS - DEP_MS));
  if (Number.isNaN(d.getTime())) return '—';
  const pad = n => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

// ============================================================
// Fuel burn — requirements §3.3 (Euler integration, see design-notes §2)
// ============================================================
function isaDensity(altM) {
  let T, p;
  if (altM <= 11000) { T = 288.2 - 0.0065 * altM; p = 101325 * Math.pow(1 - 0.0065 * altM / 288.2, 5.2561); }
  else { T = 216.65; p = 22632 * Math.exp(-G * (altM - 11000) / (287 * T)); }
  return p / (287 * T);
}
// Precompute remaining-fuel vs progress so the HUD can read it at any p (incl. scrub).
const FUEL_TL = (() => {
  const payload = Math.min(AC.totalSeats * PAX_MASS_KG, Math.max(0, AC.mzfwKg - AC.emptyWeightKg));
  let mass = AC.emptyWeightKg + payload + PLAN.fuelKg;
  let fuel = PLAN.fuelKg;
  const v = AC.cruiseSpeedMs;                 // TAS ≈ cruise speed (v0.2; climb/descent use same v)
  const steps = 240, dt = FLIGHT_SECONDS / steps;
  const tl = [{ p: 0, fuel }];
  for (let i = 1; i <= steps; i++) {
    const p = i / steps;
    const rho = isaDensity(altMAt(p));
    const CL = (2 * mass * G) / (rho * AC.wingArea * v * v);
    const CD = AC.cd0 + (CL * CL) / (Math.PI * AC.aspectRatio * AC.oswaldE);
    const D = 0.5 * rho * v * v * AC.wingArea * CD;
    const thrust = D;                          // cruise: level flight ⇒ thrust = drag (TODO climb excess)
    const burn = (thrust * dt * AC.tsfcNNs) / G;
    fuel = Math.max(0, fuel - burn); mass -= burn;
    tl.push({ p, fuel });
  }
  return tl;
})();
function fuelAt(p) {
  const x = Math.max(0, Math.min(1, p)) * (FUEL_TL.length - 1);
  const i = Math.floor(x), f = x - i;
  if (i >= FUEL_TL.length - 1) return FUEL_TL[FUEL_TL.length - 1].fuel;
  return lerp(FUEL_TL[i].fuel, FUEL_TL[i + 1].fuel, f);
}
function gsKtAt(p) {
  const ph = phaseFor(p);
  if (ph === 'On stand' || ph === 'Arrived') return 0;
  let base = AC.cruiseSpeedMs * MS_TO_KT;
  if (ph === 'Take-off') base *= 0.4; else if (ph === 'Climb') base *= 0.78;
  else if (ph === 'Descent') base *= 0.82; else if (ph === 'Landing') base *= 0.35;
  const w = windAt(p), track = headingAt(p), windTo = (w.dir + 180) % 360;
  const tail = w.speed * MS_TO_KT * Math.cos((track - windTo) * DEG); // tailwind +, headwind −
  return Math.max(0, base + tail);
}
function phaseFor(p) {
  if (!ui.playing && p === 0) return 'On stand';
  if (p <= 0) return 'On stand';
  if (p < 0.02) return 'Take-off';
  if (p < CLIMB_FRAC) return 'Climb';
  if (p < 1 - DESC_FRAC) return 'Cruise';
  if (p < 0.985) return 'Descent';
  if (p < 1) return 'Landing';
  return 'Arrived';
}

// ---- flight state ----
const flight = {
  p: 0, lon: ORIGIN_WP.lon, lat: ORIGIN_WP.lat, altM: 0,
  gs: 0, phase: 'On stand', done: false, history: [],
};
const DURATION = 60 + TOTAL_NM * 0.28;   // animation seconds at 1× (scales with route length)

// ============================================================
// MapLibre base map
// ============================================================
const TRACK0 = bearingDeg(ORIGIN_WP, DEST_WP);
const map = new maplibregl.Map({
  container: 'map',
  style: 'https://tiles.openfreemap.org/styles/liberty',
  center: [(ORIGIN_WP.lon + DEST_WP.lon) / 2, (ORIGIN_WP.lat + DEST_WP.lat) / 2],
  zoom: 7.4, pitch: 58, bearing: TRACK0 - 6,
  antialias: true, maxPitch: 84, attributionControl: true, preserveDrawingBuffer: true,
});
window.__map = map;
map.on('error', e => { try { window.__mapErr = (window.__mapErr || '') + ' | ' + (e.error?.message || e.type); } catch (x) {} });
map.addControl(new maplibregl.NavigationControl({ visualizePitch: true }), 'bottom-right');
map.dragRotate.enable();
map.touchZoomRotate.enableRotation();

// --- CS2-style follow-camera input (active only while in follow mode) ---
let followZoom = null, followDrag = false, _fx = 0, _fy = 0;
const _cv = map.getCanvas();
_cv.addEventListener('wheel', e => {
  if (ui.camera !== 'follow') return;
  e.preventDefault();
  followZoom = Math.min(15, Math.max(5.5, (followZoom ?? map.getZoom()) - e.deltaY * 0.0022));
}, { passive: false });
_cv.addEventListener('pointerdown', e => { if (ui.camera === 'follow') { followDrag = true; _fx = e.clientX; _fy = e.clientY; } });
window.addEventListener('pointerup', () => { followDrag = false; });
window.addEventListener('pointermove', e => {
  if (ui.camera !== 'follow' || !followDrag) return;
  const dx = e.clientX - _fx, dy = e.clientY - _fy; _fx = e.clientX; _fy = e.clientY;
  map.setBearing(map.getBearing() - dx * 0.35);
  map.setPitch(Math.min(82, Math.max(0, map.getPitch() + dy * 0.25)));
});

const mapEl = document.getElementById('map');
new ResizeObserver(() => map.resize()).observe(mapEl);
window.addEventListener('resize', () => map.resize());
new MutationObserver(() => setTimeout(() => map.resize(), 220)).observe(document.body, { attributes: true, attributeFilter: ['class'] });
[60, 300, 800].forEach(ms => setTimeout(() => map.resize(), ms));

// ============================================================
// Local ENU frame (metres relative to the route mid-point)
// ============================================================
const origin = { lat: (ORIGIN_WP.lat + DEST_WP.lat) / 2, lon: (ORIGIN_WP.lon + DEST_WP.lon) / 2 };
const originMerc = maplibregl.MercatorCoordinate.fromLngLat([origin.lon, origin.lat], 0);
const mScale = originMerc.meterInMercatorCoordinateUnits();
const cosLat = Math.cos(origin.lat * DEG);
function toModel(lon, lat, altM) {
  const E = (lon - origin.lon) * 111320 * cosLat;
  const N = (lat - origin.lat) * 110540;
  return new THREE.Vector3(E, altM * ui.exag, -N);
}

// ============================================================
// Three.js scene
// ============================================================
const scene = new THREE.Scene();
scene.add(new THREE.HemisphereLight(0xcfe4ff, 0x223044, 1.15));
const dirLight = new THREE.DirectionalLight(0xffffff, 1.5);
dirLight.position.set(0.4, 1, 0.5).multiplyScalar(10000);
scene.add(dirLight);

const planeGroup = new THREE.Group();
scene.add(planeGroup);

let three = { camera: null, renderer: null };
const customLayer = {
  id: 'aisafe-plane', type: 'custom', renderingMode: '3d',
  onAdd(m, gl) {
    three.camera = new THREE.Camera();
    three.renderer = new THREE.WebGLRenderer({ canvas: m.getCanvas(), context: gl, antialias: true });
    three.renderer.autoClear = false;
  },
  render(gl, matrix) {
    const rotX = new THREE.Matrix4().makeRotationAxis(new THREE.Vector3(1, 0, 0), Math.PI / 2);
    const l = new THREE.Matrix4().makeTranslation(originMerc.x, originMerc.y, originMerc.z)
      .scale(new THREE.Vector3(mScale, -mScale, mScale)).multiply(rotX);
    three.camera.projectionMatrix = new THREE.Matrix4().fromArray(matrix).multiply(l);
    three.renderer.resetState();
    three.renderer.render(scene, three.camera);
    map.triggerRepaint();
  },
};

// ---- simple low-poly airliner (≈A320 proportions, metres). Nose points toward -Z. ----
function buildPlaneModel() {
  const g = new THREE.Group();
  const skin = new THREE.MeshStandardMaterial({ color: 0xe9eef7, metalness: 0.35, roughness: 0.45 });
  const accent = new THREE.MeshStandardMaterial({ color: 0x00E5FF, emissive: 0x00E5FF, emissiveIntensity: 0.4, metalness: 0.3, roughness: 0.4 });
  const dark = new THREE.MeshStandardMaterial({ color: 0x1a2336, metalness: 0.2, roughness: 0.6 });
  const fus = new THREE.Mesh(new THREE.CylinderGeometry(2.0, 2.0, 34, 18), skin);
  fus.rotation.x = Math.PI / 2; g.add(fus);
  const nose = new THREE.Mesh(new THREE.ConeGeometry(2.0, 7, 18), dark);
  nose.rotation.x = -Math.PI / 2; nose.position.z = -20.5; g.add(nose);
  const tailCone = new THREE.Mesh(new THREE.ConeGeometry(2.0, 7, 18), skin);
  tailCone.rotation.x = Math.PI / 2; tailCone.position.z = 20.5; g.add(tailCone);
  const wing = new THREE.Mesh(new THREE.BoxGeometry(34, 0.8, 7), accent);
  wing.position.set(0, -0.4, 1); g.add(wing);
  const hstab = new THREE.Mesh(new THREE.BoxGeometry(13, 0.6, 3.6), accent);
  hstab.position.set(0, 0.4, 17); g.add(hstab);
  const fin = new THREE.Mesh(new THREE.BoxGeometry(0.7, 7, 5), accent);
  fin.position.set(0, 4.2, 18); g.add(fin);
  for (const sx of [-9, 9]) {
    const eng = new THREE.Mesh(new THREE.CylinderGeometry(1.4, 1.4, 5, 12), dark);
    eng.rotation.x = Math.PI / 2; eng.position.set(sx, -2, 1.5); g.add(eng);
  }
  return g;
}

let planeMesh, trailLine, dropLine, labelSprite;
function makeLabelSprite(text) {
  const fs = 36, pad = 10;
  const c = document.createElement('canvas');
  let ctx = c.getContext('2d');
  ctx.font = `600 ${fs}px 'JetBrains Mono', monospace`;
  const w = ctx.measureText(text).width;
  c.width = w + pad * 2; c.height = fs + pad * 2;
  ctx = c.getContext('2d');
  ctx.font = `600 ${fs}px 'JetBrains Mono', monospace`;
  ctx.fillStyle = 'rgba(7,11,20,0.82)';
  rr(ctx, 0, 0, c.width, c.height, 9); ctx.fill();
  ctx.strokeStyle = '#00E5FF'; ctx.lineWidth = 2.5;
  rr(ctx, 1.2, 1.2, c.width - 2.4, c.height - 2.4, 9); ctx.stroke();
  ctx.fillStyle = '#E5ECF7'; ctx.textBaseline = 'middle';
  ctx.fillText(text, pad, c.height / 2 + 1);
  const tex = new THREE.CanvasTexture(c); tex.colorSpace = THREE.SRGBColorSpace;
  const spr = new THREE.Sprite(new THREE.SpriteMaterial({ map: tex, depthTest: false, transparent: true }));
  const targetH = 7000;
  spr.scale.set((c.width / c.height) * targetH, targetH, 1);
  spr.renderOrder = 999;
  return spr;
}
function rr(ctx, x, y, w, h, r) {
  ctx.beginPath();
  ctx.moveTo(x + r, y); ctx.arcTo(x + w, y, x + w, y + h, r); ctx.arcTo(x + w, y + h, x, y + h, r);
  ctx.arcTo(x, y + h, x, y, r); ctx.arcTo(x, y, x + w, y, r); ctx.closePath();
}

function buildScene() {
  [...planeGroup.children].forEach(o => { planeGroup.remove(o); o.traverse?.(c => { c.geometry?.dispose?.(); c.material?.map?.dispose?.(); c.material?.dispose?.(); }); });
  const wrap = new THREE.Group();
  planeMesh = buildPlaneModel();
  wrap.add(planeMesh);
  planeGroup.add(wrap);
  planeGroup.userData.wrap = wrap;
  dropLine = new THREE.Line(
    new THREE.BufferGeometry().setFromPoints([new THREE.Vector3(), new THREE.Vector3()]),
    new THREE.LineBasicMaterial({ color: 0x00E5FF, transparent: true, opacity: 0.25 })
  );
  planeGroup.add(dropLine);
  labelSprite = makeLabelSprite(`${PLAN.designator} · FL000`);
  planeGroup.add(labelSprite);
  trailLine = new THREE.Line(
    new THREE.BufferGeometry().setFromPoints([new THREE.Vector3()]),
    new THREE.LineBasicMaterial({ color: 0x7B61FF, transparent: true, opacity: 0.85 })
  );
  scene.add(trailLine);
}
buildScene();

// ============================================================
// Frame update
// ============================================================
function updateScene() {
  const pos = toModel(flight.lon, flight.lat, flight.altM);
  const wrap = planeGroup.userData.wrap;
  planeGroup.position.copy(pos);

  const zsc = Math.min(300, Math.max(9, Math.pow(2, 12.5 - map.getZoom())));
  planeMesh.scale.setScalar(zsc);

  wrap.rotation.set(0, 0, 0);
  wrap.rotateY(-headingAt(flight.p) * DEG);
  const pAhead = Math.min(1, flight.p + 0.01);
  const ahead = posAt(pAhead);
  const climbM = (altMAt(pAhead) - flight.altM) * ui.exag;
  const horizM = haversineNm({ lon: flight.lon, lat: flight.lat }, ahead) * NM_TO_M || 1;
  wrap.rotateX(Math.atan2(climbM, horizM) * 0.8);

  dropLine.visible = !ui.show.terrain;
  const arr = dropLine.geometry.attributes.position.array;
  arr[0] = 0; arr[1] = 0; arr[2] = 0; arr[3] = 0; arr[4] = -pos.y; arr[5] = 0;
  dropLine.geometry.attributes.position.needsUpdate = true;

  labelSprite.visible = ui.show.labels;
  labelSprite.position.set(0, 11000, 0);

  trailLine.visible = ui.show.trail;
  if (flight.history.length > 1) {
    const pts = flight.history.map(h => toModel(h.lon, h.lat, h.altM));
    pts.push(pos);
    trailLine.geometry.dispose();
    trailLine.geometry = new THREE.BufferGeometry().setFromPoints(pts);
  }
}

let last = performance.now();
function loop(now) {
  const dt = Math.min(0.1, (now - last) / 1000); last = now;
  if (ui.playing && !flight.done) {
    flight.p = Math.min(1, flight.p + (dt * ui.rate) / DURATION);
    const s = posAt(flight.p);
    flight.lon = s.lon; flight.lat = s.lat; flight.altM = altMAt(flight.p);
    flight.gs = lerp(flight.gs, gsKtAt(flight.p), 0.15);
    if (!flight.history.length || flight.p - flight.history[flight.history.length - 1].p > 0.004) {
      flight.history.push({ lon: flight.lon, lat: flight.lat, altM: flight.altM, p: flight.p });
    }
    flight.phase = phaseFor(flight.p);
    if (flight.p >= 1) { flight.done = true; ui.playing = false; flight.phase = 'Arrived'; flight.gs = 0; }
    updateHUD();
  }
  updateScene();
  if (ui.camera === 'follow' && followZoom != null && !followDrag) {
    const planeAltM = Math.max(0, flight.altM * ui.exag);
    const mpp = 40075016.686 * Math.cos(flight.lat * DEG) / (512 * Math.pow(2, followZoom));
    const offsetPx = (planeAltM / mpp) * Math.sin(map.getPitch() * DEG);
    const maxPad = map.getCanvas().clientHeight * 0.8;
    const padTop = Math.max(0, Math.min(maxPad, 2 * offsetPx));
    map.jumpTo({
      center: [flight.lon, flight.lat], zoom: followZoom,
      bearing: map.getBearing(), pitch: map.getPitch(),
      padding: { top: padTop, bottom: 0, left: 0, right: 0 },
    });
  }
  requestAnimationFrame(loop);
}
requestAnimationFrame(loop);

// ============================================================
// Map layers
// ============================================================
function routeGeoJSON() {
  return { type: 'FeatureCollection', features: [{ type: 'Feature', geometry: { type: 'LineString', coordinates: WPTS.map(w => [w.lon, w.lat]) } }] };
}
function airportsGeoJSON() {
  return { type: 'FeatureCollection', features: [
    { type: 'Feature', properties: { code: PLAN.originCode }, geometry: { type: 'Point', coordinates: [ORIGIN_WP.lon, ORIGIN_WP.lat] } },
    { type: 'Feature', properties: { code: PLAN.destinationCode }, geometry: { type: 'Point', coordinates: [DEST_WP.lon, DEST_WP.lat] } },
  ] };
}

function addGeoLayers() {
  if (!map.getSource('esri-sat')) map.addSource('esri-sat', { type: 'raster', tileSize: 256, tiles: ['https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}'], attribution: 'Imagery © Esri, Maxar, Earthstar Geographics' });
  if (!map.getSource('carto-dark')) map.addSource('carto-dark', { type: 'raster', tileSize: 256, tiles: ['https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/{z}/{y}/{x}'], attribution: 'Tiles © Esri — Esri, HERE, Garmin, © OpenStreetMap contributors' });
  if (!map.getSource('terrain-dem')) map.addSource('terrain-dem', { type: 'raster-dem', tileSize: 256, encoding: 'terrarium', maxzoom: 13, tiles: ['https://s3.amazonaws.com/elevation-tiles-prod/terrarium/{z}/{x}/{y}.png'] });

  const layers = map.getStyle().layers || [];
  const beforeId = layers.find(l => l.type === 'symbol')?.id;

  if (!map.getLayer('esri-sat-layer')) map.addLayer({ id: 'esri-sat-layer', type: 'raster', source: 'esri-sat', layout: { visibility: ui.basemap === 'sat' ? 'visible' : 'none' } }, beforeId);
  if (!map.getLayer('carto-dark-layer')) map.addLayer({ id: 'carto-dark-layer', type: 'raster', source: 'carto-dark', layout: { visibility: ui.basemap === 'dark' ? 'visible' : 'none' } }, beforeId);

  if (!map.getLayer('buildings-3d')) map.addLayer({
    id: 'buildings-3d', type: 'fill-extrusion', source: 'openmaptiles', 'source-layer': 'building', minzoom: 12,
    paint: {
      'fill-extrusion-color': ['case', ['has', 'colour'], ['get', 'colour'], '#8493ad'],
      'fill-extrusion-height': ['coalesce', ['get', 'render_height'], ['get', 'height'], 8],
      'fill-extrusion-base': ['coalesce', ['get', 'render_min_height'], ['get', 'min_height'], 0],
      'fill-extrusion-opacity': 0.9,
    }, layout: { visibility: ui.show.buildings ? 'visible' : 'none' },
  });

  if (!map.getSource('route')) map.addSource('route', { type: 'geojson', data: routeGeoJSON() });
  if (!map.getLayer('route-line')) map.addLayer({ id: 'route-line', type: 'line', source: 'route', paint: { 'line-color': '#00E5FF', 'line-width': 2, 'line-dasharray': [2, 2], 'line-opacity': 0.8 } });

  if (!map.getSource('airports')) map.addSource('airports', { type: 'geojson', data: airportsGeoJSON() });
  if (!map.getLayer('airport-dot')) map.addLayer({ id: 'airport-dot', type: 'circle', source: 'airports', paint: { 'circle-radius': 5, 'circle-color': '#FFB547', 'circle-stroke-width': 2, 'circle-stroke-color': '#070B14' } });
  if (!map.getLayer('airport-label')) map.addLayer({ id: 'airport-label', type: 'symbol', source: 'airports', layout: { 'text-field': ['get', 'code'], 'text-size': 12, 'text-offset': [0, 1.4], 'text-font': ['Noto Sans Bold'] }, paint: { 'text-color': '#FFB547', 'text-halo-color': '#070B14', 'text-halo-width': 1.4 } });

  if (!map.getLayer('aisafe-plane')) map.addLayer(customLayer);
}

function setBasemap(b) {
  ui.basemap = b;
  if (map.getLayer('esri-sat-layer')) map.setLayoutProperty('esri-sat-layer', 'visibility', b === 'sat' ? 'visible' : 'none');
  if (map.getLayer('carto-dark-layer')) map.setLayoutProperty('carto-dark-layer', 'visibility', b === 'dark' ? 'visible' : 'none');
  document.querySelectorAll('#basemap-seg .seg-btn').forEach(el => el.classList.toggle('on', el.dataset.base === b));
}
function setTerrain(on) {
  if (!map.getSource('terrain-dem')) return;
  map.setTerrain(on ? { source: 'terrain-dem', exaggeration: 1.3 } : null);
}
function applyToggles() {
  if (map.getLayer('buildings-3d')) map.setLayoutProperty('buildings-3d', 'visibility', ui.show.buildings ? 'visible' : 'none');
  if (map.getLayer('route-line')) map.setLayoutProperty('route-line', 'visibility', ui.show.route ? 'visible' : 'none');
  setTerrain(ui.show.terrain);
}

function setCamera(mode) {
  ui.camera = mode;
  document.querySelectorAll('#camera-seg .seg-btn').forEach(el => el.classList.toggle('on', el.dataset.cam === mode));
  if (mode === 'follow') {
    followZoom = Math.max(10.3, map.getZoom());
    map.dragPan.disable(); map.dragRotate.disable(); map.scrollZoom.disable(); map.touchZoomRotate.disable();
    map.setPitch(64); map.setBearing(headingAt(flight.p) - 8);
  } else {
    map.dragPan.enable(); map.dragRotate.enable(); map.scrollZoom.enable(); map.touchZoomRotate.enable();
    map.jumpTo({ padding: { top: 0, bottom: 0, left: 0, right: 0 } });
  }
}

// ============================================================
// HUD
// ============================================================
function notice(msg, warn = false) {
  const n = document.getElementById('notice');
  n.textContent = msg; n.classList.toggle('warn', warn); n.classList.add('show');
  clearTimeout(notice._t); notice._t = setTimeout(() => n.classList.remove('show'), 1800);
}
function setText(id, v) { const el = document.getElementById(id); if (el) el.textContent = v; }
function updateHUD() {
  setText('kpi-alt', `${Math.round(flight.altM * M_TO_FT / 100) * 100} ft`);
  setText('kpi-spd', `${Math.max(0, Math.round(flight.gs))} kt`);
  const fuel = fuelAt(flight.p);
  const fuelPct = PLAN.fuelKg > 0 ? Math.max(0, Math.min(1, fuel / PLAN.fuelKg)) : 0;
  setText('kpi-fuel', `${Math.round(fuel)} kg`);
  setText('kpi-fuel-pct', `${Math.round(fuelPct * 100)}%`);
  const fuelEl = document.getElementById('kpi-fuel'); if (fuelEl) fuelEl.classList.toggle('low', fuelPct < 0.15);
  const fbar = document.getElementById('kpi-fuel-bar');
  if (fbar) {
    fbar.style.width = `${(fuelPct * 100).toFixed(1)}%`;
    fbar.style.background = fuelPct < 0.15 ? 'linear-gradient(90deg,#FF4D9D,#FFB547)'
      : fuelPct < 0.35 ? 'linear-gradient(90deg,#FFB547,#3EE6A8)'
        : 'linear-gradient(90deg,#3EE6A8,#00E5FF)';
  }
  const w = windAt(flight.p);
  setText('kpi-wind-dir', `${String(Math.round(w.dir)).padStart(3, '0')}°`);
  setText('kpi-wind-spd', `${w.speed.toFixed(1)} m/s`);
  setText('kpi-clock', clockAt(flight.p));
  setText('kpi-dist', `${((1 - flight.p) * TOTAL_NM).toFixed(0)} nm rem`);
  const prog = document.getElementById('kpi-prog'); if (prog) prog.style.width = `${(flight.p * 100).toFixed(1)}%`;
  setText('active-phase', flight.phase.toLowerCase());
  const ps = document.getElementById('p-prog');
  if (ps && document.activeElement !== ps) { ps.value = flight.p * 100; setText('p-prog-v', (flight.p * 100).toFixed(0) + '%'); }
  labelSprite && relabel();

  const phasePill = document.getElementById('kpi-phase');
  if (phasePill) {
    phasePill.textContent = flight.phase;
    phasePill.className = 'status-pill ' + (flight.phase === 'Cruise' ? 'running' : flight.phase === 'Arrived' ? 'done' : flight.phase === 'On stand' ? 'idle' : 'paused');
  }
  const pill = document.getElementById('kpi-status');
  if (pill) {
    pill.className = 'status-pill';
    if (flight.done) { pill.classList.add('done'); pill.textContent = 'Arrived'; }
    else if (ui.playing) { pill.classList.add('running'); pill.textContent = 'In flight'; }
    else if (flight.p > 0) { pill.classList.add('paused'); pill.textContent = 'Paused'; }
    else { pill.classList.add('idle'); pill.textContent = 'Ready'; }
  }
  const btn = document.getElementById('btn-play');
  if (btn) btn.textContent = flight.done ? '↻ Replay' : ui.playing ? '⏸ Pause' : '▶ Fly';
}
let _lastLabel = '';
function relabel() {
  const t = `${PLAN.designator} · FL${String(Math.round(flight.altM * M_TO_FT / 100)).padStart(3, '0')}`;
  if (t === _lastLabel) return;
  _lastLabel = t;
  const old = labelSprite;
  labelSprite = makeLabelSprite(t);
  labelSprite.position.copy(old.position); labelSprite.visible = old.visible;
  planeGroup.add(labelSprite);
  planeGroup.remove(old); old.material.map.dispose(); old.material.dispose();
}

function scrubTo(p) {
  ui.playing = false;
  flight.p = Math.max(0, Math.min(1, p));
  const s = posAt(flight.p);
  flight.lon = s.lon; flight.lat = s.lat; flight.altM = altMAt(flight.p);
  flight.done = flight.p >= 1;
  flight.phase = phaseFor(flight.p);
  flight.gs = gsKtAt(flight.p);
  flight.history = [];
  const N = Math.max(2, Math.round(flight.p / 0.004));
  for (let i = 0; i <= N; i++) { const pp = flight.p * i / N; const ss = posAt(pp); flight.history.push({ lon: ss.lon, lat: ss.lat, altM: altMAt(pp), p: pp }); }
  updateHUD();
}

function resetFlight() {
  flight.p = 0; flight.done = false; ui.playing = false;
  flight.lon = ORIGIN_WP.lon; flight.lat = ORIGIN_WP.lat; flight.altM = 0; flight.gs = 0;
  flight.phase = 'On stand'; flight.history = [{ lon: ORIGIN_WP.lon, lat: ORIGIN_WP.lat, altM: 0, p: 0 }];
  updateHUD();
}

// ============================================================
// Wiring
// ============================================================
function bindSlider(id, fn) {
  const el = document.getElementById(id), v = document.getElementById(id + '-v');
  if (!el) return;
  el.addEventListener('input', () => {
    const val = Number(el.value); fn(val);
    if (v) v.textContent = id === 'rate' ? val.toFixed(1) + '×' : id === 'exag' ? val + '×' : String(val);
  });
}
function togglePlay() {
  if (flight.done) { resetFlight(); ui.playing = true; }
  else ui.playing = !ui.playing;
  updateHUD();
}
function doReset() { resetFlight(); notice(`Flight reset to ${PLAN.originCode}`); }

function wire() {
  const playBtn = document.getElementById('btn-play');
  const resetBtn = document.getElementById('btn-reset');
  // pointerup + blur: the mouse click toggles reliably and the button never keeps
  // focus (so Space can't double-fire native activation + our handler).
  playBtn.addEventListener('click', () => { togglePlay(); playBtn.blur(); });
  resetBtn.addEventListener('click', () => { doReset(); resetBtn.blur(); });

  bindSlider('rate', v => { ui.rate = v; });
  bindSlider('exag', v => { ui.exag = v; });
  bindSlider('p-prog', v => { scrubTo(v / 100); });

  document.querySelectorAll('#basemap-seg .seg-btn').forEach(el => el.addEventListener('click', () => setBasemap(el.dataset.base)));
  document.querySelectorAll('#camera-seg .seg-btn').forEach(el => el.addEventListener('click', () => setCamera(el.dataset.cam)));

  document.querySelectorAll('.chip[data-toggle]').forEach(el => el.addEventListener('click', () => {
    const k = el.dataset.toggle; ui.show[k] = !ui.show[k]; el.classList.toggle('on', ui.show[k]); applyToggles();
  }));

  window.addEventListener('keydown', e => {
    if (e.target.tagName === 'INPUT' || e.target.tagName === 'SELECT') return;
    if (e.code === 'Space') { e.preventDefault(); togglePlay(); }
    else if (e.code === 'KeyR') doReset();
    else if (e.code === 'KeyF') setCamera(ui.camera === 'follow' ? 'free' : 'follow');
  });
}

// ---- static plan read-outs in the Flight panel ----
function fillPlanPanel() {
  setText('fp-aircraft', AC.model);
  setText('fp-cruise-alt', `${Math.round(CRUISE_M)} m`);
  setText('fp-fuel0', `${Math.round(PLAN.fuelKg)} kg`);
  setText('fp-dist', `${TOTAL_NM.toFixed(0)} nm`);
}

// ============================================================
// Boot
// ============================================================
wire();
fillPlanPanel();
resetFlight();
map.on('load', () => {
  addGeoLayers();
  setBasemap(ui.basemap);
  updateHUD();
});