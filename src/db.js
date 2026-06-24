import Database from "better-sqlite3";
import { mkdirSync } from "node:fs";
import { dirname } from "node:path";

const DB_PATH = process.env.POWERPAL_DB_PATH || "data/powerpal.db";

mkdirSync(dirname(DB_PATH), { recursive: true });

export const db = new Database(DB_PATH);
db.pragma("journal_mode = WAL");
db.pragma("foreign_keys = ON");

db.exec(`
  CREATE TABLE IF NOT EXISTS events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    area TEXT NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('outage_start', 'power_restored')),
    ts INTEGER NOT NULL,
    reporter TEXT,
    created_at INTEGER NOT NULL DEFAULT (unixepoch() * 1000)
  );
  CREATE INDEX IF NOT EXISTS idx_events_area_ts ON events (area, ts DESC);
`);

const insertEventStmt = db.prepare(
  "INSERT INTO events (area, type, ts, reporter) VALUES (?, ?, ?, ?)"
);

export function insertEvent({ area, type, ts, reporter }) {
  const info = insertEventStmt.run(area, type, ts, reporter ?? null);
  return { id: info.lastInsertRowid, area, type, ts, reporter: reporter ?? null };
}

const recentEventsStmt = db.prepare(
  "SELECT id, area, type, ts, reporter FROM events WHERE area = ? ORDER BY ts DESC LIMIT ?"
);

export function recentEvents(area, limit = 50) {
  return recentEventsStmt.all(area, limit);
}

const eventsSinceStmt = db.prepare(
  "SELECT id, area, type, ts FROM events WHERE area = ? AND ts >= ? ORDER BY ts ASC"
);

export function eventsSince(area, sinceMs) {
  return eventsSinceStmt.all(area, sinceMs);
}

const lastEventStmt = db.prepare(
  "SELECT id, area, type, ts FROM events WHERE area = ? ORDER BY ts DESC LIMIT 1"
);

export function lastEvent(area) {
  return lastEventStmt.get(area);
}

const listAreasStmt = db.prepare(
  "SELECT area, COUNT(*) AS event_count, MAX(ts) AS last_ts FROM events GROUP BY area ORDER BY last_ts DESC"
);

export function listAreas() {
  return listAreasStmt.all();
}
