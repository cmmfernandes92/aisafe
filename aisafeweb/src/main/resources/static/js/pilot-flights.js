// Keeps the shared "My Flights & Plans" panel in sync: whenever a form on the
// page swaps a result into #form-result (Create Flight, Import Flight Plan, …),
// the flights tbody re-fetches itself with the current filters applied.
(function () {
  document.body.addEventListener('htmx:afterSwap', (event) => {
    const target = event.target;
    if (!target || !(target.id === 'form-result' || (target.closest && target.closest('#form-result')))) {
      return;
    }
    if (document.getElementById('pilot-flights-tbody')) {
      document.body.dispatchEvent(new Event('pilot-flights-refresh'));
    }
  });
})();