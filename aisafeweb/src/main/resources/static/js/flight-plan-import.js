(function () {
  const form = document.getElementById('plan-import-form');
  if (!form) return;

  const textarea = document.getElementById('plan-content');
  const dropZone = document.getElementById('plan-drop-zone');
  const fileInput = document.getElementById('plan-file-input');
  const pickButton = document.getElementById('pick-plan-button');
  const pasteButton = document.getElementById('paste-plan-button');
  const fileNote = document.getElementById('plan-file-note');
  const fileName = document.getElementById('plan-file-name');

  function setContent(text, name) {
    if (!textarea) return;
    textarea.value = text;
    textarea.dispatchEvent(new Event('input', { bubbles: true }));
    if (name && fileName) fileName.value = name;
    if (fileNote) fileNote.textContent = name ? name : 'pasted content';
  }

  function loadFile(file) {
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => setContent(String(reader.result || ''), file.name);
    reader.readAsText(file);
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
      loadFile(event.dataTransfer.files && event.dataTransfer.files[0]);
    });
    dropZone.addEventListener('click', () => fileInput && fileInput.click());
    dropZone.addEventListener('keydown', (event) => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        if (fileInput) fileInput.click();
      }
    });
  }

  if (pickButton && fileInput) {
    pickButton.addEventListener('click', () => fileInput.click());
    fileInput.addEventListener('change', () => loadFile(fileInput.files && fileInput.files[0]));
  }

  if (pasteButton && navigator.clipboard) {
    pasteButton.addEventListener('click', async () => {
      setContent(await navigator.clipboard.readText(), null);
    });
  }
})();