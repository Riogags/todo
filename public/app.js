// PowerPal frontend — single-file vanilla JS client.

const AREA_KEY = "powerpal:area";

const els = {
  areaInput: document.getElementById("area-input"),
  areaSave: document.getElementById("area-save"),
  statusCard: document.getElementById("status-card"),
  statusText: document.getElementById("status-text"),
  statusDetail: document.getElementById("status-detail"),
  btnOutageStart: document.getElementById("btn-outage-start"),
  btnPowerRestored: document.getElementById("btn-power-restored"),
  btnRefresh: document.getElementById("btn-refresh"),
  predictionSummary: document.getElementById("prediction-summary"),
  statConfidence: document.getElementById("stat-confidence"),
  statOutages7d: document.getElementById("stat-outages-7d"),
  statDuration: document.getElementById("stat-duration"),
  statEvents: document.getElementById("stat-events"),
  btnAiSummary: document.getElementById("btn-ai-summary"),
  aiSummary: document.getElementById("ai-summary"),
  historyList: document.getElementById("history-list"),
  toast: document.getElementById("toast"),
};

let currentArea = localStorage.getItem(AREA_KEY) || "";
if (currentArea) els.areaInput.value = currentArea;

function toast(message, { error = false } = {}) {
  els.toast.textContent = message;
  els.toast.classList.toggle("error", error);
  els.toast.classList.add("show");
  clearTimeout(toast._t);
  toast._t = setTimeout(() => els.toast.classList.remove("show"), 2800);
}

async function apiJson(method, url, body) {
  const res = await fetch(url, {
    method,
    headers: body ? { "Content-Type": "application/json" } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) {
    const err = new Error(data?.error || `HTTP ${res.status}`);
    err.data = data;
    throw err;
  }
  return data;
}

async function refreshAll() {
  if (!currentArea) {
    els.statusText.textContent = "Set your area to begin";
    els.statusCard.removeAttribute("data-status");
    els.predictionSummary.textContent = "Enter an area above to see predictions.";
    els.historyList.innerHTML = '<li class="muted">No area selected.</li>';
    setStats({});
    return;
  }

  try {
    await Promise.all([refreshStatus(), refreshPrediction(), refreshHistory()]);
  } catch (e) {
    console.error(e);
    toast(`Failed to load: ${e.message}`, { error: true });
  }
}

async function refreshStatus() {
  const encoded = encodeURIComponent(currentArea);
  const data = await apiJson("GET", `/api/areas/${encoded}/status`);
  if (data.currentlyOut) {
    els.statusCard.setAttribute("data-status", "off");
    els.statusText.textContent = "Power OFF";
    const since = data.lastEvent ? new Date(data.lastEvent.ts) : null;
    els.statusDetail.textContent = since
      ? `Since ${formatTime(since)}`
      : "";
  } else if (data.lastEvent) {
    els.statusCard.setAttribute("data-status", "on");
    els.statusText.textContent = "Power ON";
    const since = new Date(data.lastEvent.ts);
    els.statusDetail.textContent = `Last change ${formatRelative(since)}`;
  } else {
    els.statusCard.removeAttribute("data-status");
    els.statusText.textContent = "No data yet";
    els.statusDetail.textContent = "Log an event to begin.";
  }
}

async function refreshPrediction() {
  const encoded = encodeURIComponent(currentArea);
  const data = await apiJson("GET", `/api/areas/${encoded}/prediction`);
  const p = data.prediction;

  els.predictionSummary.textContent = data.summary;
  setStats({
    confidence: p.confidence,
    outages7d: p.outagesLast7d,
    duration: p.avgDurationMinutes,
    events: p.eventCount,
  });
  els.btnAiSummary.hidden = !data.aiSummaryAvailable;
  els.aiSummary.hidden = true;
  els.aiSummary.textContent = "";
}

async function refreshHistory() {
  const encoded = encodeURIComponent(currentArea);
  const data = await apiJson("GET", `/api/areas/${encoded}/events?limit=25`);
  if (data.events.length === 0) {
    els.historyList.innerHTML = '<li class="muted">No events yet.</li>';
    return;
  }
  els.historyList.innerHTML = data.events
    .map((e) => {
      const label = e.type === "outage_start" ? "Power off" : "Power on";
      return `
        <li>
          <span class="type-${e.type}">${label}</span>
          <time datetime="${new Date(e.ts).toISOString()}">${formatRelative(new Date(e.ts))}</time>
        </li>
      `;
    })
    .join("");
}

function setStats({ confidence, outages7d, duration, events }) {
  els.statConfidence.textContent = confidence ?? "—";
  els.statOutages7d.textContent = outages7d != null ? String(outages7d) : "—";
  els.statDuration.textContent = duration != null ? `${duration} min` : "—";
  els.statEvents.textContent = events != null ? String(events) : "—";
}

function formatTime(date) {
  return date.toLocaleTimeString(undefined, { hour: "numeric", minute: "2-digit" });
}

function formatRelative(date) {
  const diffMs = Date.now() - date.getTime();
  const s = Math.round(diffMs / 1000);
  if (s < 60) return "just now";
  const m = Math.round(s / 60);
  if (m < 60) return `${m} min ago`;
  const h = Math.round(m / 60);
  if (h < 24) return `${h}h ago`;
  const d = Math.round(h / 24);
  if (d < 7) return `${d}d ago`;
  return date.toLocaleDateString(undefined, { month: "short", day: "numeric" });
}

async function logEvent(type) {
  if (!currentArea) {
    toast("Set your area first", { error: true });
    els.areaInput.focus();
    return;
  }
  els.btnOutageStart.disabled = true;
  els.btnPowerRestored.disabled = true;
  try {
    await apiJson("POST", "/api/events", { area: currentArea, type });
    toast(type === "outage_start" ? "Logged: power OFF" : "Logged: power ON");
    await refreshAll();
  } catch (e) {
    toast(`Log failed: ${e.message}`, { error: true });
  } finally {
    els.btnOutageStart.disabled = false;
    els.btnPowerRestored.disabled = false;
  }
}

async function loadAiSummary() {
  if (!currentArea) return;
  els.btnAiSummary.disabled = true;
  const originalLabel = els.btnAiSummary.textContent;
  els.btnAiSummary.textContent = "Thinking…";
  try {
    const encoded = encodeURIComponent(currentArea);
    const data = await apiJson("GET", `/api/areas/${encoded}/ai-summary`);
    els.aiSummary.hidden = false;
    els.aiSummary.textContent = data.summary;
  } catch (e) {
    toast(`AI summary failed: ${e.message}`, { error: true });
    if (e.data?.fallbackSummary) {
      els.aiSummary.hidden = false;
      els.aiSummary.textContent = e.data.fallbackSummary;
    }
  } finally {
    els.btnAiSummary.disabled = false;
    els.btnAiSummary.textContent = originalLabel;
  }
}

// --- Event wiring --------------------------------------------------------

els.areaSave.addEventListener("click", () => {
  const value = els.areaInput.value.trim();
  if (!value) {
    toast("Enter an area name", { error: true });
    return;
  }
  currentArea = value;
  localStorage.setItem(AREA_KEY, value);
  toast(`Area set: ${value}`);
  refreshAll();
});

els.areaInput.addEventListener("keydown", (e) => {
  if (e.key === "Enter") {
    e.preventDefault();
    els.areaSave.click();
  }
});

els.btnOutageStart.addEventListener("click", () => logEvent("outage_start"));
els.btnPowerRestored.addEventListener("click", () => logEvent("power_restored"));
els.btnRefresh.addEventListener("click", refreshAll);
els.btnAiSummary.addEventListener("click", loadAiSummary);

// Initial load
refreshAll();
