(function () {
  const mapEl = document.getElementById('map');
  if (!mapEl || typeof L === 'undefined') return;

  const areas = window.AISAFE_AREAS || [];
  const airports = window.AISAFE_AIRPORTS || [];

  const map = L.map('map', {
    zoomControl: true,
    attributionControl: true,
    minZoom: 3,
    maxZoom: 12
  }).setView([48.0, 12.0], 5);

  L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/{z}/{y}/{x}', {
    attribution: 'Tiles &copy; Esri &mdash; Esri, HERE, Garmin, &copy; OpenStreetMap contributors',
    maxZoom: 16
  }).addTo(map);

  L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Reference/MapServer/tile/{z}/{y}/{x}', {
    maxZoom: 16,
    pane: 'overlayPane'
  }).addTo(map);

  map.createPane('areaGlow'); map.getPane('areaGlow').style.zIndex = 350;
  map.createPane('areaRect'); map.getPane('areaRect').style.zIndex = 400;
  map.createPane('airportPin'); map.getPane('airportPin').style.zIndex = 500;
  map.createPane('areaDot'); map.getPane('areaDot').style.zIndex = 550;
  map.createPane('areaLabel'); map.getPane('areaLabel').style.zIndex = 600;

  const rectLayers = [];
  const glowLayers = [];
  const labelMarkers = [];
  const airportMarkers = [];
  const areaDotMarkers = [];
  const allBounds = [];

  areas.forEach((area) => {
    const bounds = [[area.swLat, area.swLon], [area.neLat, area.neLon]];
    allBounds.push([area.swLat, area.swLon], [area.neLat, area.neLon]);

    rectLayers.push(L.rectangle(bounds, {
      pane: 'areaRect',
      color: '#00E5FF',
      weight: 1.2,
      opacity: 0.85,
      fillColor: '#7B61FF',
      fillOpacity: 0.08,
      dashArray: '4 4'
    }));

    glowLayers.push(L.rectangle(bounds, {
      pane: 'areaGlow',
      color: '#00E5FF',
      weight: 8,
      opacity: 0.05,
      fill: false
    }));

    const label = L.divIcon({
      className: '',
      html: `<div class="area-label">${escapeHtml(area.code)}<div class="sub">${escapeHtml(area.name)}</div></div>`,
      iconSize: [92, 34],
      iconAnchor: [0, 0]
    });
    labelMarkers.push(L.marker([area.neLat - 0.3, area.swLon + 0.3], {
      icon: label,
      interactive: false,
      pane: 'areaLabel'
    }));

    const dot = L.divIcon({
      className: '',
      html: `<div class="area-dot"><div class="area-dot-core"></div><div class="area-dot-ring"></div><div class="area-dot-code">${escapeHtml(area.code)}</div></div>`,
      iconSize: [60, 60],
      iconAnchor: [30, 30]
    });
    areaDotMarkers.push(L.marker([area.centerLat, area.centerLon], { icon: dot, pane: 'areaDot' }));
  });

  airports.forEach((airport) => {
    const pin = L.divIcon({
      className: '',
      html: '<div class="airport-pin"></div>',
      iconSize: [8, 8],
      iconAnchor: [4, 4]
    });
    const marker = L.marker([airport.latitude, airport.longitude], {
      icon: pin,
      pane: 'airportPin'
    }).bindTooltip(escapeHtml(airport.icaoCode), {
      direction: 'top',
      className: 'area-label',
      offset: [0, -4]
    });
    airportMarkers.push(marker);
  });

  function applyZoomLayer() {
    const z = map.getZoom();
    const overview = z <= 4;
    const medium = z === 5;
    const detail = z >= 6;

    rectLayers.forEach((l) => toggle(l, !overview));
    glowLayers.forEach((l) => toggle(l, !overview));
    labelMarkers.forEach((l) => toggle(l, !overview));
    airportMarkers.forEach((l) => toggle(l, detail));
    areaDotMarkers.forEach((l) => toggle(l, overview));

    setTier('zoom-tier-overview', overview);
    setTier('zoom-tier-areas', medium);
    setTier('zoom-tier-airports', detail);
  }

  function toggle(layer, on) {
    if (on && !map.hasLayer(layer)) layer.addTo(map);
    if (!on && map.hasLayer(layer)) map.removeLayer(layer);
  }

  function setTier(id, active) {
    const el = document.getElementById(id);
    if (el) el.classList.toggle('active', active);
  }

  document.querySelectorAll('[id^="zoom-tier-"]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const z = parseInt(btn.dataset.zoom, 10);
      if (!Number.isNaN(z)) map.setZoom(z);
    });
  });

  document.querySelectorAll('.map-area-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      const index = parseInt(btn.dataset.areaIndex, 10);
      const area = areas[index];
      if (!area) return;
      map.fitBounds([[area.swLat, area.swLon], [area.neLat, area.neLon]], { padding: [30, 30] });
    });
  });

  map.on('zoomend', applyZoomLayer);

  if (allBounds.length) {
    map.fitBounds(allBounds, { padding: [24, 24] });
  }
  applyZoomLayer();

  function escapeHtml(value) {
    return String(value || '')
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#039;');
  }
})();
