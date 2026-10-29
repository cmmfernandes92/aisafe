(function () {
  function cellValue(row, index) {
    const cell = row.children[index];
    return cell ? cell.textContent.trim() : '';
  }

  function numericValue(value) {
    const normalized = value.replace(/\s/g, '').replace(',', '.');
    if (!/^-?\d+(\.\d+)?$/.test(normalized)) {
      return null;
    }
    return Number(normalized);
  }

  function columnIsNumeric(rows, index) {
    const values = rows.map((row) => cellValue(row, index)).filter(Boolean);
    return values.length > 0 && values.every((value) => numericValue(value) !== null);
  }

  function sortTable(table, header) {
    const headers = Array.from(header.parentElement.children);
    const index = headers.indexOf(header);
    const tbody = table.tBodies[0];
    if (!tbody || index < 0) return;

    const rows = Array.from(tbody.rows).filter((row) => row.children.length === headers.length);
    if (rows.length < 2) return;

    const direction = header.dataset.sortDirection === 'asc' ? 'desc' : 'asc';
    const numeric = columnIsNumeric(rows, index);
    const multiplier = direction === 'asc' ? 1 : -1;

    rows.sort((left, right) => {
      const leftValue = cellValue(left, index);
      const rightValue = cellValue(right, index);
      if (numeric) {
        return (numericValue(leftValue) - numericValue(rightValue)) * multiplier;
      }
      return leftValue.localeCompare(rightValue, undefined, {
        numeric: true,
        sensitivity: 'base'
      }) * multiplier;
    });

    headers.forEach((item) => {
      item.classList.remove('sort-asc', 'sort-desc');
      item.removeAttribute('aria-sort');
      delete item.dataset.sortDirection;
    });
    header.dataset.sortDirection = direction;
    header.classList.add(direction === 'asc' ? 'sort-asc' : 'sort-desc');
    header.setAttribute('aria-sort', direction === 'asc' ? 'ascending' : 'descending');

    rows.forEach((row) => tbody.appendChild(row));
  }

  function initialize(table) {
    const headers = table.tHead ? Array.from(table.tHead.rows[0].cells) : [];
    headers.forEach((header) => {
      header.tabIndex = 0;
      header.setAttribute('role', 'button');
      header.addEventListener('click', () => sortTable(table, header));
      header.addEventListener('keydown', (event) => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault();
          sortTable(table, header);
        }
      });
    });
  }

  document.querySelectorAll('table.sortable-table').forEach(initialize);

  document.body.addEventListener('htmx:afterSwap', (event) => {
    const tbody = event.target;
    if (!(tbody instanceof HTMLTableSectionElement)) return;
    const table = tbody.closest('table.sortable-table');
    if (!table || !table.tHead) return;
    const active = table.tHead.querySelector('th.sort-asc, th.sort-desc');
    if (!active) return;
    active.dataset.sortDirection = active.classList.contains('sort-asc') ? 'desc' : 'asc';
    sortTable(table, active);
  });
})();
