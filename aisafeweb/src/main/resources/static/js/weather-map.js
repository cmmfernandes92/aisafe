(function () {
  const mapEl = document.getElementById('weather-map');
  if (!mapEl || typeof L === 'undefined') return;

  const areas = window.AISAFE_WEATHER_AREAS || [];
  const map = L.map('weather-map', {
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
  map.createPane('areaDot'); map.getPane('areaDot').style.zIndex = 550;
  map.createPane('areaLabel'); map.getPane('areaLabel').style.zIndex = 600;
  map.createPane('areaWeather'); map.getPane('areaWeather').style.zIndex = 620;

  // Live weather icons (animated Meteocons SVGs) served from jsDelivr.
  const WX_ICON_BASE = 'https://cdn.jsdelivr.net/npm/@meteocons/svg@0.1.0/fill/';
  const weatherMarkers = {};

  const rectLayers = [];
  const glowLayers = [];
  const labelMarkers = [];
  const areaDotMarkers = [];
  const allBounds = [];

  areas.forEach((area) => {
    const covered = !!area.hasWeatherData;
    const stroke = covered ? '#00E5FF' : '#ff4d9d';
    const fill = covered ? '#7B61FF' : '#ff4d9d';
    const bounds = [[area.swLat, area.swLon], [area.neLat, area.neLon]];
    allBounds.push([area.swLat, area.swLon], [area.neLat, area.neLon]);

    rectLayers.push(L.rectangle(bounds, {
      pane: 'areaRect',
      color: stroke,
      weight: 1.3,
      opacity: 0.9,
      fillColor: fill,
      fillOpacity: covered ? 0.08 : 0.12,
      dashArray: covered ? '4 4' : '2 6'
    }).bindTooltip(`${escapeHtml(area.code)} - ${area.weatherRecordCount} weather records`, {
      direction: 'top',
      className: 'area-label'
    }));

    glowLayers.push(L.rectangle(bounds, {
      pane: 'areaGlow',
      color: stroke,
      weight: 8,
      opacity: covered ? 0.05 : 0.08,
      fill: false
    }));

    const label = L.divIcon({
      className: '',
      html: `<div class="area-label">${escapeHtml(area.code)}<div class="sub">${covered ? escapeHtml(area.weatherRecordCount) + ' records' : 'no weather data'}</div></div>`,
      iconSize: [104, 34],
      iconAnchor: [0, 0]
    });
    labelMarkers.push(L.marker([area.neLat - 0.3, area.swLon + 0.3], {
      icon: label,
      interactive: false,
      pane: 'areaLabel'
    }));

    const dot = L.divIcon({
      className: '',
      html: `<div class="area-dot ${covered ? '' : 'missing'}"><div class="area-dot-core"></div><div class="area-dot-ring"></div><div class="area-dot-code">${escapeHtml(area.code)}</div></div>`,
      iconSize: [60, 60],
      iconAnchor: [30, 30]
    });
    areaDotMarkers.push(L.marker([area.centerLat, area.centerLon], { icon: dot, pane: 'areaDot' }));
  });

  function applyZoomLayer() {
    const z = map.getZoom();
    const overview = z <= 4;
    const medium = z >= 5;

    rectLayers.forEach((layer) => toggle(layer, medium));
    glowLayers.forEach((layer) => toggle(layer, medium));
    labelMarkers.forEach((layer) => toggle(layer, medium));
    areaDotMarkers.forEach((layer) => toggle(layer, overview));

    setTier('weather-zoom-tier-overview', overview);
    setTier('weather-zoom-tier-areas', medium);

    renderWeatherMarkers();
  }

  function toggle(layer, on) {
    if (on && !map.hasLayer(layer)) layer.addTo(map);
    if (!on && map.hasLayer(layer)) map.removeLayer(layer);
  }

  function setTier(id, active) {
    const el = document.getElementById(id);
    if (el) el.classList.toggle('active', active);
  }

  document.querySelectorAll('[id^="weather-zoom-tier-"]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const z = parseInt(btn.dataset.zoom, 10);
      if (!Number.isNaN(z)) map.setZoom(z);
    });
  });

  document.querySelectorAll('.weather-map-area-btn').forEach((btn) => {
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
  fetchCurrentWeather();

  function fetchCurrentWeather() {
    areas.forEach((area) => {
      if (!Number.isFinite(area.centerLat) || !Number.isFinite(area.centerLon)) return;
      const target = document.querySelector(`.weather-live-meteo[data-area-code="${cssEscape(area.code)}"]`);
      const params = new URLSearchParams({
        latitude: area.centerLat,
        longitude: area.centerLon,
        current: 'temperature_2m,weather_code,wind_speed_10m,wind_direction_10m,is_day'
      });
      fetch(`https://api.open-meteo.com/v1/forecast?${params.toString()}`)
        .then((response) => response.ok ? response.json() : Promise.reject(response))
        .then((payload) => {
          const current = payload.current || {};
          const units = payload.current_units || {};
          if (target) {
            target.textContent = `Live: ${formatWeatherValue(current.temperature_2m, units.temperature_2m)} / wind ${formatWeatherValue(current.wind_speed_10m, units.wind_speed_10m)} ${formatDirection(current.wind_direction_10m)}`;
          }
          area._wx = current;
          renderWeatherMarkers();
        })
        .catch(() => {
          if (target) target.textContent = 'Live weather unavailable';
        });
    });
  }

  // --- live weather icon chips (tiered by zoom: less detail out, more in) ---

  function currentTier() {
    const z = map.getZoom();
    if (z <= 4) return 'mini';     // overview: condition glyph only
    if (z <= 6) return 'compact';  // areas: condition + temperature
    return 'full';                 // detail: condition + strength + wind
  }

  function renderWeatherMarkers() {
    const tier = currentTier();
    areas.forEach((area, index) => {
      if (!area._wx || !Number.isFinite(area.centerLat) || !Number.isFinite(area.swLon)) return;
      const icon = weatherIconFor(area, tier);
      let marker = weatherMarkers[index];
      if (!marker) {
        // anchored to the WEST edge of the area, mid-height, so the chip sits
        // to the LEFT of the rectangle and never hides its coverage colour.
        marker = L.marker([area.centerLat, area.swLon], { icon, pane: 'areaWeather' })
          .bindTooltip(`${escapeHtml(area.code)} - live weather`, { direction: 'top', className: 'area-label' });
        weatherMarkers[index] = marker;
      } else {
        marker.setIcon(icon);
      }
      if (!map.hasLayer(marker)) marker.addTo(map);
    });
  }

  function weatherIconFor(area, tier) {
    // anchor X = icon width + 8px gap (chip grows left of the west edge);
    // anchor Y = half height (chip is vertically centred on the area).
    const dims = {
      mini:    { size: [40, 40],   anchor: [48, 20] },
      compact: { size: [108, 46],  anchor: [116, 23] },
      full:    { size: [80, 146],  anchor: [88, 73] }
    }[tier];
    return L.divIcon({
      className: '',
      html: weatherChipHtml(area, tier),
      iconSize: dims.size,
      iconAnchor: dims.anchor
    });
  }

  function weatherChipHtml(area, tier) {
    const w = area._wx;
    const cond = conditionIcon(w.weather_code, w.is_day === 1);
    const condImg = `<img class="wx-cond" src="${WX_ICON_BASE}${cond}.svg" alt="${cond}">`;
    const title = `${escapeHtml(area.code)} - WMO ${escapeHtml(w.weather_code)}, wind ${Math.round(Number(w.wind_speed_10m) || 0)} km/h from ${Math.round(Number(w.wind_direction_10m) || 0)} deg`;

    if (tier === 'mini') {
      return `<div class="wx-chip mini" title="${title}">${condImg}</div>`;
    }

    const temp = formatTemp(w.temperature_2m);
    if (tier === 'compact') {
      return `<div class="wx-chip" title="${title}">${condImg}` +
        `<div class="wx-meta"><span class="wx-temp">${temp}</span></div></div>`;
    }

    // full = vertical stack: condition -> temp -> wind icon -> speed -> direction arrow
    const bft = beaufort(Number(w.wind_speed_10m) || 0);
    const fromDir = compass8(Number(w.wind_direction_10m) || 0);
    const flowDeg = ((Number(w.wind_direction_10m) || 0) + 180) % 360;
    const speed = Math.round(Number(w.wind_speed_10m) || 0);
    return `<div class="wx-chip vertical" title="${title}">` +
      condImg +
      `<span class="wx-temp">${temp}</span>` +
      `<img class="wx-beaufort" src="${WX_ICON_BASE}wind-beaufort-${bft}.svg" alt="beaufort ${bft}">` +
      `<span class="wx-speed">${speed} km/h</span>` +
      `<span class="wx-wind">` +
        `<span class="wx-arrow" style="transform: rotate(${flowDeg}deg)">&#10148;</span>${fromDir}` +
      `</span>` +
    `</div>`;
  }

  // WMO weather_code -> Meteocons icon name (day/night aware)
  function conditionIcon(code, isDay) {
    const d = isDay ? 'day' : 'night';
    if (code === 0 || code === 1) return `clear-${d}`;
    if (code === 2) return `partly-cloudy-${d}`;
    if (code === 3) return 'overcast';
    if (code === 45 || code === 48) return 'fog';
    if (code >= 51 && code <= 55) return `partly-cloudy-${d}-drizzle`;
    if (code === 56 || code === 57) return 'sleet';
    if (code >= 61 && code <= 65) return 'rain';
    if (code === 66 || code === 67) return 'sleet';
    if (code >= 71 && code <= 77) return 'snow';
    if (code >= 80 && code <= 82) return `partly-cloudy-${d}-rain`;
    if (code === 85 || code === 86) return 'snow';
    if (code === 95) return 'thunderstorms';
    if (code === 96 || code === 99) return 'thunderstorms-rain';
    return 'not-available';
  }

  // wind speed (km/h) -> Beaufort number 0..12
  function beaufort(kmh) {
    const thresholds = [1, 5, 11, 19, 28, 38, 49, 61, 74, 88, 102, 117];
    for (let i = 0; i < thresholds.length; i++) {
      if (kmh < thresholds[i]) return i;
    }
    return 12;
  }

  // bearing degrees -> 8-point compass abbreviation the wind comes FROM
  function compass8(deg) {
    const points = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'];
    return points[Math.round(deg / 45) % 8];
  }

  function formatTemp(value) {
    if (value === null || value === undefined || Number.isNaN(Number(value))) return '-';
    return `${Math.round(Number(value))}°`;
  }

  function formatWeatherValue(value, unit) {
    if (value === null || value === undefined || Number.isNaN(Number(value))) return '-';
    return `${Math.round(Number(value) * 10) / 10}${unit || ''}`;
  }

  function formatDirection(value) {
    if (value === null || value === undefined || Number.isNaN(Number(value))) return '';
    return `@ ${Math.round(Number(value))} deg`;
  }

  function cssEscape(value) {
    if (window.CSS && typeof window.CSS.escape === 'function') return window.CSS.escape(String(value));
    return String(value).replaceAll('"', '\\"');
  }

  function escapeHtml(value) {
    return String(value || '')
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#039;');
  }
})();
