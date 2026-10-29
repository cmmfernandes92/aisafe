// AJAX pollers for the Remote Accesses Logging Server.
//
// One poller per page region. Each request reschedules itself only AFTER the
// previous one finishes (in onload / ontimeout / onerror) rather than on a fixed
// setInterval, so a slow or dead server can never pile up overlapping requests.
// Effective refresh period = response time + REFRESH_MS.

var REFRESH_MS = 2000;
var TIMEOUT_MS = 5000;

// Event types and services the server may report. Used purely to colour the
// fragments client-side; the server still returns plain HTML tables, untouched.
var EVENT_TYPES = { LOGIN_OK: 1, LOGIN_FAIL: 1, LOGOUT: 1, DISCONNECT: 1 };

// The remote-access services, mapped from the code the server emits to a display
// name and chip class. ATC is future (its remote access is not yet wired).
var SERVICES = {
    US44: { name: "Weather", cls: "svc-weather" },
    US86: { name: "Pilot",   cls: "svc-pilot"   },
    US78: { name: "ATC",     cls: "svc-atc"     }
};

// Turn raw "LOGIN_OK" / service cells into coloured chips and report the row count.
function decorate(div, countId) {
    var cells = div.querySelectorAll("td");
    for (var i = 0; i < cells.length; i++) {
        var td = cells[i];
        var text = td.textContent.trim();
        if (EVENT_TYPES[text]) {
            td.innerHTML = '<span class="badge ev-' + text.toLowerCase() + '">' + text + '</span>';
        } else if (SERVICES[text]) {
            var svc = SERVICES[text];
            td.innerHTML = '<span class="tag ' + svc.cls + '">' + svc.name + '</span>';
        }
    }
    if (countId) {
        var rows = div.querySelectorAll("tbody tr, table tr");
        var dataRows = 0;
        for (var r = 0; r < rows.length; r++) {
            if (rows[r].querySelector("td")) { dataRows++; }   // skip header (th-only) rows
        }
        var badge = document.getElementById(countId);
        if (badge) { badge.textContent = dataRows; }
    }
}

function poll(url, divId, countId, ms) {
    var req = new XMLHttpRequest();
    var div = document.getElementById(divId);

    req.onload = function () {
        div.innerHTML = this.responseText;
        decorate(div, countId);
        setTimeout(function () { poll(url, divId, countId, ms); }, ms);
    };
    req.ontimeout = function () {
        setTimeout(function () { poll(url, divId, countId, ms); }, 100);
    };
    req.onerror = function () {
        div.innerHTML = "<p>Logging server unreachable, reconnecting&hellip;</p>";
        setTimeout(function () { poll(url, divId, countId, ms); }, REFRESH_MS);
    };

    req.open("GET", url, true);
    req.timeout = TIMEOUT_MS;
    req.send();
}

function refreshEvents() { poll("/events", "events", "events-count", REFRESH_MS); }
function refreshActive() { poll("/active", "active", "active-count", REFRESH_MS); }
