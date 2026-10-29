(function () {
  const form = document.getElementById('import-form');
  if (!form) return;

  const sourceInput = document.getElementById('import-source');
  const config = document.getElementById('import-config');
  const submit = document.getElementById('import-submit');
  const dataSource = document.getElementById('import-data-source');
  const options = Array.from(document.querySelectorAll('.source-option'));
  const sections = {
    'csv': document.getElementById('csv-section'),
    'open-meteo': document.getElementById('open-meteo-section'),
  };

  // CSV helpers
  const textarea = document.getElementById('csv-content');
  const dropZone = document.getElementById('csv-drop-zone');
  const pasteButton = document.getElementById('paste-csv-button');
  const rowCount = document.getElementById('csv-row-count');

  // Open-Meteo helpers
  const omDay = document.getElementById('om-day');

  function applyOpenMeteoDateBounds() {
    if (!omDay) return;
    // Open-Meteo's forecast API only covers ~92 days of past data and ~16 days ahead.
    const isoOffset = (days) => {
      const d = new Date();
      d.setDate(d.getDate() + days);
      return d.toISOString().slice(0, 10);
    };
    omDay.min = isoOffset(-92);
    omDay.max = isoOffset(16);
  }

  function setSectionEnabled(section, enabled) {
    if (!section) return;
    section.querySelectorAll('input, textarea, select').forEach((field) => {
      field.disabled = !enabled;
    });
  }

  function choose(source) {
    sourceInput.value = source;
    config.hidden = false;
    submit.hidden = false;
    if (dataSource) dataSource.disabled = false;

    Object.entries(sections).forEach(([key, section]) => {
      const active = key === source;
      if (section) section.hidden = !active;
      setSectionEnabled(section, active);
    });

    options.forEach((button) => {
      button.classList.toggle('active', button.dataset.source === source);
    });

    if (source === 'open-meteo') {
      if (dataSource && !dataSource.value) dataSource.value = 'Open-Meteo';
      applyOpenMeteoDateBounds();
    }
    if (source === 'csv' && textarea) {
      textarea.focus();
    }
  }

  options.forEach((button) => {
    button.addEventListener('click', () => choose(button.dataset.source));
  });

  // ── CSV drop / paste / row count ──────────────────────────────────────────
  function updateRowCount() {
    if (!textarea || !rowCount) return;
    const lines = textarea.value.split(/\r?\n/).filter((line) => line.trim().length > 0);
    const dataRows = Math.max(0, lines.length - 1);
    rowCount.textContent = `${dataRows} data row${dataRows === 1 ? '' : 's'}`;
  }

  function setCsvText(text) {
    if (!textarea) return;
    textarea.value = text;
    textarea.dispatchEvent(new Event('input', { bubbles: true }));
    updateRowCount();
  }

  if (dropZone) {
    ['dragenter', 'dragover'].forEach((eventName) => {
      dropZone.addEventListener(eventName, (event) => {
        event.preventDefault();
        dropZone.classList.add('drag-over');
      });
    });
    ['dragleave', 'drop'].forEach((eventName) => {
      dropZone.addEventListener(eventName, () => dropZone.classList.remove('drag-over'));
    });
    dropZone.addEventListener('drop', (event) => {
      event.preventDefault();
      const file = event.dataTransfer.files && event.dataTransfer.files[0];
      if (!file) return;
      const reader = new FileReader();
      reader.onload = () => setCsvText(String(reader.result || ''));
      reader.readAsText(file);
    });
  }

  if (pasteButton && navigator.clipboard) {
    pasteButton.addEventListener('click', async () => {
      setCsvText(await navigator.clipboard.readText());
    });
  }

  if (textarea) {
    textarea.addEventListener('input', updateRowCount);
    updateRowCount();
  }
})();