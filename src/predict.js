// Statistical prediction for PowerPal.
// Deliberately simple: no ML, no Claude — just pair-up events into outages and
// compute frequency, time-of-day patterns, and typical duration.

const DAY_MS = 24 * 60 * 60 * 1000;
const HOUR_MS = 60 * 60 * 1000;

/**
 * Pair consecutive outage_start / power_restored events into discrete outages.
 * Skips unpaired events gracefully.
 */
export function buildOutages(events) {
  const sorted = [...events].sort((a, b) => a.ts - b.ts);
  const outages = [];
  let openStart = null;

  for (const e of sorted) {
    if (e.type === "outage_start") {
      // If there's already an open outage, close it at this start (data gap).
      if (openStart) {
        outages.push({ start: openStart.ts, end: e.ts, closed: false });
      }
      openStart = e;
    } else if (e.type === "power_restored" && openStart) {
      outages.push({ start: openStart.ts, end: e.ts, closed: true });
      openStart = null;
    }
  }

  return { outages, currentlyOut: openStart ? { since: openStart.ts } : null };
}

/**
 * Build a histogram of outage start hours (0..23).
 */
function hourHistogram(outages) {
  const hist = new Array(24).fill(0);
  for (const o of outages) {
    const h = new Date(o.start).getHours();
    hist[h] += 1;
  }
  return hist;
}

/**
 * Pick the top N hours by frequency, with ties broken by later hour
 * (later-in-day outages are usually more user-relevant).
 */
function topHours(hist, n = 3) {
  return hist
    .map((count, hour) => ({ hour, count }))
    .filter((h) => h.count > 0)
    .sort((a, b) => (b.count - a.count) || (b.hour - a.hour))
    .slice(0, n);
}

function average(nums) {
  if (nums.length === 0) return 0;
  return nums.reduce((a, b) => a + b, 0) / nums.length;
}

/**
 * Compute stats + a prediction for an area given raw events.
 * Returns null-ish fields when there isn't enough data.
 */
export function predict(events, { now = Date.now(), windowDays = 14 } = {}) {
  const since = now - windowDays * DAY_MS;
  const recent = events.filter((e) => e.ts >= since);
  const { outages, currentlyOut } = buildOutages(recent);

  const closed = outages.filter((o) => o.closed);
  const durationsMs = closed.map((o) => o.end - o.start);
  const avgDurationMs = average(durationsMs);

  const last7dCutoff = now - 7 * DAY_MS;
  const outagesLast7d = outages.filter((o) => o.start >= last7dCutoff).length;
  const outagesPerDay = outagesLast7d / 7;

  const hist = hourHistogram(outages);
  const top = topHours(hist, 3);

  // Prediction: find the next hour-of-day in `top` after `now`.
  let nextWindow = null;
  if (top.length > 0) {
    const nowDate = new Date(now);
    const nowHour = nowDate.getHours();
    // Look up to 48 hours ahead to find the nearest top-frequency hour.
    let best = null;
    for (let offset = 0; offset < 48; offset++) {
      const candidateHour = (nowHour + offset) % 24;
      const match = top.find((t) => t.hour === candidateHour);
      if (match) {
        const target = new Date(nowDate);
        target.setHours(target.getHours() + offset, 0, 0, 0);
        // If we're already past the target hour today, push to that hour on a future day.
        if (target.getTime() <= now) {
          target.setDate(target.getDate() + 1);
        }
        best = {
          ts: target.getTime(),
          hour: candidateHour,
          confidence: match.count / outages.length,
        };
        break;
      }
    }
    nextWindow = best;
  }

  const confidence =
    outages.length === 0
      ? "none"
      : outages.length < 3
      ? "low"
      : outages.length < 10
      ? "medium"
      : "high";

  return {
    now,
    windowDays,
    eventCount: recent.length,
    outageCount: outages.length,
    outagesLast7d,
    outagesPerDay: round(outagesPerDay, 2),
    avgDurationMinutes: closed.length ? Math.round(avgDurationMs / 60000) : null,
    topHours: top,
    nextLikelyOutage: nextWindow,
    expectedDurationMinutes:
      closed.length && avgDurationMs > 0 ? Math.round(avgDurationMs / 60000) : null,
    currentlyOut: currentlyOut
      ? {
          since: currentlyOut.since,
          durationMinutes: Math.round((now - currentlyOut.since) / 60000),
        }
      : null,
    confidence,
  };
}

function round(n, digits) {
  const f = 10 ** digits;
  return Math.round(n * f) / f;
}

/**
 * Rule-based natural-language summary. Used as the fallback when Claude is
 * unavailable or disabled.
 */
export function summarize(prediction, area) {
  const lines = [];
  if (prediction.eventCount === 0) {
    return `No data yet for ${area}. Log a few outages to start seeing predictions.`;
  }

  if (prediction.currentlyOut) {
    lines.push(
      `Power is currently OFF in ${area} (for ~${prediction.currentlyOut.durationMinutes} min).`
    );
  } else {
    lines.push(`Power is currently ON in ${area}.`);
  }

  if (prediction.outagesLast7d > 0) {
    lines.push(
      `${prediction.outagesLast7d} outage${prediction.outagesLast7d === 1 ? "" : "s"} in the last 7 days (~${prediction.outagesPerDay}/day).`
    );
  }

  if (prediction.nextLikelyOutage) {
    const t = new Date(prediction.nextLikelyOutage.ts);
    const timeLabel = t.toLocaleString(undefined, {
      weekday: "short",
      hour: "numeric",
      minute: "2-digit",
    });
    const dur = prediction.expectedDurationMinutes
      ? ` lasting ~${prediction.expectedDurationMinutes} min`
      : "";
    lines.push(`Next likely outage: ${timeLabel}${dur} (confidence: ${prediction.confidence}).`);
  }

  return lines.join(" ");
}
