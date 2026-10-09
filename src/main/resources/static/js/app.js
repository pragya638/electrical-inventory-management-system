// Small progressive-enhancement helpers. No framework, no build step.
// Every behaviour here is optional: the pages work with JavaScript disabled.

/**
 * Asks for confirmation before a destructive or state-changing action.
 * Forms carry data-confirm="message"; buttons use data-confirm too.
 */
document.addEventListener('submit', function (event) {
    var message = event.target.getAttribute('data-confirm');
    if (message && !window.confirm(message)) {
        event.preventDefault();
    }
});

document.addEventListener('click', function (event) {
    var trigger = event.target.closest('[data-confirm]');
    if (!trigger || trigger.tagName === 'FORM') {
        return;
    }
    if (!window.confirm(trigger.getAttribute('data-confirm'))) {
        event.preventDefault();
    }
});

/**
 * Filters the rows of a table as the user types. Used on the product and category
 * lists. Purely a convenience: the server-side filter form remains the source
 * of truth.
 */
function filterTable(inputId, tableId) {
    var input = document.getElementById(inputId);
    var table = document.getElementById(tableId);
    if (!input || !table) {
        return;
    }
    input.addEventListener('input', function () {
        var needle = input.value.trim().toLowerCase();
        var rows = table.tBodies[0].rows;
        for (var i = 0; i < rows.length; i++) {
            var text = rows[i].textContent.toLowerCase();
            rows[i].style.display = (needle === '' || text.indexOf(needle) !== -1) ? '' : 'none';
        }
    });
}
