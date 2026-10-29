// AISafe — Area-flights 3D viewer (MapLibre GL + Three.js custom layer)
//
// Replays a REAL FCO area simulation: the user picks a finished SCOMP run, the
// server serves that run's scenario.json (+ verdict/violations), and the
// sim-area-kernel — a faithful port of movement.c — reproduces the trajectory
// over actual terrain. The C stays authoritative for the verdict; this view
// reads it from report.txt and only animates + highlights conflicts live.

import { createAreaSim, localToLatLon } from './sim-area-kernel.js?v=20260614-glow';
import * as THREE from 'three';

const RUNS_URL = '/modules/flight-control/simulate-area-flights/runs';

const { sim, loadScenario, resetState, step } = createAreaSim();

const ui = {
  show: { buildings: true, terrain: false, zones: true, trails: true, links: true, drops: true, labels: true, sector: true },
  basemap: 'dark',
  playing: false,
  rate: 2,
  exag: 1,
  trailSteps: 10,   // how many of the latest steps the path keeps lit (UI slider)
  timeAccum: 0,
};

// the recent path is drawn as a string of soft glow sprites (same look as the
// flight marker) sampled evenly along it, fading out toward the tail.
const TRAIL_DOTS = 30;       // glow sprites per flight along the path
const TRAIL_DOT_HEAD = 7000; // sprite diameter (m) nearest the flight
const TRAIL_DOT_TAIL = 2400; // sprite diameter (m) at the fading tail

let currentRun = null;   // RunBundle currently loaded
let runList = [];

// ============================================================
// MapLibre base map
// ============================================================
const map = new maplibregl.Map({
  container: 'map',
  style: 'https://tiles.openfreemap.org/styles/liberty',
  center: [-7.85, 39.35],
  zoom: 6.2,
  pitch: 62,
  bearing: -17,
  antialias: true,
  maxPitch: 82,
  attributionControl: true,
  preserveDrawingBuffer: true,
});
map.addControl(new maplibregl.NavigationControl({ visualizePitch: true }), 'bottom-right');
map.dragRotate.enable();
map.touchZoomRotate.enableRotation();

const mapEl = document.getElementById('map');
const ro = new ResizeObserver(() => map.resize());
ro.observe(mapEl);
window.addEventListener('resize', () => map.resize());
new MutationObserver(() => setTimeout(() => map.resize(), 220))
  .observe(document.body, { attributes: true, attributeFilter: ['class'] });
[60, 300, 800].forEach(ms => setTimeout(() => map.resize(), ms));

// ============================================================
// Local frame — metres relative to the run's projection origin (ref_lat/ref_lon)
// ============================================================
let origin = { lat: 39.35, lon: -7.85 };
let originMerc = null, mScale = 1;
function setOrigin(lat, lon) {
  origin = { lat, lon };
  originMerc = maplibregl.MercatorCoordinate.fromLngLat([lon, lat], 0);
  mScale = originMerc.meterInMercatorCoordinateUnits();
}
setOrigin(39.5, -10.5);

// kernel (x, y) are metres east/north relative to the origin; alt in metres.
// Model axes (MapLibre custom-layer convention, rotateX(π/2), scale(s,-s,s)):
//   X = east (m), Y = up/altitude (m), Z = south (m) = -north
function toModel(xEast, yNorth, altM) {
  return new THREE.Vector3(xEast, altM * ui.exag, -yNorth);
}

// ============================================================
// Three.js scene (rendered through MapLibre's matrix)
// ============================================================
const scene = new THREE.Scene();
scene.add(new THREE.HemisphereLight(0xcfe4ff, 0x223044, 1.1));
const dir = new THREE.DirectionalLight(0xffffff, 1.4);
dir.position.set(0.5, 1, 0.4).multiplyScalar(10000);
scene.add(dir);

const fleetGroup = new THREE.Group();
scene.add(fleetGroup);

let three = { camera: null, renderer: null };

const customLayer = {
  id: 'aisafe-fleet',
  type: 'custom',
  renderingMode: '3d',
  onAdd(m, gl) {
    three.camera = new THREE.Camera();
    three.renderer = new THREE.WebGLRenderer({ canvas: m.getCanvas(), context: gl, antialias: true });
    three.renderer.autoClear = false;
  },
  render(gl, matrix) {
    if (!originMerc) return;
    const rotX = new THREE.Matrix4().makeRotationAxis(new THREE.Vector3(1, 0, 0), Math.PI / 2);
    const l = new THREE.Matrix4()
      .makeTranslation(originMerc.x, originMerc.y, originMerc.z)
      .scale(new THREE.Vector3(mScale, -mScale, mScale))
      .multiply(rotX);
    three.camera.projectionMatrix = new THREE.Matrix4().fromArray(matrix).multiply(l);
    three.renderer.resetState();
    three.renderer.render(scene, three.camera);
    map.triggerRepaint();
  },
};

// ============================================================
// Per-flight visuals
// ============================================================
const visuals = new WeakMap();
const CONFLICT = 0xFF4D9D;
const RING_MIN_M = 4000;   // floor so the (often tiny, metre-scale) h_min ring stays visible
const FADE_SECONDS = 0.6;  // map fade-in on activation / fade-out on arrival

// Flights are drawn as glowing circles: a soft additive halo + a brighter core
// disc, both camera-facing sprites tinted by the flight colour. The radial-gradient
// textures are built once and shared; per-flight tint comes from the material colour.
const GLOW_M = 30000;   // halo diameter in metres
const CORE_M = 11000;   // core-disc diameter in metres

function radialTexture(stops) {
  const size = 128;
  const c = document.createElement('canvas');
  c.width = c.height = size;
  const ctx = c.getContext('2d');
  const g = ctx.createRadialGradient(size / 2, size / 2, 0, size / 2, size / 2, size / 2);
  stops.forEach(([at, col]) => g.addColorStop(at, col));
  ctx.fillStyle = g;
  ctx.fillRect(0, 0, size, size);
  const tex = new THREE.CanvasTexture(c);
  tex.colorSpace = THREE.SRGBColorSpace;
  return tex;
}
// soft halo: bright centre falling smoothly to nothing
const GLOW_TEX = radialTexture([
  [0.0, 'rgba(255,255,255,0.9)'],
  [0.25, 'rgba(255,255,255,0.45)'],
  [0.55, 'rgba(255,255,255,0.16)'],
  [1.0, 'rgba(255,255,255,0)'],
]);
// core: a near-solid disc with a thin feathered edge
const CORE_TEX = radialTexture([
  [0.0, 'rgba(255,255,255,1)'],
  [0.62, 'rgba(255,255,255,1)'],
  [0.82, 'rgba(255,255,255,0.55)'],
  [1.0, 'rgba(255,255,255,0)'],
]);

function glowSprite(tex, color, diameter) {
  const spr = new THREE.Sprite(new THREE.SpriteMaterial({
    map: tex, color, transparent: true, depthTest: false, blending: THREE.AdditiveBlending,
  }));
  spr.scale.set(diameter, diameter, 1);
  return spr;
}

function ringGeometry(radiusM) {
  const seg = 64, pts = [];
  for (let i = 0; i <= seg; i++) {
    const a = (i / seg) * Math.PI * 2;
    pts.push(new THREE.Vector3(Math.cos(a) * radiusM, 0, Math.sin(a) * radiusM));
  }
  return new THREE.BufferGeometry().setFromPoints(pts);
}

function makeLabel(text, colorHex) {
  const pad = 8, fs = 34;
  const c = document.createElement('canvas');
  const ctx = c.getContext('2d');
  ctx.font = `600 ${fs}px 'JetBrains Mono', monospace`;
  const w = ctx.measureText(text).width;
  c.width = w + pad * 2; c.height = fs + pad * 2;
  ctx.font = `600 ${fs}px 'JetBrains Mono', monospace`;
  ctx.fillStyle = 'rgba(7,11,20,0.82)';
  roundRect(ctx, 0, 0, c.width, c.height, 8); ctx.fill();
  ctx.strokeStyle = colorHex; ctx.lineWidth = 2;
  roundRect(ctx, 1, 1, c.width - 2, c.height - 2, 8); ctx.stroke();
  ctx.fillStyle = '#E5ECF7'; ctx.textBaseline = 'middle';
  ctx.fillText(text, pad, c.height / 2 + 1);
  const tex = new THREE.CanvasTexture(c);
  tex.colorSpace = THREE.SRGBColorSpace;
  const spr = new THREE.Sprite(new THREE.SpriteMaterial({ map: tex, depthTest: false, transparent: true }));
  const targetH = 9000;
  spr.scale.set((c.width / c.height) * targetH, targetH, 1);
  spr.renderOrder = 999;
  return spr;
}
function roundRect(ctx, x, y, w, h, r) {
  ctx.beginPath();
  ctx.moveTo(x + r, y);
  ctx.arcTo(x + w, y, x + w, y + h, r);
  ctx.arcTo(x + w, y + h, x, y + h, r);
  ctx.arcTo(x, y + h, x, y, r);
  ctx.arcTo(x, y, x + w, y, r);
  ctx.closePath();
}

// altitude (metres) → flight level (hundreds of feet)
function flLevel(altM) { return String(Math.round(altM / 30.48)).padStart(3, '0'); }
function flLabel(f) { return `${f.label} · FL${flLevel(f.alt)}`; }
function ringRadius() { return Math.max(sim.hMin || 0, RING_MIN_M); }

function clearFleet() {
  for (const child of [...fleetGroup.children]) {
    fleetGroup.remove(child);
    child.traverse?.(o => {
      o.geometry?.dispose?.();
      if (o.material) {
        const m = o.material.map;
        if (m && m !== GLOW_TEX && m !== CORE_TEX) m.dispose(); // keep the shared glow textures
        o.material.dispose();
      }
    });
  }
}

function rebuildFleet() {
  clearFleet();
  if (!sim.flights.length) return;
  for (const f of sim.flights) {
    const col = new THREE.Color(f.color);
    const group = new THREE.Group();

    // glowing circle: soft halo behind a brighter core disc
    const glow = glowSprite(GLOW_TEX, col.clone(), GLOW_M);
    const body = glowSprite(CORE_TEX, col.clone(), CORE_M);
    group.add(glow);
    group.add(body);

    const ring = new THREE.LineLoop(ringGeometry(ringRadius()), new THREE.LineBasicMaterial({
      color: col, transparent: true, opacity: 0.5,
    }));
    group.add(ring);

    const drop = new THREE.Line(
      new THREE.BufferGeometry().setFromPoints([new THREE.Vector3(), new THREE.Vector3()]),
      new THREE.LineBasicMaterial({ color: col, transparent: true, opacity: 0.28 })
    );
    group.add(drop);

    // recent path: a pool of soft glow sprites (same look as the marker) sampled
    // along the last few steps, repositioned each frame and faded toward the tail.
    const trailDots = [];
    for (let d = 0; d < TRAIL_DOTS; d++) {
      const dot = glowSprite(GLOW_TEX, col.clone(), TRAIL_DOT_HEAD);
      dot.visible = false;
      fleetGroup.add(dot);
      trailDots.push(dot);
    }

    const label = makeLabel(flLabel(f), f.color);
    group.add(label);

    fleetGroup.add(group);
    visuals.set(f, { group, glow, body, ring, drop, trailDots, label, baseColor: col, fade: 0, heading: null });
  }
  applyToggles();
  updateFleetTransforms(0);
}

// ============================================================
// Sector outline (from the run's area bounds, inverse-projected)
// ============================================================
function sectorGeoJSON() {
  const p = sim.params;
  if (!p) return { type: 'FeatureCollection', features: [] };
  const corners = [
    [p.area_x_min, p.area_y_min], [p.area_x_max, p.area_y_min],
    [p.area_x_max, p.area_y_max], [p.area_x_min, p.area_y_max], [p.area_x_min, p.area_y_min],
  ].map(([x, y]) => {
    const ll = localToLatLon(x, y, p.ref_lat, p.ref_lon);
    return [ll.lon, ll.lat];
  });
  return { type: 'FeatureCollection', features: [{ type: 'Feature', geometry: { type: 'LineString', coordinates: corners } }] };
}

function areaCorners() {
  const p = sim.params;
  const sw = localToLatLon(p.area_x_min, p.area_y_min, p.ref_lat, p.ref_lon);
  const ne = localToLatLon(p.area_x_max, p.area_y_max, p.ref_lat, p.ref_lon);
  return { sw, ne };
}

// Resample a polyline into `count` points spaced evenly by arc length, so the
// trail dots stay continuous regardless of how far apart the per-step points are.
function resamplePolyline(pts, count) {
  const out = [];
  const seg = [];
  let total = 0;
  for (let i = 1; i < pts.length; i++) { const d = pts[i].distanceTo(pts[i - 1]); seg.push(d); total += d; }
  if (total < 1e-6) { for (let m = 0; m < count; m++) out.push(pts[pts.length - 1].clone()); return out; }
  for (let m = 0; m < count; m++) {
    const target = total * (m / (count - 1));
    let acc = 0;
    let i = 0;
    while (i < seg.length - 1 && acc + seg[i] < target) { acc += seg[i]; i++; }
    const t = seg[i] > 1e-9 ? (target - acc) / seg[i] : 0;
    out.push(pts[i].clone().lerp(pts[i + 1], t));
  }
  return out;
}

// ============================================================
// Per-frame transforms (interpolate prev → current)
// ============================================================
const lerp = (a, b, t) => a + (b - a) * t;

function updateFleetTransforms(t) {
  const conflictIdx = new Set();
  for (const k in sim.active) { conflictIdx.add(sim.active[k].i); conflictIdx.add(sim.active[k].j); }
  for (const idx of (sim.altActive || [])) conflictIdx.add(idx);   // altitude-bound busts

  for (const f of sim.flights) {
    const v = visuals.get(f); if (!v) continue;
    const ix = lerp(f.prev.x, f.x, t);
    const iy = lerp(f.prev.y, f.y, t);
    const ia = lerp(f.prev.alt, f.alt, t);
    const pos = toModel(ix, iy, ia);
    const conflict = conflictIdx.has(f.index);

    // fade in on activation (entry_step), fade out once arrived/inactive — map only
    const target = (f.active && !f.arrived) ? 1 : 0;
    const dFade = (ui._dt || 0.016) / FADE_SECONDS;
    v.fade = target > v.fade ? Math.min(target, v.fade + dFade) : Math.max(target, v.fade - dFade);
    const shown = v.fade > 0.01;

    v.group.position.copy(pos);
    v.group.visible = shown;

    const c = conflict ? new THREE.Color(CONFLICT) : v.baseColor;
    v.body.material.color.copy(c);
    v.body.material.opacity = v.fade;
    // halo pulses a touch brighter while in conflict
    v.glow.material.color.copy(c);
    v.glow.material.opacity = (conflict ? 1 : 0.85) * v.fade;
    v.ring.material.color.copy(c);
    v.ring.material.opacity = (conflict ? 0.95 : 0.5) * v.fade;
    v.ring.visible = ui.show.zones;

    v.drop.visible = ui.show.drops;
    v.drop.material.opacity = 0.28 * v.fade;
    if (ui.show.drops) {
      const arr = v.drop.geometry.attributes.position.array;
      arr[0] = 0; arr[1] = 0; arr[2] = 0;
      arr[3] = 0; arr[4] = -pos.y; arr[5] = 0;
      v.drop.geometry.attributes.position.needsUpdate = true;
    }

    v.label.visible = ui.show.labels;
    v.label.material.opacity = v.fade;
    v.label.position.set(0, 13000, 0);

    // recent path: last ui.trailSteps steps + current pos, resampled into evenly
    // spaced glow dots that fade out toward the tail (head = nearest the flight)
    const hist = f.history.slice(-Math.max(1, ui.trailSteps - 1));
    const pathPts = hist.map(h => toModel(h.x, h.y, h.alt));
    pathPts.push(pos);
    const showTrail = ui.show.trails && shown && pathPts.length >= 2;
    if (showTrail) {
      const samples = resamplePolyline(pathPts, TRAIL_DOTS);   // index 0 = tail, last = head
      for (let d = 0; d < TRAIL_DOTS; d++) {
        const dot = v.trailDots[d];
        const frac = d / (TRAIL_DOTS - 1);   // 0 tail → 1 head
        const k = frac * frac;               // pronounced fade-out toward the tail
        dot.visible = true;
        dot.position.copy(samples[d]);
        dot.material.color.copy(c);
        dot.material.opacity = v.fade * (0.1 + 0.9 * k);
        const size = TRAIL_DOT_TAIL + (TRAIL_DOT_HEAD - TRAIL_DOT_TAIL) * k;
        dot.scale.set(size, size, 1);
      }
    } else {
      for (const dot of v.trailDots) dot.visible = false;
    }
  }

  if (linkGroup) { fleetGroup.remove(linkGroup); linkGroup.traverse(o => o.geometry?.dispose?.()); linkGroup = null; }
  if (ui.show.links) {
    linkGroup = new THREE.Group();
    for (const k in sim.active) {
      const { i, j } = sim.active[k];
      const a = sim.flights[i], b = sim.flights[j];
      const pa = toModel(lerp(a.prev.x, a.x, t), lerp(a.prev.y, a.y, t), lerp(a.prev.alt, a.alt, t));
      const pb = toModel(lerp(b.prev.x, b.x, t), lerp(b.prev.y, b.y, t), lerp(b.prev.alt, b.alt, t));
      const ln = new THREE.Line(new THREE.BufferGeometry().setFromPoints([pa, pb]),
        new THREE.LineBasicMaterial({ color: CONFLICT, transparent: true, opacity: 0.95 }));
      linkGroup.add(ln);
    }
    fleetGroup.add(linkGroup);
  }
}
let linkGroup = null;

function applyToggles() {
  if (map.getLayer('buildings-3d')) map.setLayoutProperty('buildings-3d', 'visibility', ui.show.buildings ? 'visible' : 'none');
  if (map.getLayer('sector-line')) map.setLayoutProperty('sector-line', 'visibility', ui.show.sector ? 'visible' : 'none');
  if (map.getLayer('sector-fill')) map.setLayoutProperty('sector-fill', 'visibility', ui.show.sector ? 'visible' : 'none');
  setTerrain(ui.show.terrain);
}

// ============================================================
// Animation loop
// ============================================================
function tick(dt) {
  ui._dt = dt;
  const stepDur = 1 / ui.rate;
  if (ui.playing && !sim.endReason) {
    ui.timeAccum += dt;
    if (ui.timeAccum >= stepDur) {
      ui.timeAccum -= stepDur;
      step();
      renderFlightList(); renderLog(); updateHUD();
    }
  }
  const t = Math.max(0, Math.min(1, ui.timeAccum / stepDur));
  updateFleetTransforms(t);
}
let last = performance.now();
function loop(now) {
  const dt = Math.min(0.1, (now - last) / 1000); last = now;
  tick(dt);
  requestAnimationFrame(loop);
}
requestAnimationFrame(loop);

// ============================================================
// Basemap / terrain / buildings
// ============================================================
function addGeoLayers() {
  if (!map.getSource('esri-sat')) {
    map.addSource('esri-sat', {
      type: 'raster', tileSize: 256,
      tiles: ['https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}'],
      attribution: 'Imagery © Esri, Maxar, Earthstar Geographics',
    });
  }
  if (!map.getSource('carto-dark')) {
    map.addSource('carto-dark', {
      type: 'raster', tileSize: 256,
      tiles: ['https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/{z}/{y}/{x}'],
      attribution: 'Tiles © Esri — Esri, HERE, Garmin, © OpenStreetMap contributors',
    });
  }
  if (!map.getSource('terrain-dem')) {
    map.addSource('terrain-dem', {
      type: 'raster-dem', tileSize: 256, encoding: 'terrarium', maxzoom: 13,
      tiles: ['https://s3.amazonaws.com/elevation-tiles-prod/terrarium/{z}/{x}/{y}.png'],
    });
  }

  const layers = map.getStyle().layers || [];
  const beforeId = layers.find(l => l.type === 'symbol')?.id;

  if (!map.getLayer('esri-sat-layer'))
    map.addLayer({ id: 'esri-sat-layer', type: 'raster', source: 'esri-sat', layout: { visibility: ui.basemap === 'sat' ? 'visible' : 'none' } }, beforeId);
  if (!map.getLayer('carto-dark-layer'))
    map.addLayer({ id: 'carto-dark-layer', type: 'raster', source: 'carto-dark', layout: { visibility: ui.basemap === 'dark' ? 'visible' : 'none' } }, beforeId);

  if (!map.getLayer('buildings-3d')) {
    map.addLayer({
      id: 'buildings-3d', type: 'fill-extrusion', source: 'openmaptiles', 'source-layer': 'building',
      minzoom: 12,
      paint: {
        'fill-extrusion-color': ['case', ['has', 'colour'], ['get', 'colour'], '#8493ad'],
        'fill-extrusion-height': ['coalesce', ['get', 'render_height'], ['get', 'height'], 8],
        'fill-extrusion-base': ['coalesce', ['get', 'render_min_height'], ['get', 'min_height'], 0],
        'fill-extrusion-opacity': 0.9,
      },
      layout: { visibility: ui.show.buildings ? 'visible' : 'none' },
    });
  }

  if (!map.getSource('sector')) map.addSource('sector', { type: 'geojson', data: sectorGeoJSON() });
  if (!map.getLayer('sector-fill'))
    map.addLayer({ id: 'sector-fill', type: 'fill', source: 'sector', paint: { 'fill-color': '#00E5FF', 'fill-opacity': 0.05 } });
  if (!map.getLayer('sector-line'))
    map.addLayer({ id: 'sector-line', type: 'line', source: 'sector', paint: { 'line-color': '#00E5FF', 'line-width': 2, 'line-dasharray': [3, 2] } });

  if (!map.getLayer('aisafe-fleet')) map.addLayer(customLayer);
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

// ============================================================
// HUD / panels
// ============================================================
function notice(msg, warn = false) {
  const n = document.getElementById('notice');
  n.textContent = msg; n.classList.toggle('warn', warn); n.classList.add('show');
  clearTimeout(notice._t); notice._t = setTimeout(() => n.classList.remove('show'), 1800);
}

function updateHUD() {
  document.getElementById('kpi-step').textContent = `${sim.step} / ${sim.totalSteps}`;
  document.getElementById('kpi-viol').textContent = `${sim.violationCount} / ${sim.maxViolations}`;
  document.getElementById('kpi-viol-box').classList.toggle('alert', sim.violationCount > sim.maxViolations);
  const pill = document.getElementById('kpi-status');
  pill.className = 'status-pill';
  if (!sim.flights.length) { pill.classList.add('idle'); pill.textContent = 'No run'; }
  else if (sim.endReason === 'THRESHOLD_EXCEEDED') { pill.classList.add('ended'); pill.textContent = 'Threshold exceeded'; }
  else if (sim.endReason === 'NORMAL') { pill.classList.add('done'); pill.textContent = 'Completed'; }
  else if (ui.playing) { pill.classList.add('running'); pill.textContent = 'Running'; }
  else { pill.classList.add('paused'); pill.textContent = 'Paused'; }
  const btn = document.getElementById('btn-play');
  if (sim.endReason) btn.textContent = '↻ Restart';
  else if (ui.playing) btn.textContent = '⏸ Pause';
  else btn.textContent = '▶ Run Sim';
}

function setText(id, text) { const el = document.getElementById(id); if (el) el.textContent = text; }

function renderRunMeta() {
  if (!currentRun) return;
  setText('area-picker-code', currentRun.area || '—');
  setText('area-picker-name', currentRun.scenario || currentRun.id);
  setText('active-preset', currentRun.scenario || currentRun.id);
  setText('kpi-sector', `${currentRun.area} · ${currentRun.scenario}`);
  const { sw, ne } = areaCorners();
  const cosLat = Math.cos(((sw.lat + ne.lat) / 2) * Math.PI / 180);
  const wKm = Math.abs(ne.lon - sw.lon) * 111.32 * cosLat;
  const hKm = Math.abs(ne.lat - sw.lat) * 111.32;
  setText('kpi-span', `${wKm.toFixed(0)} × ${hKm.toFixed(0)} km`);
  const ap = document.getElementById('active-preset');
  if (ap) { ap.classList.remove('text-slate-400'); ap.classList.add('text-cyan2'); }

  // read-only scenario parameter readout
  const ro = document.getElementById('param-readout');
  if (ro) {
    const verdict = (currentRun.verdict || '—');
    const end = (currentRun.endReason || '—');
    ro.innerHTML = `
      <div class="status-row"><span class="lbl">Run</span><span class="val mono" style="color:#94A3B8">${currentRun.id}</span></div>
      <div class="status-row"><span class="lbl">Verdict</span><span class="val mono" style="color:${verdict === 'PASS' ? '#3EE6A8' : '#FF4D9D'}">${verdict}</span></div>
      <div class="status-row"><span class="lbl">End reason</span><span class="val mono">${end}</span></div>
      <div class="status-row"><span class="lbl">h_min</span><span class="val mono">${sim.hMin} m</span></div>
      <div class="status-row"><span class="lbl">v_min</span><span class="val mono">${sim.vMin} m</span></div>
      <div class="status-row"><span class="lbl">max viol</span><span class="val mono">${sim.maxViolations}</span></div>
      <div class="status-row"><span class="lbl">steps</span><span class="val mono">${sim.totalSteps} × ${sim.params.seconds_per_step}s</span></div>`;
  }
}

function renderFlightList() {
  const host = document.getElementById('flights');
  if (!host) return;
  host.innerHTML = '';
  const cnt = document.getElementById('fl-count');
  if (cnt) cnt.textContent = sim.flights.length ? String(sim.flights.length) : '0';
  if (!sim.flights.length) { host.innerHTML = '<div class="text-[11px] text-slate-500 text-center py-3 italic">Pick a run to load flights</div>'; return; }
  const conflictIdx = new Set();
  for (const k in sim.active) { conflictIdx.add(sim.active[k].i); conflictIdx.add(sim.active[k].j); }
  for (const idx of (sim.altActive || [])) conflictIdx.add(idx);   // altitude-bound busts
  for (const f of sim.flights) {
    const card = document.createElement('div');
    card.className = 'flight-card';
    const badge = conflictIdx.has(f.index) ? '<span class="badge alarm">conflict</span>'
      : f.outcome === 'TERMINATED' ? '<span class="badge viol">terminated</span>'
      : f.outcome === 'COMPLETED' ? '<span class="badge ok">complete</span>'
      : f.arrived ? '<span class="badge ok">arrived</span>'
      : !f.active ? '<span class="badge">holding</span>' : '';
    card.innerHTML = `
      <div class="head">
        <div class="id-line">
          <span class="swatch" style="background:${f.color};color:${f.color}"></span>
          <span class="mono" style="font-size:11px">${f.label}</span>
          <span class="badge">FL${flLevel(f.alt)}</span>
          ${badge}
        </div>
      </div>
      <div class="field-grid" style="grid-template-columns:auto 1fr 1fr 1fr">
        <span class="lbl">x·y km</span>
        <span class="val mono" style="font-size:10px">${(f.x / 1000).toFixed(1)}</span>
        <span class="val mono" style="font-size:10px">${(f.y / 1000).toFixed(1)}</span>
        <span class="val mono" style="font-size:10px">${(f.alt).toFixed(0)} m</span>
      </div>`;
    host.appendChild(card);
  }
}

function renderLog() {
  const body = document.getElementById('log-body');
  if (!body) return;
  if (!sim.log.length) { body.innerHTML = '<div class="text-slate-500 italic text-center py-3">No violations.</div>'; return; }
  body.innerHTML = sim.log.slice().reverse().map(e => {
    const alt = e.kind === 'ALT_CEILING' || e.kind === 'ALT_FLOOR';
    const head = alt
      ? `<b style="color:#FF4D9D;font-weight:500">${e.id1}</b> <span style="color:#FFB547">${e.kind === 'ALT_CEILING' ? 'ALT CEILING' : 'ALT FLOOR'}</span>`
      : `<b style="color:#FF4D9D;font-weight:500">${e.id1} ↔ ${e.id2}</b>`;
    const body = alt
      ? `Δalt=${e.v.toFixed(0)} m past bound`
      : `h=${e.h.toFixed(0)} m &nbsp; v=${e.v.toFixed(0)} m`;
    return `
    <div style="padding:4px 0;border-bottom:1px solid rgba(58,70,100,.2);color:#94A3B8">
      <span style="color:#64748B">step ${e.step}</span> &nbsp;
      ${head}<br>
      ${body}
    </div>`;
  }).join('');
}

// ============================================================
// Run picker
// ============================================================
function buildRunMenu() {
  const menu = document.getElementById('area-menu');
  if (!menu) return;
  if (!runList.length) {
    menu.innerHTML = '<div class="row" style="grid-template-columns:1fr"><span class="name" style="color:#64748B">No runs found. Run an area simulation first.</span></div>';
    return;
  }
  menu.innerHTML = runList.map(r => {
    const vc = r.verdict === 'PASS' ? '#3EE6A8' : '#FF4D9D';
    return `<div class="row" data-id="${r.id}" style="grid-template-columns:64px 1fr 64px">
      <span class="code">${r.area}</span>
      <span class="name">${r.scenario}
        <span style="color:#64748B;margin-left:6px">· ${r.flights} fl · ${r.violations} viol</span>
        <div style="color:#64748B;font-size:9px;margin-top:2px">${r.id}</div></span>
      <span class="span" style="color:${vc}">${r.verdict}</span>
    </div>`;
  }).join('');
  menu.querySelectorAll('.row[data-id]').forEach(row =>
    row.addEventListener('click', () => { selectRun(row.dataset.id); menu.classList.add('hidden'); }));
}

function frameArea() {
  const { sw, ne } = areaCorners();
  const centerLon = (sw.lon + ne.lon) / 2;
  const centerLat = (sw.lat + ne.lat) / 2;
  const widthDeg = Math.abs(ne.lon - sw.lon);
  let zoom = Math.log2(360 / Math.max(0.2, widthDeg)) - 0.35;
  zoom = Math.max(4.5, Math.min(8.5, zoom));
  map.easeTo({ center: [centerLon, centerLat], zoom, pitch: 58, bearing: -17, duration: 900 });
}

async function selectRun(id) {
  let bundle;
  try {
    const res = await fetch(`${RUNS_URL}/${encodeURIComponent(id)}`, { headers: { Accept: 'application/json' } });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    bundle = await res.json();
  } catch (e) {
    notice('Could not load run', true);
    return;
  }
  currentRun = bundle;
  loadScenario(bundle);
  setOrigin(sim.params.ref_lat, sim.params.ref_lon);
  ui.playing = false; ui.timeAccum = 0;
  if (map.getSource('sector')) map.getSource('sector').setData(sectorGeoJSON());
  rebuildFleet();
  renderRunMeta(); renderFlightList(); renderLog(); updateHUD();
  frameArea();
  notice(`Loaded ${bundle.scenario}`);
}

async function loadRunList() {
  try {
    const res = await fetch(RUNS_URL, { headers: { Accept: 'application/json' } });
    runList = res.ok ? await res.json() : [];
  } catch (e) {
    runList = [];
  }
  buildRunMenu();
  return runList;
}

// ============================================================
// Wiring
// ============================================================
function stepOnce() {
  if (!sim.flights.length) { notice('Pick a run first', true); return; }
  if (sim.endReason) { resetState(); rebuildFleet(); renderFlightList(); renderLog(); updateHUD(); return; }
  ui.playing = false; step(); ui.timeAccum = 0;
  renderFlightList(); renderLog(); updateHUD();
}

function bindSlider(id, fn) {
  const el = document.getElementById(id);
  if (!el) return;
  const v = document.getElementById(id + '-v');
  el.addEventListener('input', () => {
    const val = Number(el.value); fn(val);
    if (v) v.textContent = (id === 'rate' ? val.toFixed(1) : id === 'exag' ? val + '×' : String(val));
  });
}

function wire() {
  const btn = document.getElementById('area-picker-btn');
  const menu = document.getElementById('area-menu');
  if (btn && menu) {
    btn.addEventListener('click', e => { e.stopPropagation(); menu.classList.toggle('hidden'); });
    document.addEventListener('click', e => { if (!document.getElementById('area-picker-wrap').contains(e.target)) menu.classList.add('hidden'); });
  }

  document.getElementById('btn-play').addEventListener('click', () => {
    if (!sim.flights.length) { notice('Pick a run first', true); return; }
    if (sim.endReason) { resetState(); rebuildFleet(); renderFlightList(); renderLog(); ui.playing = true; }
    else ui.playing = !ui.playing;
    updateHUD();
  });
  document.getElementById('btn-step').addEventListener('click', stepOnce);
  document.getElementById('btn-reset').addEventListener('click', () => {
    if (!sim.flights.length) return;
    resetState(); rebuildFleet(); renderFlightList(); renderLog(); updateHUD();
    notice('Reset to step 0');
  });

  bindSlider('rate', v => { ui.rate = v; });
  bindSlider('exag', v => { ui.exag = v; });
  bindSlider('trail', v => { ui.trailSteps = v; });

  document.querySelectorAll('#basemap-seg .seg-btn').forEach(el => el.addEventListener('click', () => setBasemap(el.dataset.base)));

  document.querySelectorAll('.chip[data-toggle]').forEach(el => {
    el.addEventListener('click', () => {
      const k = el.dataset.toggle;
      if (k === 'log') {
        const log = document.getElementById('log-panel');
        const on = log.style.display === 'none';
        log.style.display = on ? 'flex' : 'none';
        el.classList.toggle('on', on);
        return;
      }
      ui.show[k] = !ui.show[k];
      el.classList.toggle('on', ui.show[k]);
      applyToggles();
    });
  });

  window.addEventListener('keydown', e => {
    if (e.target.tagName === 'INPUT') return;
    if (e.code === 'Space') { e.preventDefault(); document.getElementById('btn-play').click(); }
    else if (e.code === 'ArrowRight') stepOnce();
    else if (e.code === 'KeyR') document.getElementById('btn-reset').click();
  });
}

// ============================================================
// Boot
// ============================================================
wire();
updateHUD();

map.on('load', async () => {
  addGeoLayers();
  setBasemap(ui.basemap);
  const runs = await loadRunList();
  const deep = (window.__deeplinkRun || '').trim();
  const start = (deep && runs.some(r => r.id === deep)) ? deep : (runs[0] && runs[0].id);
  if (start) selectRun(start);
});