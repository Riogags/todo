import express from "express";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

import { insertEvent, recentEvents, eventsSince, lastEvent, listAreas } from "./db.js";
import { predict, summarize } from "./predict.js";
import * as claude from "./claude.js";

const __dirname = dirname(fileURLToPath(import.meta.url));
const PUBLIC_DIR = join(__dirname, "..", "public");
const PORT = Number(process.env.PORT) || 3000;
const WINDOW_DAYS = 30; // fetch a wider window so predictions see enough history

const app = express();
app.use(express.json({ limit: "32kb" }));
app.use(express.static(PUBLIC_DIR));

// --- Areas ---------------------------------------------------------------

app.get("/api/areas", (_req, res) => {
  res.json({ areas: listAreas() });
});

// --- Events --------------------------------------------------------------

app.post("/api/events", (req, res) => {
  const { area, type, ts, reporter } = req.body ?? {};
  const err = validateEvent({ area, type, ts });
  if (err) return res.status(400).json({ error: err });

  const event = insertEvent({
    area: area.trim(),
    type,
    ts: ts ?? Date.now(),
    reporter: reporter ? String(reporter).slice(0, 64) : null,
  });
  res.status(201).json({ event });
});

app.get("/api/areas/:area/events", (req, res) => {
  const area = req.params.area.trim();
  const limit = clampInt(req.query.limit, 1, 200, 50);
  res.json({ area, events: recentEvents(area, limit) });
});

// --- Status + Prediction -------------------------------------------------

app.get("/api/areas/:area/status", (req, res) => {
  const area = req.params.area.trim();
  const last = lastEvent(area);
  const currentlyOut = last?.type === "outage_start";
  res.json({
    area,
    currentlyOut,
    lastEvent: last ?? null,
  });
});

app.get("/api/areas/:area/prediction", (req, res) => {
  const area = req.params.area.trim();
  const since = Date.now() - WINDOW_DAYS * 24 * 60 * 60 * 1000;
  const events = eventsSince(area, since);
  const prediction = predict(events);
  res.json({
    area,
    prediction,
    summary: summarize(prediction, area),
    aiSummaryAvailable: claude.available,
  });
});

app.get("/api/areas/:area/ai-summary", async (req, res) => {
  const area = req.params.area.trim();
  if (!claude.available) {
    return res.status(501).json({
      error: "Claude AI summary is not configured on this server.",
      hint: "Set ANTHROPIC_API_KEY to enable.",
    });
  }

  const since = Date.now() - WINDOW_DAYS * 24 * 60 * 60 * 1000;
  const events = eventsSince(area, since);
  const prediction = predict(events);

  try {
    const text = await claude.generateSummary(area, prediction);
    res.json({ area, prediction, summary: text, source: "claude" });
  } catch (e) {
    console.error("[claude] failed:", e.message);
    res.status(502).json({
      error: "Upstream AI request failed.",
      fallbackSummary: summarize(prediction, area),
    });
  }
});

// --- Health --------------------------------------------------------------

app.get("/api/health", (_req, res) => {
  res.json({
    ok: true,
    version: "0.1.0",
    aiSummaryAvailable: claude.available,
  });
});

// --- Validation helpers --------------------------------------------------

const VALID_EVENT_TYPES = new Set(["outage_start", "power_restored"]);

function validateEvent({ area, type, ts }) {
  if (typeof area !== "string" || area.trim().length === 0) {
    return "`area` must be a non-empty string.";
  }
  if (area.length > 80) return "`area` is too long (max 80 chars).";
  if (!VALID_EVENT_TYPES.has(type)) {
    return `\`type\` must be one of: ${[...VALID_EVENT_TYPES].join(", ")}.`;
  }
  if (ts != null) {
    if (typeof ts !== "number" || !Number.isFinite(ts)) {
      return "`ts` must be a Unix timestamp in milliseconds.";
    }
    const now = Date.now();
    if (ts > now + 60_000) return "`ts` is in the future.";
    if (ts < now - 365 * 24 * 60 * 60 * 1000) return "`ts` is more than a year old.";
  }
  return null;
}

function clampInt(value, min, max, fallback) {
  const n = Number.parseInt(value, 10);
  if (!Number.isFinite(n)) return fallback;
  return Math.max(min, Math.min(max, n));
}

// --- Start ---------------------------------------------------------------

app.listen(PORT, () => {
  console.log(`PowerPal listening on http://localhost:${PORT}`);
  console.log(`  AI summary: ${claude.available ? "enabled" : "disabled (set ANTHROPIC_API_KEY to enable)"}`);
});
