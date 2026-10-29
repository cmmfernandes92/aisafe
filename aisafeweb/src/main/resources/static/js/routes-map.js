(function () {
  const mapEl = document.getElementById('routes-map');
  if (!mapEl || typeof L === 'undefined') return;

  const areas    = window.AISAFE_AREAS    || [];
  const airports = window.AISAFE_AIRPORTS || [];
  const routes   = window.AISAFE_ROUTES   || [];

  const COMPANY_COLORS = {
    TP: '#7b61ff',
    FR: '#fb7185',
    LH: '#3ee6a8',
    VY: '#ffb547',
  };
  function companyColor(iata) {
    return COMPANY_COLORS[iata] || '#ffb547';
  }

  // ── MAP INIT ───────────────────────────────────────────────────────────────

  const map = L.map('routes-map', {
    zoomControl: true,
    attributionControl: true,
    minZoom: 3,
    maxZoom: 12,
  }).setView([48.0, 12.0], 5);

  L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/{z}/{y}/{x}', {
    attribution: 'Tiles &copy; Esri &mdash; Esri, HERE, Garmin, &copy; OpenStreetMap contributors',
    maxZoom: 16,
  }).addTo(map);

  L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Reference/MapServer/tile/{z}/{y}/{x}', {
    maxZoom: 16,
    pane: 'overlayPane',
  }).addTo(map);

  map.createPane('areaGlow');    map.getPane('areaGlow').style.zIndex    = 350;
  map.createPane('areaRect');    map.getPane('areaRect').style.zIndex    = 400;
  map.createPane('routeLine');   map.getPane('routeLine').style.zIndex   = 450;
  map.createPane('airportPin');  map.getPane('airportPin').style.zIndex  = 500;
  map.createPane('areaDot');     map.getPane('areaDot').style.zIndex     = 550;
  map.createPane('areaLabel');   map.getPane('areaLabel').style.zIndex   = 600;

  // ── HELPERS ────────────────────────────────────────────────────────────────

  function escapeHtml(v) {
    return String(v || '')
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#039;');
  }

  function offsetLine(lat1, lon1, lat2, lon2, d) {
    const dx = lat2 - lat1, dy = lon2 - lon1;
    const len = Math.sqrt(dx * dx + dy * dy) || 1;
    return [
      [lat1 + (-dy / len) * d, lon1 + (dx / len) * d],
      [lat2 + (-dy / len) * d, lon2 + (dx / len) * d],
    ];
  }

  function bearing(lat1, lon1, lat2, lon2) {
    return Math.atan2(lon2 - lon1, lat2 - lat1) * 180 / Math.PI;
  }

  // ── AREAS ─────────────────────────────────────────────────────────────────

  const rectLayers = [], glowLayers = [], labelMarkers = [], areaDotMarkers = [];
  const allBounds  = [];

  areas.forEach((area) => {
    const bounds = [[area.swLat, area.swLon], [area.neLat, area.neLon]];
    allBounds.push([area.swLat, area.swLon], [area.neLat, area.neLon]);

    rectLayers.push(L.rectangle(bounds, {
      pane: 'areaRect', color: '#00E5FF', weight: 1.2, opacity: 0.85,
      fillColor: '#7B61FF', fillOpacity: 0.08, dashArray: '4 4',
    }));
    glowLayers.push(L.rectangle(bounds, {
      pane: 'areaGlow', color: '#00E5FF', weight: 8, opacity: 0.05, fill: false,
    }));
    labelMarkers.push(L.marker([area.neLat - 0.3, area.swLon + 0.3], {
      icon: L.divIcon({
        className: '',
        html: `<div class="area-label">${escapeHtml(area.code)}<div class="sub">${escapeHtml(area.name)}</div></div>`,
        iconSize: [92, 34], iconAnchor: [0, 0],
      }),
      interactive: false, pane: 'areaLabel',
    }));
    areaDotMarkers.push(L.marker([area.centerLat, area.centerLon], {
      pane: 'areaDot',
      icon: L.divIcon({
        className: '',
        html: `<div class="area-dot"><div class="area-dot-core"></div><div class="area-dot-ring"></div><div class="area-dot-code">${escapeHtml(area.code)}</div></div>`,
        iconSize: [60, 60], iconAnchor: [30, 30],
      }),
    }));
  });

  // ── AIRPORTS ───────────────────────────────────────────────────────────────

  const airportMarkers = [];
  const airportByIata  = {};

  airports.forEach((airport) => {
    airportByIata[airport.iataCode] = airport;
    const marker = L.marker([airport.latitude, airport.longitude], {
      pane: 'airportPin',
      icon: L.divIcon({
        className: '',
        html: '<div class="airport-pin"></div>',
        iconSize: [8, 8], iconAnchor: [4, 4],
      }),
    }).bindPopup(
      `<div style="min-width:160px;">
        <div style="display:flex;gap:8px;align-items:center;margin-bottom:7px;padding-bottom:7px;border-bottom:1px solid var(--line);">
          <span style="font:700 1rem 'JetBrains Mono',monospace;color:var(--cyan);">${escapeHtml(airport.iataCode)}</span>
          <span style="font:600 0.72rem 'JetBrains Mono',monospace;color:var(--muted-2);background:rgba(255,255,255,0.05);padding:1px 6px;border-radius:4px;">${escapeHtml(airport.icaoCode)}</span>
        </div>
        <div style="font-size:0.84rem;margin-bottom:3px;">${escapeHtml(airport.name)}</div>
        <div style="font-size:0.72rem;color:var(--muted-2);">Area: ${escapeHtml(airport.areaCode)}</div>
      </div>`,
      { maxWidth: 220 }
    );
    airportMarkers.push(marker);
  });

  // ── ROUTES ────────────────────────────────────────────────────────────────

  const pairCount = {}, pairIdx = {};
  routes.forEach((r) => {
    const k = [r.originIata, r.destinationIata].sort().join('|');
    pairCount[k] = (pairCount[k] || 0) + 1;
  });

  const routeLineLayers = {};

  routes.forEach((r) => {
    const orig = airportByIata[r.originIata];
    const dest = airportByIata[r.destinationIata];
    if (!orig || !dest) return;

    const k = [r.originIata, r.destinationIata].sort().join('|');
    const total = pairCount[k];
    const idx   = pairIdx[k] || 0;
    pairIdx[k]  = idx + 1;

    const offsetVal    = total > 1 ? (idx - (total - 1) / 2) * 0.04 : 0;
    const [start, end] = offsetLine(orig.latitude, orig.longitude, dest.latitude, dest.longitude, offsetVal);
    const color        = companyColor(r.companyIata);
    const deg          = bearing(start[0], start[1], end[0], end[1]);

    const line = L.polyline([start, end], {
      pane: 'routeLine', color, weight: 2.2, opacity: 0.9,
    });

    const arrowMarker = L.marker([(start[0] + end[0]) / 2, (start[1] + end[1]) / 2], {
      pane: 'routeLine',
      icon: L.divIcon({
        className: '',
        html: `<div style="width:0;height:0;border-left:4px solid transparent;border-right:4px solid transparent;border-bottom:9px solid ${color};transform:rotate(${deg}deg);opacity:0.85;"></div>`,
        iconSize: [8, 9], iconAnchor: [4, 4],
      }),
      interactive: false,
    });

    line.bindPopup(
      `<div style="min-width:170px;">
        <div style="display:flex;align-items:center;gap:8px;margin-bottom:8px;padding-bottom:8px;border-bottom:1px solid var(--line);">
          <span style="font:700 0.9rem 'JetBrains Mono',monospace;color:${color};background:rgba(255,255,255,0.05);padding:2px 8px;border-radius:5px;">${escapeHtml(r.routeName)}</span>
          <span style="font-size:0.72rem;color:var(--muted-2);">${escapeHtml(r.companyName)}</span>
        </div>
        <div style="font-size:0.84rem;">${escapeHtml(r.originIata)} <span style="color:var(--muted-2);margin:0 6px;">&rarr;</span> ${escapeHtml(r.destinationIata)}</div>
        <div style="font-size:0.72rem;color:var(--muted-2);margin-top:3px;">${escapeHtml(orig.name)} &rarr; ${escapeHtml(dest.name)}</div>
        <div style="margin-top:7px;padding-top:7px;border-top:1px solid var(--line);font-size:0.72rem;color:var(--green);">&#9679; ${r.active ? 'Active' : 'Inactive'}</div>
      </div>`,
      { maxWidth: 240 }
    );

    routeLineLayers[r.routeName] = { line, arrowMarker };
  });

  // ── VISIBILITY STATE ───────────────────────────────────────────────────────
  // All routes visible by default
  const visible = new Set(routes.map((r) => r.routeName));

  function applyVisibility(routeName) {
    const entry = routeLineLayers[routeName];
    if (!entry) return;
    const show = visible.has(routeName) && mapDetailActive;
    toggle(entry.line,        show);
    toggle(entry.arrowMarker, show);
  }

  function applyAllVisibility() {
    routes.forEach((r) => applyVisibility(r.routeName));
  }

  // ── ZOOM TIERS ─────────────────────────────────────────────────────────────

  let mapDetailActive = false;

  function applyZoomLayer() {
    const z        = map.getZoom();
    const overview = z <= 4;
    const detail   = z >= 6;
    mapDetailActive = detail;

    rectLayers.forEach((l)     => toggle(l, !overview));
    glowLayers.forEach((l)     => toggle(l, !overview));
    labelMarkers.forEach((l)   => toggle(l, !overview));
    airportMarkers.forEach((l) => toggle(l, detail));
    areaDotMarkers.forEach((l) => toggle(l, overview));
    applyAllVisibility();

    setTier('rzt-overview', overview);
    setTier('rzt-areas',    z === 5);
    setTier('rzt-airports', detail);
  }

  function toggle(layer, on) {
    if (on  && !map.hasLayer(layer)) layer.addTo(map);
    if (!on &&  map.hasLayer(layer)) map.removeLayer(layer);
  }

  function setTier(id, active) {
    const el = document.getElementById(id);
    if (el) el.classList.toggle('active', active);
  }

  mapEl.closest('.map-wrap') && mapEl.closest('.map-wrap').querySelectorAll('[data-zoom]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const z = parseInt(btn.dataset.zoom, 10);
      if (!Number.isNaN(z)) map.setZoom(z);
    });
  });

  map.on('zoomend', applyZoomLayer);

  if (allBounds.length) map.fitBounds(allBounds, { padding: [24, 24] });
  applyZoomLayer();

  // ── ROUTE PANEL ────────────────────────────────────────────────────────────

  function renderFilterBar() {
    const bar = document.getElementById('route-filter-bar');
    if (!bar) return;
    bar.innerHTML = '';

    // Show All / Hide All
    const allBtn = document.createElement('button');
    allBtn.className = 'route-filter-btn active';
    allBtn.textContent = 'All';
    allBtn.addEventListener('click', () => {
      routes.forEach((r) => visible.add(r.routeName));
      applyAllVisibility();
      renderRouteList();
    });
    bar.appendChild(allBtn);

    const noneBtn = document.createElement('button');
    noneBtn.className = 'route-filter-btn';
    noneBtn.textContent = 'None';
    noneBtn.addEventListener('click', () => {
      visible.clear();
      applyAllVisibility();
      renderRouteList();
    });
    bar.appendChild(noneBtn);

    // Per-company toggle buttons
    const companies = {};
    routes.forEach((r) => { companies[r.companyIata] = r.companyName; });

    Object.entries(companies).forEach(([iata, name]) => {
      const color = companyColor(iata);
      const btn = document.createElement('button');
      btn.className = 'route-filter-btn';
      btn.style.borderColor = color;
      btn.textContent = iata;
      btn.title = name;
      btn.addEventListener('click', () => {
        const companyRoutes = routes.filter((r) => r.companyIata === iata);
        const allShown = companyRoutes.every((r) => visible.has(r.routeName));
        companyRoutes.forEach((r) => {
          if (allShown) visible.delete(r.routeName);
          else          visible.add(r.routeName);
        });
        applyAllVisibility();
        renderRouteList();
      });
      bar.appendChild(btn);
    });
  }

  function renderRouteList() {
    const listEl  = document.getElementById('route-list');
    const countEl = document.getElementById('route-count');
    if (!listEl) return;
    listEl.innerHTML = '';

    const shown = routes.filter((r) => visible.has(r.routeName)).length;
    if (countEl) countEl.textContent = `(${shown}/${routes.length})`;

    if (routes.length === 0) {
      listEl.innerHTML = '<div class="map-empty">No routes registered.</div>';
      return;
    }

    routes.forEach((r) => {
      const color   = companyColor(r.companyIata);
      const isOn    = visible.has(r.routeName);

      const row = document.createElement('div');
      row.className = 'map-route-row';
      row.style.cssText = `border-left-color:${isOn ? color : 'transparent'};opacity:${isOn ? '1' : '0.38'};`;

      // Toggle button (the whole left part)
      const toggleBtn = document.createElement('button');
      toggleBtn.className = 'route-row-toggle';
      toggleBtn.title = isOn ? 'Hide route' : 'Show route';
      toggleBtn.innerHTML = `
        <span class="rvis-dot" style="background:${isOn ? color : 'transparent'};border-color:${color};"></span>
        <span class="route-row-info">
          <span class="rn" style="color:${isOn ? color : 'var(--muted-2)'};">${escapeHtml(r.routeName)}</span>
          <span class="rp">${escapeHtml(r.originIata)} &rarr; ${escapeHtml(r.destinationIata)}</span>
        </span>
        <em class="rc">${escapeHtml(r.companyIata)}</em>
      `;
      toggleBtn.addEventListener('click', () => {
        if (visible.has(r.routeName)) visible.delete(r.routeName);
        else                          visible.add(r.routeName);
        applyVisibility(r.routeName);
        renderRouteList();
      });

      // Focus button (arrow icon)
      const focusBtn = document.createElement('button');
      focusBtn.className = 'route-row-focus';
      focusBtn.title = 'Focus route';
      focusBtn.innerHTML = '&#8599;';
      focusBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        const entry = routeLineLayers[r.routeName];
        if (!entry) return;
        if (!visible.has(r.routeName)) {
          visible.add(r.routeName);
          applyVisibility(r.routeName);
          renderRouteList();
        }
        map.setZoom(7);
        map.fitBounds(entry.line.getBounds().pad(0.4), { animate: true });
        entry.line.openPopup();
      });

      row.appendChild(toggleBtn);
      row.appendChild(focusBtn);
      listEl.appendChild(row);
    });
  }

  renderFilterBar();
  renderRouteList();
})();
