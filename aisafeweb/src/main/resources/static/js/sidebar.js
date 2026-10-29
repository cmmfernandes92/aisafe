(function () {
  const KEY = 'aisafe.sidebar.collapsed';
  const body = document.body;
  const toggle = document.getElementById('sidebar-toggle');

  function apply(collapsed) {
    body.classList.toggle('sidebar-collapsed', collapsed);
    try {
      localStorage.setItem(KEY, collapsed ? '1' : '0');
    } catch (e) {
      // localStorage may be unavailable in restricted browser contexts.
    }
  }

  try {
    apply(localStorage.getItem(KEY) === '1');
  } catch (e) {
    apply(false);
  }

  if (toggle) {
    toggle.addEventListener('click', () => {
      apply(!body.classList.contains('sidebar-collapsed'));
      setTimeout(() => window.dispatchEvent(new Event('resize')), 220);
    });
  }
})();
